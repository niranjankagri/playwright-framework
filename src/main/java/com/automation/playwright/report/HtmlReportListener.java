package com.automation.playwright.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.xml.XmlSuite;

import com.automation.playwright.config.Config;

/**
 * Writes one HTML report ({@value #FILE_NAME}) after the run. Tests are
 * grouped by class in collapsible sections; every test shows its steps
 * (page actions and API calls), and a failed UI test also shows its
 * screenshot, a video player and its Playwright trace with the command to
 * open it. The report has search, status filters and a light/dark theme.
 * <p>
 * Videos and traces stay next to the report in {@code target/artifacts}
 * and are linked relatively, so keep the folders together when sharing.
 * Registered through {@code META-INF/services/org.testng.ITestNGListener}.
 */
public class HtmlReportListener implements IReporter {

	/** Result attribute: Base64 PNG screenshot of a failed UI test. */
	public static final String SCREENSHOT = "screenshot";
	/** Result attribute: java.nio.file.Path of the kept video. */
	public static final String VIDEO = "video";
	/** Result attribute: java.nio.file.Path of the kept trace zip. */
	public static final String TRACE = "trace";

	// Name of the report file in TestNG's output directory
	static final String FILE_NAME = "playwright-test-report.html";
	// Report title when the run has no suite name of its own (Maven names its suite "Surefire suite")
	private static final String DEFAULT_TITLE = "Playwright UI + API Suite";
	// Start time format in the report header
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss").withZone(ZoneId.systemDefault());
	// Stack trace lines from these packages are framework noise and are left out
	private static final List<String> NOISE = List.of("org.testng.", "java.base/", "jdk.internal.", "sun.reflect.", "org.apache.maven.");

