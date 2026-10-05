package com.automation.playwright.browser;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import com.automation.playwright.config.Config;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Owns the Playwright objects of each test thread.
 * <p>
 * Playwright objects must not be shared between threads, so every thread
 * gets its own {@link Playwright} and {@link Browser}, created once and
 * reused by the tests of that thread. Every UI test then gets a fresh
 * {@link BrowserContext} (its own cookies, storage, trace and video), which
 * is fast and keeps tests isolated.
 */
public final class BrowserManager {

	// Where the traces and videos of failed tests are kept
	public static final Path ARTIFACT_DIR = Path.of("target", "artifacts");
	// Videos are recorded here first; a passed test's video is deleted afterwards
	private static final Path VIDEO_TEMP_DIR = Path.of("target", "video-temp");
	// Viewport of every test page (the video is recorded at the same size)
	private static final int WIDTH = 1440;
	private static final int HEIGHT = 900;

	// Every Playwright instance created, so all of them can be closed at the end of the run
	private static final List<Playwright> ALL = new CopyOnWriteArrayList<>();
	// One Playwright and one browser per thread
	private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
	private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
	// The context and page of the UI test running on this thread
	private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
	private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

	/**
	 * The trace and video kept for a test; null where nothing was kept.
	 *
	 * @param trace  the trace zip, open it with {@code mvn exec:java -Dexec.args="show-trace <zip>"}.
	 * @param video  the video (webm).
	 */
	public record Artifacts(Path trace, Path video) {
	}

	// Static helper only
	private BrowserManager() {
	}

	/**
	 * @return Playwright the Playwright of the current thread, created on first use.
	 */
	public static Playwright playwright() {
		Playwright playwright = PLAYWRIGHT.get();
		if (playwright == null) {
			playwright = Playwright.create();
			// Sauce Demo marks its elements with data-test, so getByTestId() reads that attribute
			playwright.selectors().setTestIdAttribute("data-test");
			// Auto-waiting assertions (assertThat) wait as long as page actions
			PlaywrightAssertions.setDefaultAssertionTimeout(Config.get().timeoutMillis());
			PLAYWRIGHT.set(playwright);
			ALL.add(playwright);
		}
		return playwright;
	}

	/**
	 * Opens a fresh context and page for a UI test, and starts its trace and
	 * video recording when they are switched on in config.properties.
	 *
	 * @param name  the test name, shown as the trace title.
	 * @return Page the new page, with the web shop as its base URL.
	 */
	public static Page startTest(String name) {
		Config config = Config.get();
		Browser.NewContextOptions options = new Browser.NewContextOptions()
				.setBaseURL(config.baseUrl())
				.setViewportSize(WIDTH, HEIGHT);
		if (config.video()) {
			options.setRecordVideoDir(VIDEO_TEMP_DIR).setRecordVideoSize(WIDTH, HEIGHT);
		}
		BrowserContext context = browser().newContext(options);
		context.setDefaultTimeout(config.timeoutMillis());
		if (config.trace()) {
			// Screenshots and DOM snapshots make the trace replayable step by step
			context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setTitle(name));
		}
		Page page = context.newPage();
		CONTEXT.set(context);
		PAGE.set(page);
		return page;
	}

	/**
	 * Opens an extra context on this thread's browser from a saved login, e.g.
	 * to show that it starts logged in. No trace or video; the caller closes it.
	 *
	 * @param storageState   the state saved with storageState() (cookies + local storage).
	 * @return BrowserContext the new context, with the web shop as its base URL.
	 */
	public static BrowserContext newContextWith(String storageState) {
		BrowserContext context = browser().newContext(new Browser.NewContextOptions()
				.setBaseURL(Config.get().baseUrl())
				.setViewportSize(WIDTH, HEIGHT)
				.setStorageState(storageState));
		context.setDefaultTimeout(Config.get().timeoutMillis());
		return context;
	}

	/**
	 * @return Page the page of the UI test running on this thread.
	 * @throws IllegalStateException if no UI test is running on this thread.
	 */
	public static Page page() {
		Page page = PAGE.get();
		if (page == null) {
			throw new IllegalStateException("No page open on this thread");
		}
		return page;
	}

	/**
	 * @return boolean true if a UI test page is open on this thread.
	 */
	public static boolean hasPage() {
		return PAGE.get() != null;
	}

	/**
	 * Ends the current UI test: stops the trace, closes the context and keeps
	 * the trace and video only if the test failed.
	 *
	 * @param fileName  the base file name for kept files, e.g. "LoginTest_lockedOutUser_1712345".
	 * @param keep      true to keep the trace and video (the test failed).
	 * @return Artifacts the files kept.
	 */
	public static Artifacts finishTest(String fileName, boolean keep) {
		BrowserContext context = CONTEXT.get();
		Page page = PAGE.get();
		if (context == null) {
			return new Artifacts(null, null);
		}
		Path trace = null;
		Path video = null;
		try {
			if (Config.get().trace()) {
				if (keep) {
					trace = ARTIFACT_DIR.resolve(fileName + "-trace.zip");
					context.tracing().stop(new Tracing.StopOptions().setPath(trace));
				} else {
					// Stopping without a path discards the trace
					context.tracing().stop();
				}
			}
		} finally {
			Video recording = page == null ? null : page.video();
			// The video file is only complete once the context is closed
			context.close();
			if (recording != null) {
				if (keep) {
					video = ARTIFACT_DIR.resolve(fileName + "-video.webm");
					recording.saveAs(video);
				}
				recording.delete();
			}
			CONTEXT.remove();
			PAGE.remove();
		}
		return new Artifacts(trace, video);
	}

	/**
	 * Closes every Playwright instance of the run (and with it every browser).
	 * Call once, after all tests have finished.
	 */
	public static void closeAll() {
		for (Playwright playwright : ALL) {
			try {
				playwright.close();
			} catch (RuntimeException e) {
				System.err.println("Could not close Playwright: " + e.getMessage());
			}
		}
		ALL.clear();
	}

	/**
	 * @return Browser the browser of the current thread, launched on first use.
	 */
	private static Browser browser() {
		Browser browser = BROWSER.get();
		if (browser == null || !browser.isConnected()) {
			Config config = Config.get();
			BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
					.setHeadless(config.headless())
					.setSlowMo(config.slowMo());
			browser = browserType(config.browser()).launch(options);
			BROWSER.set(browser);
		}
		return browser;
	}

	/**
	 * @param name          chromium, firefox or webkit (case-insensitive).
	 * @return BrowserType  the matching browser engine.
	 */
	private static BrowserType browserType(String name) {
		Playwright playwright = playwright();
		return switch (name.toLowerCase(Locale.ROOT)) {
		case "chromium", "chrome" -> playwright.chromium();
		case "firefox" -> playwright.firefox();
		case "webkit", "safari" -> playwright.webkit();
		default -> throw new IllegalArgumentException("Unsupported browser '" + name + "' (use chromium, firefox or webkit)");
		};
	}
}
