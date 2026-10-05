package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.automation.playwright.pages.factory.TestId;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A product's detail page ({@code /inventory-item.html?id=...}).
 */
public class ProductPage extends ShopPage {

	// Name, price and the button back to the list
	@TestId("inventory-item-name")
	private Locator name;
	@TestId("inventory-item-price")
	private Locator price;
	@TestId("back-to-products")
	private Locator back;

	/**
	 * @param page the browser tab this page works on.
	 */
	public ProductPage(Page page) {
		super(page);
	}

	/**
	 * Waits for the Back button, which only the detail page has. The URL changes
	 * before this single-page app draws the page, so the URL alone is not enough.
	 */
	public void waitUntilLoaded() {
		assertThat(back).isVisible();
	}

	/**
	 * @return Locator the product name.
	 */
	public Locator name() {
		return name;
	}

	/**
	 * @return double the price shown, e.g. 29.99.
	 */
	public double price() {
		return money(price.innerText());
	}

	/**
	 * @return InventoryPage the product list, after clicking "Back to products".
	 */
	public InventoryPage backToProducts() {
		Steps.log("Go back to the products");
		back.click();
		InventoryPage inventory = new InventoryPage(page);
		inventory.waitUntilLoaded();
		return inventory;
	}
}
