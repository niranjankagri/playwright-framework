package com.automation.playwright.pages;

import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Parent of every page shown after login. Holds the header they share: the
 * page title, the cart icon with its item count, and the side menu.
 */
public abstract class ShopPage extends BasePage {

	// Page title under the header, e.g. "Products", "Your Cart"
	private final Locator title = byTestId("title");
	// Cart icon and the red count on it (absent when the cart is empty)
	private final Locator cartLink = byTestId("shopping-cart-link");
	private final Locator cartBadge = byTestId("shopping-cart-badge");
	// Burger button that opens the side menu, and its Logout entry
	private final Locator menuButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open Menu"));
	private final Locator logoutLink = byTestId("logout-sidebar-link");

	/**
	 * @param page the browser tab this page works on.
	 */
	protected ShopPage(Page page) {
		super(page);
	}

	/**
	 * @return Locator the page title, e.g. "Products".
	 */
	public Locator title() {
		return title;
	}

	/**
	 * @return Locator the item count on the cart icon.
	 */
	public Locator cartBadge() {
		return cartBadge;
	}

	/**
	 * Opens the cart from the header.
	 *
	 * @return CartPage the cart.
	 */
	public CartPage openCart() {
		Steps.log("Open the cart");
		cartLink.click();
		CartPage cart = new CartPage(page);
		cart.waitUntilLoaded();
		return cart;
	}

	/**
	 * Logs out through the side menu.
	 *
	 * @return LoginPage the login page shown after logging out.
	 */
	public LoginPage logout() {
		Steps.log("Log out from the side menu");
		menuButton.click();
		logoutLink.click();
		return new LoginPage(page);
	}
}
