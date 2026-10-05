package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The order confirmation ({@code /checkout-complete.html}).
 */
public class CheckoutCompletePage extends ShopPage {

	// "Thank you for your order!"
	private final Locator header = byTestId("complete-header");

	/**
	 * @param page the browser tab this page works on.
	 */
	public CheckoutCompletePage(Page page) {
		super(page);
	}

	/**
	 * Waits for the confirmation heading.
	 */
	public void waitUntilLoaded() {
		assertThat(header).isVisible();
	}

	/**
	 * @return Locator the confirmation heading.
	 */
	public Locator header() {
		return header;
	}
}
