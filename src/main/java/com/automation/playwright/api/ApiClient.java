package com.automation.playwright.api;

import java.util.Map;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

/**
 * Sends HTTP requests with Playwright's own API client
 * ({@link APIRequestContext}), so UI and API tests use one library.
 * Every request is logged as a step, e.g. {@code POST /booking → 200 (312 ms)}.
 * <p>
 * Create one per test and close it afterwards (try-with-resources or
 * {@code @AfterMethod}); it holds its own cookies and connections.
 */
public class ApiClient implements AutoCloseable {

	// Playwright's request context: base URL, default headers, cookies
	private final APIRequestContext request;

	/**
	 * @param playwright the Playwright of the current thread.
	 * @param baseUrl    the API's base URL; request paths are relative to it.
	 */
	public ApiClient(Playwright playwright, String baseUrl) {
		this.request = playwright.request().newContext(new APIRequest.NewContextOptions()
				.setBaseURL(baseUrl)
				// Restful Booker answers in XML unless JSON is asked for
				.setExtraHTTPHeaders(Map.of("Accept", "application/json")));
	}

	/**
	 * @param path          the path, e.g. "/booking/1".
	 * @return ApiResponse  the response.
	 */
	public ApiResponse get(String path) {
		return send("GET", path, null, Map.of(), Map.of());
	}

	/**
	 * @param path          the path.
	 * @param query         query parameters, e.g. firstname=Jim.
	 * @return ApiResponse  the response.
	 */
	public ApiResponse get(String path, Map<String, String> query) {
		return send("GET", path, null, Map.of(), query);
	}

	/**
	 * Sends a request, with a JSON body when one is given.
	 *
	 * @param method        GET, POST, PUT, PATCH or DELETE.
	 * @param path          the path, relative to the base URL.
	 * @param body          the body object, sent as JSON; null for none.
	 * @param headers       extra headers, e.g. Cookie: token=...
	 * @param query         query parameters.
	 * @return ApiResponse  the full response (failing statuses are returned, not thrown).
	 */
	public ApiResponse send(String method, String path, Object body, Map<String, String> headers, Map<String, String> query) {
		RequestOptions options = RequestOptions.create().setMethod(method);
		headers.forEach(options::setHeader);
		query.forEach(options::setQueryParam);
		if (body != null) {
			options.setHeader("Content-Type", "application/json").setData(Json.write(body));
		}
		long start = System.nanoTime();
		APIResponse response = request.fetch(path, options);
		long millis = (System.nanoTime() - start) / 1_000_000;
		ApiResponse result = new ApiResponse(response.status(), response.text(), millis);
		response.dispose();
		Steps.log(method + " \"" + path + (query.isEmpty() ? "" : "?" + query) + "\" → " + result.status() + " (" + millis + " ms)");
		return result;
	}

	/**
	 * Releases the request context and its connections.
	 */
	@Override
	public void close() {
		request.dispose();
	}
}
