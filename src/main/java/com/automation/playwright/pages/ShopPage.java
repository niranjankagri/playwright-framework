package com.automation.playwright.pages;

import com.automation.playwright.pages.factory.Role;
import com.automation.playwright.pages.factory.TestId;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Parent of every page shown after login. Holds the header they share: the
 * page title, the cart icon with its item count, and the side menu.
 */
public abstract class ShopPage extends BasePage {

	// Clicks on the burger button before giving up, and how long each waits for the menu
	private static final int MENU_ATTEMPTS = 3;
	private static final double MENU_WAIT_MILLIS = 3000;

	// Page title under the header, e.g. "Products", "Your Cart"
	@TestId("title")
	private Locator title;
	// Cart icon and the red count on it (absent when the cart is empty)
	@TestId("shopping-cart-link")
	private Locator cartLink;
	@TestId("shopping-cart-badge")
	private Locator cartBadge;
	// Burger button that opens the side menu, and its Logout entry
	@Role(value = AriaRole.BUTTON, name = "Open Menu")
	private Locator menuButton;
	@TestId("logout-sidebar-link")
	private Locator logoutLink;

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
	 * @return String the login as saved by storageState(): cookies and local
	 *         storage as JSON, for starting another context already logged in.
	 */
	public String savedLogin() {
		Steps.log("Save the login (storage state: cookies + local storage)");
		return page.context().storageState();
	}

	/**
	 * @return String the cart as the shop keeps it in local storage, e.g. "[4]" (product ids).
	 */
	public String cartInStorage() {
		return (String) page.evaluate("() => localStorage.getItem('cart-contents')");
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
	 * Logs out through the side menu. The Logout link counts as visible while
	 * the menu is still sliding in, and in Firefox the menu can close again
	 * before the click lands (CI run 40272507304: "element is not stable",
	 * then "element is not visible" until the 10 s timeout). So each click
	 * gets a short timeout, and the menu is opened again if it closed.
	 *
	 * @return LoginPage the login page shown after logging out.
	 */
	public LoginPage logout() {
		Steps.log("Log out from the side menu");
		for (int attempt = 1; attempt <= MENU_ATTEMPTS; attempt++) {
			openMenu();
			try {
				logoutLink.click(new Locator.ClickOptions().setTimeout(MENU_WAIT_MILLIS));
				return new LoginPage(page);
			} catch (TimeoutError e) {
				Steps.log("The side menu closed before Logout was clicked, opening it again (" + attempt + " of " + MENU_ATTEMPTS + ")");
			}
		}
		throw new IllegalStateException("Logout could not be clicked after " + MENU_ATTEMPTS + " attempts");
	}

	/**
	 * Opens the side menu unless it is already open (another click on the
	 * burger button would close it). In Firefox a click on the burger button
	 * sometimes does not open the menu (the page is still busy), and the Logout
	 * link then never becomes visible; so the click is repeated until the menu shows.
	 */
	private void openMenu() {
		if (logoutLink.isVisible()) {
			return;
		}
		for (int attempt = 1; attempt <= MENU_ATTEMPTS; attempt++) {
			menuButton.click();
			try {
				logoutLink.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(MENU_WAIT_MILLIS));
				return;
			} catch (TimeoutError e) {
				Steps.log("The side menu did not open, clicking again (" + attempt + " of " + MENU_ATTEMPTS + ")");
			}
		}
		throw new IllegalStateException("The side menu did not open after " + MENU_ATTEMPTS + " clicks");
	}
}
