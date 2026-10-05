package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.List;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * The product list ({@code /inventory.html}), shown after login.
 */
public class InventoryPage extends ShopPage {

	// Path of the page, relative to the base URL
	static final String PATH = "/inventory.html";

	// One card per product, and the name and price inside the cards
	private final Locator items = byTestId("inventory-item");
	private final Locator names = byTestId("inventory-item-name");
	private final Locator prices = byTestId("inventory-item-price");
	// Sort drop-down: a real <select>, so selectOption() works
	private final Locator sort = byTestId("product-sort-container");

	/**
	 * @param page the browser tab this page works on.
	 */
	public InventoryPage(Page page) {
		super(page);
	}

	/**
	 * Opens the product list by its URL (the user must already be logged in).
	 *
	 * @return InventoryPage this page.
	 */
	public InventoryPage open() {
		Steps.log("Go to " + PATH);
		page.navigate(PATH);
		return this;
	}

	/**
	 * Waits until the product cards are shown.
	 */
	public void waitUntilLoaded() {
		assertThat(title()).hasText("Products");
		assertThat(items.first()).isVisible();
	}

	/**
	 * @return Locator every product card.
	 */
	public Locator items() {
		return items;
	}

	/**
	 * @return List the product names, top to bottom.
	 */
	public List<String> productNames() {
		return texts(names);
	}

	/**
	 * @return List the product prices, top to bottom.
	 */
	public List<Double> productPrices() {
		return texts(prices).stream().map(BasePage::money).toList();
	}

	/**
	 * @param option         the option value: "az", "za", "lohi" or "hilo".
	 * @return InventoryPage this page, re-sorted.
	 */
	public InventoryPage sortBy(String option) {
		Steps.log("Sort products by \"" + option + "\"");
		sort.selectOption(option);
		return this;
	}

	/**
	 * Adds a product to the cart with the button on its card.
	 *
	 * @param name           the product name, e.g. "Sauce Labs Backpack".
	 * @return InventoryPage this page.
	 */
	public InventoryPage addToCart(String name) {
		Steps.log("Add \"" + name + "\" to the cart");
		card(name).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart")).click();
		return this;
	}

	/**
	 * Removes a product from the cart with the button on its card.
	 *
	 * @param name           the product name.
	 * @return InventoryPage this page.
	 */
	public InventoryPage removeFromCart(String name) {
		Steps.log("Remove \"" + name + "\" from the cart");
		card(name).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove")).click();
		return this;
	}

	/**
	 * @param name     the product name.
	 * @return double  the price shown on its card, e.g. 29.99.
	 */
	public double priceOf(String name) {
		return money(card(name).getByTestId("inventory-item-price").innerText());
	}

	/**
	 * Opens a product's detail page with the title link on its card, and
	 * waits until the detail page is drawn (the URL changes before the page does).
	 *
	 * @param name         the product name.
	 * @return ProductPage the detail page.
	 */
	public ProductPage openProduct(String name) {
		Steps.log("Open the details of \"" + name + "\"");
		card(name).locator("[data-test$='title-link']").click();
		ProductPage product = new ProductPage(page);
		product.waitUntilLoaded();
		return product;
	}

	/**
	 * @param name     the product name.
	 * @return Locator the product's card; filter() keeps only the card that contains that name.
	 */
	private Locator card(String name) {
		return items.filter(new Locator.FilterOptions().setHas(page.getByText(name, new Page.GetByTextOptions().setExact(true))));
	}
}
