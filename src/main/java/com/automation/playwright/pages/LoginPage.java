package com.automation.playwright.pages;

import java.util.LinkedHashMap;
import java.util.Map;

import com.automation.playwright.config.Config;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;

/**
 * The Sauce Demo login page ({@code /}).
 */
public class LoginPage extends BasePage {

	// User name and password inputs, and the Login button
	private final Locator username = byTestId("username");
	private final Locator password = byTestId("password");
	private final Locator loginButton = byTestId("login-button");
	// Red banner above the button, e.g. "Epic sadface: Username is required"
	private final Locator error = byTestId("error");
	// The whole login form, for the element screenshot
	private final Locator form = page.locator("form");

	/**
	 * @param page the browser tab this page works on.
	 */
	public LoginPage(Page page) {
		super(page);
	}

	/**
	 * Opens the login page (the context's base URL).
	 *
	 * @return LoginPage this page.
	 */
	public LoginPage open() {
		Steps.log("Open the login page");
		page.navigate("/");
		return this;
	}

	/**
	 * Fills in the form and submits it, without checking the outcome.
	 *
	 * @param user      the user name; empty leaves the field empty.
	 * @param secret    the password; empty leaves the field empty.
	 * @return LoginPage this page, for checking the error banner.
	 */
	public LoginPage submit(String user, String secret) {
		Steps.log("Log in with username \"" + user + "\" and password \"" + "*".repeat(secret.length()) + "\"");
		// fill() clears the field first and types the new value in one step
		username.fill(user);
		password.fill(secret);
		loginButton.click();
		return this;
	}

	/**
	 * Logs in with the shared Sauce Demo password and waits for the products.
	 *
	 * @param user           the user name, e.g. "standard_user".
	 * @return InventoryPage the product list shown after login.
	 * @throws IllegalStateException with the shop's error text if the login is refused.
	 */
	public InventoryPage loginAs(String user) {
		submit(user, Config.get().password());
		InventoryPage inventory = new InventoryPage(page);
		// Waits for whichever comes first: the product list or the error banner
		inventory.title().or(error).first().waitFor();
		if (error.isVisible()) {
			// Fails at once with the reason, instead of a timeout waiting for the products
			throw new IllegalStateException("Login as \"" + user + "\" failed: " + error.innerText());
		}
		inventory.waitUntilLoaded();
		return inventory;
	}

	/**
	 * Logs in with the keyboard only: types into the focused field, moves on
	 * with Tab and submits with Enter, as a user without a mouse would.
	 *
	 * @param user           the user name.
	 * @return InventoryPage the product list shown after login.
	 */
	public InventoryPage loginWithKeyboard(String user) {
		Steps.log("Log in as \"" + user + "\" with the keyboard (Tab, Enter)");
		username.focus();
		page.keyboard().type(user);
		page.keyboard().press("Tab");
		page.keyboard().type(Config.get().password());
		page.keyboard().press("Enter");
		InventoryPage inventory = new InventoryPage(page);
		inventory.waitUntilLoaded();
		return inventory;
	}

	/**
	 * @return byte[] a PNG of the login form only (an element screenshot).
	 */
	public byte[] formScreenshot() {
		Steps.log("Take a screenshot of the login form");
		return form.screenshot();
	}

	/**
	 * Logs in and returns the first product image the browser downloads:
	 * waitForResponse() starts listening before the login, so the response
	 * cannot be missed.
	 *
	 * @param user      the user name.
	 * @return Response the first .jpg response after logging in.
	 */
	public Response loginAndWaitForFirstImage(String user) {
		return page.waitForResponse(response -> response.url().endsWith(".jpg"), () -> loginAs(user));
	}

	/**
	 * The user name field found in five ways, for comparing locator strategies:
	 * test id, placeholder, CSS, XPath and role.
	 *
	 * @return Map strategy name → locator, in that order.
	 */
	public Map<String, Locator> usernameByEveryStrategy() {
		Map<String, Locator> strategies = new LinkedHashMap<>();
		strategies.put("getByTestId", username);
		strategies.put("getByPlaceholder", page.getByPlaceholder("Username"));
		strategies.put("CSS", page.locator("#user-name"));
		strategies.put("XPath", page.locator("//input[@name='user-name']"));
		strategies.put("getByRole", page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")));
		return strategies;
	}

	/**
	 * @return Locator the Login button found by its role and name.
	 */
	public Locator loginButtonByRole() {
		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
	}

	/**
	 * @return Locator the error banner (use with assertThat, which waits for it).
	 */
	public Locator error() {
		return error;
	}

	/**
	 * @return Locator the Login button (visible only on the login page).
	 */
	public Locator loginButton() {
		return loginButton;
	}
}
