package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;

import com.automation.playwright.pages.factory.TestId;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * The cart ({@code /cart.html}).
 */
public class CartPage extends ShopPage {

	// Names of the products in the cart, and the Checkout button
	@TestId("cart-item-name")
	private Locator names;
	// One row per product in the cart
	@TestId("cart-item")
	private Locator rows;
	@TestId("checkout")
	private Locator checkout;

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
	/**
	 * Removes a product with the Remove button in its row; the cart reloads without it.
	 *
	 * @param name      the product name.
	 * @return CartPage this page.
	 */
	public CartPage remove(String name) {
		Steps.log("Remove \"" + name + "\" from the cart");
		Locator row = rows.filter(new Locator.FilterOptions().setHas(page.getByText(name, new Page.GetByTextOptions().setExact(true))));
		row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove")).click();
		assertThat(row).hasCount(0);
		return this;
	}

	public CheckoutInfoPage checkout() {
		Steps.log("Check out");
		checkout.click();
		CheckoutInfoPage info = new CheckoutInfoPage(page);
		info.waitUntilLoaded();
		return info;
	}
}
