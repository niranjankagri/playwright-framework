package com.automation.playwright.pages;

import java.util.LinkedHashMap;
import java.util.Map;

import com.automation.playwright.config.Config;
import com.automation.playwright.pages.factory.Css;
import com.automation.playwright.pages.factory.Placeholder;
import com.automation.playwright.pages.factory.Role;
import com.automation.playwright.pages.factory.TestId;
import com.automation.playwright.pages.factory.XPath;
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
	@TestId("username")
	private Locator username;
	@TestId("password")
	private Locator password;
	@TestId("login-button")
	private Locator loginButton;
	// Red banner above the button, e.g. "Epic sadface: Username is required"
	@TestId("error")
	private Locator error;
	// The whole login form, for the element screenshot
	@Css("form")
	private Locator form;
	// The same user name field and Login button found in other ways, for the locator-strategy example
	@Placeholder("Username")
	private Locator usernameByPlaceholder;
	@Css("#user-name")
	private Locator usernameByCss;
	@XPath("//input[@name='user-name']")
	private Locator usernameByXPath;
	@Role(value = AriaRole.TEXTBOX, name = "Username")
	private Locator usernameByRole;
	@Role(value = AriaRole.BUTTON, name = "Login", exact = true)
	private Locator loginButtonByRole;

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
		strategies.put("@TestId", username);
		strategies.put("@Placeholder", usernameByPlaceholder);
		strategies.put("@Css", usernameByCss);
		strategies.put("@XPath", usernameByXPath);
		strategies.put("@Role", usernameByRole);
		return strategies;
	}

	/**
	 * @return Locator the Login button found by its role and name.
	 */
	public Locator loginButtonByRole() {
		return loginButtonByRole;
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
