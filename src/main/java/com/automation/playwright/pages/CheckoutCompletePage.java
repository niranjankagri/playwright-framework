package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.automation.playwright.pages.factory.TestId;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The order confirmation ({@code /checkout-complete.html}).
 */
public class CheckoutCompletePage extends ShopPage {

	// "Thank you for your order", and the order number under it
	@TestId("complete-title")
	private Locator header;
	@TestId("order-number")
	private Locator orderNumber;

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

	/**
	 * @return Locator the order number, e.g. "ORD-4821".
	 */
	public Locator orderNumber() {
		return orderNumber;
	}
}
