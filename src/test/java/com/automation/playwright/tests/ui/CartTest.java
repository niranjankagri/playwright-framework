package com.automation.playwright.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.CartPage;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.utils.Steps;

/**
 * The cart: adding and removing products, and keeping them on reload.
 * Each test starts in a fresh browser context, so the cart is always empty
 * at the start.
 */
@Test(groups = "cart")
public class CartTest extends UiTest {

	/**
	 * Adds two products: the badge shows 2 and the cart lists exactly them.
	 */
	@Test(groups = "smoke", description = "Added products appear in the cart")
	public void addedProductsAppearInTheCart() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER)
				.addToCart(TestData.BACKPACK)
				.addToCart(TestData.BIKE_LIGHT);
		assertThat(inventory.cartBadge()).hasText("2");

		CartPage cart = inventory.openCart();
		assertThat(cart.title()).hasText("Your Cart");
		assertThat(cart.items()).hasText(new String[] { TestData.BACKPACK, TestData.BIKE_LIGHT });
	}

	/**
	 * Adds a product and removes it again: the badge disappears.
	 */
	@Test(description = "Removing the only product empties the cart")
	public void removingTheOnlyProductEmptiesTheCart() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK);
		assertThat(inventory.cartBadge()).hasText("1");

		inventory.removeFromCart(TestData.BACKPACK);
		assertThat(inventory.cartBadge()).isHidden();
		assertThat(inventory.openCart().items()).hasCount(0);
	}

	/**
	 * Adds a product and reloads the page: Sauce Demo keeps the cart in the
	 * browser's local storage, so the badge must still show 1.
	 */
	@Test(description = "The cart survives a page reload")
	public void cartSurvivesAReload() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER).addToCart(TestData.BOLT_SHIRT);

		Steps.log("Reload the page");
		page().reload();
		assertThat(inventory.cartBadge()).hasText("1");
	}
}
