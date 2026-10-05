package com.automation.playwright.pages;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A JSON document opened directly in the browser, e.g. an API address.
 * Used to show what the browser receives when a response is mocked.
 */
public class JsonPage extends BasePage {

	// The browser shows the raw JSON as the page body
	private final Locator body = page.locator("body");

	/**
	 * @param page the browser tab this page works on.
	 */
	public JsonPage(Page page) {
		super(page);
	}

	/**
	 * @param url       the full address of the JSON document.
	 * @return JsonPage this page.
	 */
	public JsonPage open(String url) {
		Steps.log("Open " + url + " in the browser");
		page.navigate(url);
		return this;
	}

	/**
	 * @return Locator the page body holding the JSON text.
	 */
	public Locator body() {
		return body;
	}
}
