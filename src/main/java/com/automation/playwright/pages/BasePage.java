package com.automation.playwright.pages;

import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Parent of all page objects. Playwright waits for every element by itself
 * (actionability checks) and its assertions retry until they pass, so pages
 * need no explicit waits; this class only holds what every page shares.
 */
public abstract class BasePage {

	// The browser tab this page works on
	protected final Page page;

	/**
	 * @param page the browser tab this page works on.
	 */
	protected BasePage(Page page) {
		this.page = page;
	}

	/**
	 * @return Page the browser tab, for checks with {@code assertThat(page)}.
	 */
	public Page page() {
		return page;
	}

	/**
	 * @param testId  the element's data-test value, e.g. "login-button".
	 * @return Locator a lazy locator; nothing is searched until it is used.
	 */
	protected Locator byTestId(String testId) {
		return page.getByTestId(testId);
	}

	/**
	 * @param locator the elements to read.
	 * @return List   the trimmed text of every match, in page order.
	 */
	protected List<String> texts(Locator locator) {
		return locator.allInnerTexts().stream().map(String::trim).toList();
	}

	/**
	 * @param priceText a price as shown, e.g. "$29.99" or "Item total: $39.98".
	 * @return double   the number, e.g. 29.99.
	 */
	protected static double money(String priceText) {
		return Double.parseDouble(priceText.substring(priceText.indexOf('$') + 1).trim());
	}
}
