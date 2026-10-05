package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The cart ({@code /cart.html}).
 */
public class CartPage extends ShopPage {

	// Names of the products in the cart, and the Checkout button
	private final Locator names = byTestId("inventory-item-name");
	private final Locator checkout = byTestId("checkout");

	/**
	 * @param page the browser tab this page works on.
	 */
	public CartPage(Page page) {
		super(page);
	}

	/**
	 * Waits for the Checkout button, which only the cart has.
	 */
	public void waitUntilLoaded() {
		assertThat(checkout).isVisible();
	}

	/**
	 * @return Locator the names of the products in the cart.
	 */
	public Locator items() {
		return names;
	}

	/**
	 * @return List the product names in the cart, top to bottom.
	 */
	public List<String> productNames() {
		return texts(names);
	}

	/**
	 * @return CheckoutInfoPage the first checkout step, after clicking Checkout.
	 */
	public CheckoutInfoPage checkout() {
		Steps.log("Check out");
		checkout.click();
		CheckoutInfoPage info = new CheckoutInfoPage(page);
		info.waitUntilLoaded();
		return info;
	}
}
