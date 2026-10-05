package com.automation.playwright.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A finished HTTP response, read completely, so it can be checked after the
 * request context is closed.
 *
 * @param status  the HTTP status code, e.g. 200.
 * @param body    the response body as text.
 * @param millis  how long the request took.
 */
public record ApiResponse(int status, String body, long millis) {

	/**
	 * @param <T>   the target type.
	 * @param type  the class to map the body to.
	 * @return T    the body as that type.
	 */
	public <T> T as(Class<T> type) {
		return Json.read(body, type);
	}

	/**
	 * @return JsonNode the body as a JSON tree.
	 */
	public JsonNode json() {
		return Json.tree(body);
	}
}
