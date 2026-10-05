package com.automation.playwright.pages;

import com.automation.playwright.config.Config;
import com.automation.playwright.utils.Steps;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

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
	 */
	public InventoryPage loginAs(String user) {
		submit(user, Config.get().password());
		InventoryPage inventory = new InventoryPage(page);
		inventory.waitUntilLoaded();
		return inventory;
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
