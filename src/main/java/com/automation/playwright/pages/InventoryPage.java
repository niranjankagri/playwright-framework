package com.automation.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.automation.playwright.utils.Steps;
import com.automation.playwright.config.Config;
import com.microsoft.playwright.APIResponse;
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
	// The product pictures, one per card
	private final Locator images = items.locator("img");

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
	 * @return List the image address (src) of every product card, top to bottom.
	 */
	public List<String> imageSources() {
		return images.all().stream().map(image -> image.getAttribute("src")).toList();
	}

	/**
	 * Requests every product image with the page's own request client
	 * (page.request(), which shares the browser's cookies).
	 *
	 * @return Map image address → HTTP status, top to bottom.
	 */
	public Map<String, Integer> imageStatuses() {
		Map<String, Integer> statuses = new LinkedHashMap<>();
		for (String src : imageSources()) {
			APIResponse response = page.request().get(Config.get().baseUrl() + src);
			Steps.log(response.status() + " " + src);
			statuses.put(src, response.status());
			response.dispose();
		}
		return statuses;
	}

	/**
	 * @return int the natural width of the first product image; 0 if it never loaded.
	 */
	public int firstImageWidth() {
		return ((Number) images.first().evaluate("img => img.naturalWidth")).intValue();
	}

	/**
	 * Opens the product list in a new tab of the same browser context (same cookies).
	 *
	 * @return InventoryPage the product list in the new tab.
	 */
	public InventoryPage openInNewTab() {
		Steps.log("Open the product list in a new tab");
		InventoryPage tab = new InventoryPage(page.context().newPage()).open();
		tab.waitUntilLoaded();
		return tab;
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
