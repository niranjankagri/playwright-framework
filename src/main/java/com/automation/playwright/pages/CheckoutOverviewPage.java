package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Checkout step two: the order overview with its totals ({@code /checkout-step-two.html}).
 */
public class CheckoutOverviewPage extends ShopPage {

	// Ordered products, their prices, and the three totals
	private final Locator names = byTestId("inventory-item-name");
	private final Locator prices = byTestId("inventory-item-price");
	private final Locator itemTotal = byTestId("subtotal-label");
	private final Locator tax = byTestId("tax-label");
	private final Locator total = byTestId("total-label");
	private final Locator finish = byTestId("finish");

	/**
	 * @param page the browser tab this page works on.
	 */
	public CheckoutOverviewPage(Page page) {
		super(page);
	}

	/**
	 * Waits for the Finish button: names and totals are read without retrying, so the page must be drawn first.
	 */
	public void waitUntilLoaded() {
		assertThat(finish).isVisible();
	}

	/**
	 * @return List the ordered product names.
	 */
	public List<String> productNames() {
		return texts(names);
	}

	/**
	 * @return List the prices of the ordered products.
	 */
	public List<Double> productPrices() {
		return texts(prices).stream().map(BasePage::money).toList();
	}

	/**
	 * @return double "Item total", the sum before tax.
	 */
	public double itemTotal() {
		return money(itemTotal.innerText());
	}

	/**
	 * @return double the tax.
	 */
	public double tax() {
		return money(tax.innerText());
	}

	/**
	 * @return double "Total", item total plus tax.
	 */
	public double total() {
		return money(total.innerText());
	}

	/**
	 * @return CheckoutCompletePage the confirmation, after clicking Finish.
	 */
	public CheckoutCompletePage finish() {
		Steps.log("Finish the order");
		finish.click();
		CheckoutCompletePage complete = new CheckoutCompletePage(page);
		complete.waitUntilLoaded();
		return complete;
	}
}