	/**
	 * Builds the report from all suites of the run.
	 *
	 * @param xmlSuites       the suite definitions (unused).
	 * @param suites          the suite results.
	 * @param outputDirectory TestNG's output folder, e.g. target/surefire-reports.
	 */
	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		List<ITestResult> results = new ArrayList<>();
		String title = DEFAULT_TITLE;
		for (ISuite suite : suites) {
			// Keep a real suite name (testng.xml); Maven's and TestNG's default names are replaced
			if (!suite.getName().startsWith("Surefire") && !suite.getName().startsWith("Default")) {
				title = suite.getName();
			}
			for (ISuiteResult suiteResult : suite.getResults().values()) {
				ITestContext context = suiteResult.getTestContext();
				results.addAll(context.getPassedTests().getAllResults());
				results.addAll(context.getFailedTests().getAllResults());
				results.addAll(context.getFailedButWithinSuccessPercentageTests().getAllResults());
				results.addAll(context.getSkippedTests().getAllResults());
			}
		}
		results.sort(Comparator.comparingLong(ITestResult::getStartMillis));
		Path report = Path.of(outputDirectory, FILE_NAME).toAbsolutePath();
		try {
			Files.createDirectories(report.getParent());
			Files.writeString(report, render(title, results, report.getParent()), StandardCharsets.UTF_8);
			System.out.println("HTML report: " + report);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write " + report, e);
		}
	}

	/**
	 * @param title      the report title.
	 * @param results    every test result, in start order.
	 * @param reportDir  the report's folder, for relative links to videos and traces.
	 * @return String    the complete HTML page.
	 */
	private String render(String title, List<ITestResult> results, Path reportDir) {
		long passed = count(results, "passed");
		long failed = count(results, "failed");
		long skipped = results.size() - passed - failed;
		int rate = results.isEmpty() ? 0 : (int) Math.round(passed * 100.0 / results.size());
		long start = results.stream().mapToLong(ITestResult::getStartMillis).min().orElse(System.currentTimeMillis());
		long end = results.stream().mapToLong(ITestResult::getEndMillis).max().orElse(start);
		String verdict = failed > 0 ? "failed" : passed == 0 ? "skipped" : "passed";

		// One section per test class; classes with a failure or skip first, then by name
		Map<String, List<ITestResult>> byClass = results.stream().collect(Collectors.groupingBy(
				r -> r.getTestClass().getRealClass().getSimpleName(), LinkedHashMap::new, Collectors.toList()));
		List<Map.Entry<String, List<ITestResult>>> groups = new ArrayList<>(byClass.entrySet());
		groups.sort(Comparator.comparing((Map.Entry<String, List<ITestResult>> g) -> count(g.getValue(), "passed") == g.getValue().size())
				.thenComparing(Map.Entry::getKey));
		StringBuilder sections = new StringBuilder();
		int id = 0;
		for (Map.Entry<String, List<ITestResult>> group : groups) {
			sections.append(renderGroup(group.getKey(), group.getValue(), id, reportDir));
			id += group.getValue().size();
		}

		return TEMPLATE
				.replace("{{title}}", esc(title))
				.replace("{{verdict}}", verdict)
				.replace("{{verdictText}}", switch (verdict) {
				case "failed" -> failed + (failed == 1 ? " test failed" : " tests failed");
				case "passed" -> "All tests passed" + (skipped > 0 ? " (" + skipped + " skipped)" : "");
				default -> "No test passed";
				})
				.replace("{{started}}", TIME.format(Instant.ofEpochMilli(start)))
				.replace("{{duration}}", duration(end - start))
				.replace("{{total}}", String.valueOf(results.size()))
				.replace("{{passed}}", String.valueOf(passed))
				.replace("{{failed}}", String.valueOf(failed))
				.replace("{{skipped}}", String.valueOf(skipped))
				.replace("{{rate}}", String.valueOf(rate))
				.replace("{{pPassed}}", percent(passed, results.size()))
				.replace("{{pFailed}}", percent(failed, results.size()))
				.replace("{{pSkipped}}", percent(skipped, results.size()))
				.replace("{{env}}", environment())
				.replace("{{groups}}", sections.toString());
	}

	/**
	 * Renders one test class: a summary row (counts, result bar, total time)
	 * that opens to its test cards. Classes with a failure or skip start open.
	 *
	 * @param name       the class name.
	 * @param results    its results, in start order.
	 * @param firstId    the element id number of its first card.
	 * @param reportDir  the report's folder.
	 * @return String    the HTML section.
	 */
	private String renderGroup(String name, List<ITestResult> results, int firstId, Path reportDir) {
		long passed = count(results, "passed");
		long failed = count(results, "failed");
		long skipped = results.size() - passed - failed;
		long millis = results.stream().mapToLong(r -> r.getEndMillis() - r.getStartMillis()).sum();
		String worst = failed > 0 ? "failed" : skipped > 0 ? "skipped" : "passed";
		boolean open = !worst.equals("passed");
		String chips = (passed > 0 ? "<span class=\"chip passed\">" + passed + " passed</span>" : "")
				+ (failed > 0 ? "<span class=\"chip failed\">" + failed + " failed</span>" : "")
				+ (skipped > 0 ? "<span class=\"chip skipped\">" + skipped + " skipped</span>" : "");
		StringBuilder cards = new StringBuilder();
		for (int i = 0; i < results.size(); i++) {
			cards.append(renderTest(results.get(i), firstId + i, reportDir));
		}
		return "<section class=\"group " + worst + (open ? " open" : "") + "\" data-default=\"" + open + "\">"
				+ "<button class=\"ghead\" aria-expanded=\"" + open + "\"><span class=\"gname\">" + esc(name)
				+ "<small>" + results.size() + (results.size() == 1 ? " test" : " tests") + "</small></span>"
				+ "<span class=\"chips\">" + chips + "</span>"
				+ "<span class=\"gbar\"><i class=\"p\" style=\"width:" + percent(passed, results.size()) + "%\"></i><i class=\"f\" style=\"width:"
				+ percent(failed, results.size()) + "%\"></i><i class=\"s\" style=\"width:" + percent(skipped, results.size()) + "%\"></i></span>"
				+ "<span class=\"gtime\">" + duration(millis) + "</span><span class=\"chev\"></span></button>"
				+ "<div class=\"gbody\"><div class=\"clip\"><div class=\"glist\">" + cards + "</div></div></div></section>";
	}

	/**
	 * @param result     one test result.
	 * @param id         a unique number for element ids.
	 * @param reportDir  the report's folder.
	 * @return String    the HTML card of the test.
	 */
	private String renderTest(ITestResult result, int id, Path reportDir) {
		String status = status(result);
		long millis = result.getEndMillis() - result.getStartMillis();
		// Title: the @Test description, else the method name; a data-provider row adds its first parameter
		String description = result.getMethod().getDescription();
		String title = description == null || description.isBlank() ? result.getMethod().getMethodName() : description;
		if (result.getParameters().length > 0) {
			title += " — " + result.getParameters()[0];
		}
		String groups = Arrays.stream(result.getMethod().getGroups()).sorted().distinct()
				.map(g -> "<span class=\"tag\">@" + esc(g) + "</span>").collect(Collectors.joining());

		StringBuilder body = new StringBuilder();
		// Steps recorded with Steps.log: page actions and API calls
		List<String> steps = Reporter.getOutput(result);
		if (!steps.isEmpty()) {
			body.append("<ol class=\"steps\">");
			steps.forEach(step -> body.append("<li>").append(highlight(esc(step))).append("</li>"));
			body.append("</ol>");
		}
		// Failure or skip reason; the stack trace for failures only
		Throwable error = result.getThrowable();
		if (error != null) {
			boolean skip = status.equals("skipped");
			body.append("<div class=\"").append(skip ? "skip" : "error").append("\"><div class=\"etitle\">")
					.append(skip ? "Skipped: " : "").append(esc(error.getClass().getSimpleName())).append("</div><pre>")
					.append(esc(message(error))).append("</pre>");
			if (!skip) {
				body.append("<details><summary>Stack trace</summary><pre class=\"trace\">").append(esc(trace(error))).append("</pre></details>");
			}
			body.append("</div>");
		}
		// What Playwright kept for a failed UI test: screenshot, video and trace
		Object screenshot = result.getAttribute(SCREENSHOT);
		Object video = result.getAttribute(VIDEO);
		Object trace = result.getAttribute(TRACE);
		if (screenshot != null || video != null || trace != null) {
			body.append("<div class=\"media\">");
			if (screenshot != null) {
				body.append("<figure><a href=\"#\" class=\"zoom\"><img alt=\"Screenshot at failure\" src=\"data:image/png;base64,")
						.append(screenshot).append("\"></a><figcaption>Screenshot at the moment of failure</figcaption></figure>");
			}
			if (video instanceof Path path) {
				body.append("<figure><video controls preload=\"metadata\" src=\"").append(esc(link(reportDir, path)))
						.append("\"></video><figcaption>Video of the whole test</figcaption></figure>");
			}
			body.append("</div>");
			if (trace instanceof Path path) {
				body.append("<div class=\"tracebox\"><b>Playwright trace</b> (every action with DOM snapshots, network and console): ")
						.append("<a href=\"").append(esc(link(reportDir, path))).append("\" download>").append(esc(path.getFileName().toString()))
						.append("</a><br>Open it with <code>mvn exec:java -Dexec.args=\"show-trace ").append(esc(path.toString().replace('\\', '/')))
						.append("\"</code> or drop the zip on <a href=\"https://trace.playwright.dev\" target=\"_blank\" rel=\"noopener\">trace.playwright.dev</a>.</div>");
			}
		}
		if (body.isEmpty()) {
			body.append("<p class=\"muted\">No steps recorded.</p>");
		}

		String search = (title + " " + result.getMethod().getMethodName() + " " + result.getTestClass().getRealClass().getSimpleName()
				+ " " + String.join(" ", result.getMethod().getGroups())).toLowerCase(Locale.ROOT);
		return "<article class=\"test " + status + "\" data-status=\"" + status + "\" data-search=\"" + esc(search) + "\">"
				+ "<button class=\"head\" aria-expanded=\"false\" aria-controls=\"t" + id + "\"><span class=\"pill " + status + "\">" + status + "</span>"
				+ "<span class=\"name\"><strong>" + esc(title) + "</strong><code>" + esc(result.getMethod().getMethodName()) + "</code></span>"
				+ "<span class=\"tags\">" + groups + "</span><span class=\"time\">" + duration(millis) + "</span><span class=\"chev\"></span></button>"
				+ "<div class=\"body\" id=\"t" + id + "\"><div class=\"clip\"><div class=\"pad\">" + body + "</div></div></div></article>";
	}

	/**
	 * @return String the run environment as definition-list items.
	 */
	private String environment() {
		Map<String, String> facts = new LinkedHashMap<>();
		try {
			Config config = Config.get();
			facts.put("Web shop", config.baseUrl());
			facts.put("API", config.apiUrl());
			facts.put("Browser", config.browser() + (config.headless() ? " (headless)" : ""));
		} catch (RuntimeException | LinkageError e) {
			// The report is still useful without the test settings
		}
		facts.put("Java", System.getProperty("java.version"));
		facts.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
		return facts.entrySet().stream()
				.map(f -> "<div><dt>" + esc(f.getKey()) + "</dt><dd>" + esc(f.getValue()) + "</dd></div>")
				.collect(Collectors.joining());
	}

	/**
	 * @param reportDir the report's folder.
	 * @param file      a kept video or trace.
	 * @return String   the file's path relative to the report, with forward slashes.
	 */
	private static String link(Path reportDir, Path file) {
		return reportDir.relativize(file.toAbsolutePath()).toString().replace('\\', '/');
	}

	/**
	 * @param results  test results.
	 * @param status   passed, failed or skipped.
	 * @return long    how many have that status.
	 */
	private static long count(List<ITestResult> results, String status) {
		return results.stream().filter(r -> status(r).equals(status)).count();
	}

	/**
	 * @param result   one test result.
	 * @return String  passed, failed or skipped.
	 */
	private static String status(ITestResult result) {
		return switch (result.getStatus()) {
		case ITestResult.SUCCESS -> "passed";
		case ITestResult.FAILURE, ITestResult.SUCCESS_PERCENTAGE_FAILURE -> "failed";
		default -> "skipped";
		};
	}

	/**
	 * @param escaped  the HTML-escaped step text.
	 * @return String  the step with each quoted value wrapped in a {@code <q>}.
	 */
	private static String highlight(String escaped) {
		return escaped.replaceAll("&quot;(.*?)&quot;", "<q>$1</q>");
	}

	/**
	 * @param error    the failure or skip reason.
	 * @return String  its message without the object ids TestNG adds to dependency skips.
	 */
	private static String message(Throwable error) {
		String message = error.getMessage() == null ? error.toString() : error.getMessage();
		return message.replaceAll(" on instance [\\w.$]+@\\p{XDigit}+", "");
	}

	/**
	 * @param error    the failure.
	 * @return String  the stack trace without TestNG, JDK and Maven frames.
	 */
	private static String trace(Throwable error) {
		StringBuilder out = new StringBuilder();
		for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
			out.append(t == error ? "" : "Caused by: ").append(t).append('\n');
			for (StackTraceElement element : t.getStackTrace()) {
				String line = element.toString();
				if (NOISE.stream().noneMatch(line::startsWith)) {
					out.append("    at ").append(line).append('\n');
				}
			}
		}
		return out.toString().trim();
	}

	/**
	 * @param millis   a duration in milliseconds.
	 * @return String  e.g. "850 ms", "12.4 s" or "2 min 05 s".
	 */
	private static String duration(long millis) {
		if (millis < 1000) {
			return millis + " ms";
		}
		if (millis < 60_000) {
			return String.format(Locale.ROOT, "%.1f s", millis / 1000.0);
		}
		return String.format(Locale.ROOT, "%d min %02d s", millis / 60_000, millis / 1000 % 60);
	}

	/**
	 * @param part     a count.
	 * @param total    the total count.
	 * @return String  the share in percent, for CSS widths.
	 */
	private static String percent(long part, long total) {
		return total == 0 ? "0" : String.format(Locale.ROOT, "%.2f", part * 100.0 / total);
	}

	/**
	 * @param text     any text.
	 * @return String  the text, safe in HTML content and attributes.
	 */
	private static String esc(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	// The page; {{name}} placeholders are filled in by render()
	private static final String TEMPLATE = """
			<!doctype html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title>{{title}} – Playwright test report</title>
			<style>
			/* Colours: light theme, then the dark theme */
			:root{--bg:#f3f6f5;--card:#fff;--ink:#14261f;--muted:#5f7369;--line:#dfe7e3;--accent:#0f9d76;--accent2:#2563eb;--hover:#f6faf8;
			--pass:#16a34a;--pass-bg:#e6f6ec;--fail:#dc2626;--fail-bg:#fdecec;--skip:#d97706;--skip-bg:#fdf3e2;--code:#eef3f1;
			--shadow:0 1px 2px rgba(10,40,30,.06),0 4px 16px rgba(10,40,30,.05)}
			[data-theme=dark]{--bg:#0d1714;--card:#14221d;--ink:#e3eee9;--muted:#93a89e;--line:#24372f;--accent:#34d3a0;--accent2:#60a5fa;--hover:#1a2c26;
			--pass:#4ade80;--pass-bg:#12301f;--fail:#f87171;--fail-bg:#3a1618;--skip:#fbbf24;--skip-bg:#33270e;--code:#0f1c18;--shadow:none}
			*{box-sizing:border-box}
			body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
			code,pre{font-family:ui-monospace,"Cascadia Code",Consolas,monospace}
			/* Header (slowly drifting gradient) and summary */
			header{background:linear-gradient(120deg,#064e3b,#0f9d76,#2563eb,#0f9d76,#064e3b);background-size:300% 300%;animation:drift 18s ease-in-out infinite;color:#fff;padding:30px 24px 86px}
			@keyframes drift{0%,100%{background-position:0% 50%}50%{background-position:100% 50%}}
			.wrap{max-width:1120px;margin:0 auto}
			.top{display:flex;justify-content:space-between;gap:16px;flex-wrap:wrap;align-items:flex-start}
			.eyebrow{font-size:12px;letter-spacing:.12em;text-transform:uppercase;opacity:.85}
			h1{margin:4px 0 6px;font-size:28px}
			.meta{opacity:.85;font-size:14px}
			.verdict{display:inline-flex;align-items:center;gap:8px;padding:8px 14px;border-radius:999px;font-weight:600;background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.3)}
			.verdict::before{content:"";width:10px;height:10px;border-radius:50%;background:#4ade80}
			.verdict.failed::before{background:#f87171;animation:pulse 1.8s ease-out infinite}.verdict.skipped::before{background:#fbbf24}
			@keyframes pulse{0%{box-shadow:0 0 0 0 rgba(248,113,113,.7)}100%{box-shadow:0 0 0 12px rgba(248,113,113,0)}}
			main{margin-top:-62px;padding:0 24px 48px}
			main>*{animation:rise .45s ease both}
			@keyframes rise{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}
			.panel{background:var(--card);border:1px solid var(--line);border-radius:16px;box-shadow:var(--shadow)}
			.summary{display:grid;grid-template-columns:auto 1fr;gap:28px;padding:24px;align-items:center}
			.rate{width:120px;height:120px;border-radius:50%;display:grid;place-content:center;text-align:center;background:conic-gradient(var(--pass) calc({{rate}}*1%),var(--line) 0)}
			.rate div{width:96px;height:96px;border-radius:50%;background:var(--card);display:grid;place-content:center}
			.rate b{font-size:28px;line-height:1}.rate small{color:var(--muted);font-size:12px}
			.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}
			.stat{padding:12px 14px;border-radius:12px;background:var(--code)}
			.stat span{display:block;color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.06em}
			.stat b{font-size:26px}.stat.p b{color:var(--pass)}.stat.f b{color:var(--fail)}.stat.s b{color:var(--skip)}
			.bar,.gbar{display:flex;border-radius:999px;overflow:hidden;background:var(--line)}.bar{height:10px;margin-top:16px}.gbar{height:8px}
			:is(.bar,.gbar) .p{background:var(--pass)}:is(.bar,.gbar) .f{background:var(--fail)}:is(.bar,.gbar) .s{background:var(--skip)}
			dl{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:4px 20px;margin:0;padding:16px 24px;border-top:1px solid var(--line)}
			dt{color:var(--muted);font-size:12px}dd{margin:0;font-weight:500;overflow-wrap:anywhere}
			/* Toolbar */
			.toolbar{position:sticky;top:0;z-index:5;display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin:20px 0;padding:12px;background:var(--bg)}
			.toolbar input{flex:1 1 220px;padding:9px 12px;border-radius:10px;border:1px solid var(--line);background:var(--card);color:var(--ink);font:inherit}
			.seg{display:inline-flex;border:1px solid var(--line);border-radius:10px;overflow:hidden;background:var(--card)}
			.seg button,.btn{border:0;background:var(--card);color:var(--ink);padding:8px 12px;font:inherit;font-size:14px;cursor:pointer}
			.seg button:hover,.btn:hover{background:var(--hover)}.seg button+button{border-left:1px solid var(--line)}
			.seg button[aria-pressed=true]{background:var(--accent);color:#fff}.btn{border:1px solid var(--line);border-radius:10px}
			/* Class groups and test cards; open and close animate their height */
			.group{background:var(--card);border:1px solid var(--line);border-left:4px solid var(--pass);border-radius:14px;margin-bottom:12px;box-shadow:var(--shadow);overflow:hidden}
			.group.failed{border-left-color:var(--fail)}.group.skipped{border-left-color:var(--skip)}
			.ghead,.head{all:unset;box-sizing:border-box;width:100%;display:grid;align-items:center;cursor:pointer}
			.ghead{grid-template-columns:minmax(0,1fr) auto 140px 76px auto;gap:16px;padding:14px 18px}
			.ghead:hover{background:var(--hover)}.ghead:focus-visible,.head:focus-visible{outline:2px solid var(--accent);outline-offset:-2px}
			.gname{font-weight:600}.gname small{display:block;color:var(--muted);font-weight:400;font-size:12px}
			.chips{display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end}
			.chip{font-size:12px;font-weight:600;padding:2px 9px;border-radius:999px;white-space:nowrap}
			.chip.passed{color:var(--pass);background:var(--pass-bg)}.chip.failed{color:var(--fail);background:var(--fail-bg)}.chip.skipped{color:var(--skip);background:var(--skip-bg)}
			.gtime,.time{color:var(--muted);font-size:13px;white-space:nowrap;text-align:right}
			.gbody,.body{display:grid;grid-template-rows:0fr;transition:grid-template-rows .35s ease}
			.group.open .gbody,.test[open] .body{grid-template-rows:1fr}
			.clip{overflow:hidden;min-height:0}.glist{padding:2px 14px 14px}
			.test{background:var(--card);border:1px solid var(--line);border-left:4px solid var(--pass);border-radius:12px;margin-top:10px;transition:transform .2s,box-shadow .2s}
			.test:hover{transform:translateY(-1px);box-shadow:0 8px 24px rgba(10,40,30,.10)}
			.test.failed{border-left-color:var(--fail)}.test.skipped{border-left-color:var(--skip)}
			.head{grid-template-columns:auto 1fr auto auto auto;gap:14px;padding:11px 16px}
			.pill{font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;padding:3px 8px;border-radius:6px}
			.pill.passed{color:var(--pass);background:var(--pass-bg)}.pill.failed{color:var(--fail);background:var(--fail-bg)}.pill.skipped{color:var(--skip);background:var(--skip-bg)}
			.name{min-width:0}.name strong{display:block;font-weight:600}.name code{color:var(--muted);font-size:12px}
			.tag{font-size:11px;color:var(--accent);background:var(--code);padding:2px 7px;border-radius:999px;margin-left:4px;white-space:nowrap}
			.chev{width:8px;height:8px;border-right:2px solid var(--muted);border-bottom:2px solid var(--muted);transform:rotate(-45deg);transition:transform .25s}
			.test[open] .chev,.group.open>.ghead .chev{transform:rotate(45deg)}
			.pad{padding:4px 20px 18px;border-top:1px solid var(--line)}
			/* Card body: steps, error, screenshot, video, trace */
			.steps{margin:14px 0;padding-left:22px}.steps li{padding:3px 0}.steps li::marker{color:var(--muted);font-size:12px}
			q{quotes:none;font-family:ui-monospace,Consolas,monospace;font-size:13px;background:var(--code);color:var(--accent);padding:1px 6px;border-radius:5px}
			.error,.skip{border-radius:10px;padding:12px 14px;margin:12px 0;background:var(--fail-bg)}.skip{background:var(--skip-bg)}
			.etitle{font-weight:600;color:var(--fail)}.skip .etitle{color:var(--skip)}
			pre{white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0;font-size:12.5px}
			details summary{cursor:pointer;color:var(--muted);font-size:13px;margin-top:8px}
			.trace{background:var(--code);padding:10px;border-radius:8px;max-height:320px;overflow:auto}
			.media{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:14px;margin-top:12px}
			.media figure{margin:0}.media img,.media video{width:100%;border:1px solid var(--line);border-radius:10px;display:block;background:#000}
			figcaption{color:var(--muted);font-size:12px;margin-top:6px}
			.tracebox{margin-top:12px;padding:10px 14px;border-radius:10px;background:var(--code);font-size:13.5px}.tracebox a{color:var(--accent2)}
			.muted{color:var(--muted)}.empty{display:none;text-align:center;color:var(--muted);padding:32px}
			.lightbox{position:fixed;inset:0;background:rgba(0,0,0,.8);display:none;place-content:center;padding:24px;z-index:10;cursor:zoom-out}
			.lightbox img{max-width:96vw;max-height:92vh;border-radius:8px}
			footer{text-align:center;color:var(--muted);font-size:12px;padding:0 0 32px}
			@media (max-width:720px){.summary{grid-template-columns:1fr;justify-items:center}.stats{grid-template-columns:repeat(2,1fr);width:100%}
			.ghead{grid-template-columns:minmax(0,1fr) auto auto}.gbar,.gtime,.tags{display:none}.head{grid-template-columns:auto 1fr auto}}
			@media print{header{animation:none}.toolbar,.chev{display:none}.gbody,.body{grid-template-rows:1fr!important}}
			@media (prefers-reduced-motion:reduce){*,*::before{animation:none!important;transition:none!important}}
			</style>
			</head>
			<body>
			<header><div class="wrap top">
			<div><div class="eyebrow">Playwright · TestNG · Sauce Demo + Restful Booker</div><h1>{{title}}</h1>
			<div class="meta">Started {{started}} · took {{duration}}</div></div>
			<div class="verdict {{verdict}}">{{verdictText}}</div>
			</div></header>
			<main class="wrap">
			<section class="panel">
			<div class="summary">
			<div class="rate" role="img" aria-label="Pass rate {{rate}} percent"><div><b>{{rate}}%</b><small>pass rate</small></div></div>
			<div><div class="stats">
			<div class="stat"><span>Total</span><b>{{total}}</b></div><div class="stat p"><span>Passed</span><b>{{passed}}</b></div>
			<div class="stat f"><span>Failed</span><b>{{failed}}</b></div><div class="stat s"><span>Skipped</span><b>{{skipped}}</b></div></div>
			<div class="bar"><i class="p" style="width:{{pPassed}}%"></i><i class="f" style="width:{{pFailed}}%"></i><i class="s" style="width:{{pSkipped}}%"></i></div></div>
			</div>
			<dl>{{env}}</dl>
			</section>
			<div class="toolbar">
			<input id="q" type="search" placeholder="Search tests, classes or @groups" aria-label="Search tests">
			<div class="seg"><button data-f="all" aria-pressed="true">All</button><button data-f="passed" aria-pressed="false">Passed</button>
			<button data-f="failed" aria-pressed="false">Failed</button><button data-f="skipped" aria-pressed="false">Skipped</button></div>
			<button class="btn" id="expand">Expand all</button><button class="btn" id="theme">Dark mode</button>
			</div>
			<div id="groups">{{groups}}</div>
			<p class="empty" id="empty">No tests match the filter.</p>
			</main>
			<footer>Generated by HtmlReportListener · playwright-framework</footer>
			<div class="lightbox" id="lightbox"><img alt="Screenshot"></div>
			<script>
			(function(){
			var tests=[].slice.call(document.querySelectorAll('.test')),groups=[].slice.call(document.querySelectorAll('.group')),filter='all',q=document.getElementById('q');
			// Cards and groups open on click; aria-expanded follows
			function syncTest(t){t.querySelector('.head').setAttribute('aria-expanded',t.hasAttribute('open'))}
			tests.forEach(function(t){t.querySelector('.head').addEventListener('click',function(){t.toggleAttribute('open');syncTest(t)})});
			function setGroup(g,open){g.classList.toggle('open',open);g.querySelector('.ghead').setAttribute('aria-expanded',open)}
			groups.forEach(function(g){g.querySelector('.ghead').addEventListener('click',function(){var o=!g.classList.contains('open');g.dataset.user=o?'1':'0';setGroup(g,o)})});
			// Search and status filter: groups with matches open, the rest hide; clearing restores the previous view
			function apply(){var term=q.value.trim().toLowerCase(),active=term!==''||filter!=='all',shown=0;
			tests.forEach(function(t){var ok=(filter==='all'||t.dataset.status===filter)&&t.dataset.search.indexOf(term)>=0;t.style.display=ok?'':'none';if(ok)shown++});
			groups.forEach(function(g){var any=[].some.call(g.querySelectorAll('.test'),function(t){return t.style.display!=='none'});g.style.display=any?'':'none';
			setGroup(g,active?any:(g.dataset.user?g.dataset.user==='1':g.dataset.default==='true'))});
			document.getElementById('empty').style.display=shown?'none':'block'}
			q.addEventListener('input',apply);
			document.querySelectorAll('.seg button').forEach(function(b){b.addEventListener('click',function(){filter=b.dataset.f;
			document.querySelectorAll('.seg button').forEach(function(x){x.setAttribute('aria-pressed',x===b)});apply()})});
			// Expand all / Collapse all
			var expand=document.getElementById('expand');
			expand.addEventListener('click',function(){var o=expand.textContent==='Expand all';groups.forEach(function(g){g.dataset.user=o?'1':'0';setGroup(g,o)});
			tests.forEach(function(t){t.toggleAttribute('open',o);syncTest(t)});expand.textContent=o?'Collapse all':'Expand all'});
			// Light/dark theme: the saved choice, else the system setting
			var root=document.documentElement,theme=document.getElementById('theme');
			function setTheme(d){root.dataset.theme=d?'dark':'light';theme.textContent=d?'Light mode':'Dark mode';try{localStorage.setItem('pw-report-theme',root.dataset.theme)}catch(e){}}
			var saved=null;try{saved=localStorage.getItem('pw-report-theme')}catch(e){}
			setTheme(saved?saved==='dark':window.matchMedia&&matchMedia('(prefers-color-scheme: dark)').matches);
			theme.addEventListener('click',function(){setTheme(root.dataset.theme!=='dark')});
			// Click a screenshot to see it full size
			var box=document.getElementById('lightbox');
			document.querySelectorAll('.zoom').forEach(function(a){a.addEventListener('click',function(e){e.preventDefault();box.querySelector('img').src=a.querySelector('img').src;box.style.display='grid'})});
			box.addEventListener('click',function(){box.style.display='none'});
			})();
			</script>
			</body>
			</html>
			""";
}
