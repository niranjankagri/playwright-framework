package com.automation.playwright.pages;

import java.util.Optional;

import com.automation.playwright.pages.factory.Css;
import com.automation.playwright.pages.factory.TestId;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;

/**
 * Parent of every page shown after login. Holds the header they share: who is
 * signed in, the cart link with its item count, and the Sign out button; and
 * the page heading.
 */
public abstract class ShopPage extends BasePage {

	// Page heading, e.g. "Products", "Your cart" (every shop page has one h1 in main)
	@Css("main h1")
	private Locator title;
	// Cart link in the header and the count in it ("Cart (2)"; shows 0 for an empty cart)
	@TestId("cart-link")
	private Locator cartLink;
	@TestId("cart-count")
	private Locator cartBadge;
	// Sign out button in the header
	@TestId("logout")
	private Locator logoutButton;

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
	 * @return Locator the item count in the cart link.
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
	 * @return Optional the shop's session cookie (SESSION), as the browser context keeps it.
	 */
	public Optional<Cookie> sessionCookie() {
		return page.context().cookies().stream().filter(cookie -> cookie.name.equals("SESSION")).findFirst();
	}

	/**
	 * @return String the cookies JavaScript in the page can read (document.cookie).
	 */
	public String cookiesVisibleToJavaScript() {
		return (String) page.evaluate("() => document.cookie");
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
	 * Logs out with the Sign out button in the header.
	 *
	 * @return LoginPage the login page shown after logging out.
	 */
	public LoginPage logout() {
		Steps.log("Sign out");
		logoutButton.click();
		LoginPage login = new LoginPage(page);
		login.waitUntilLoaded();
		return login;
	}
}
