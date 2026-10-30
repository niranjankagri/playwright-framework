package com.automation.playwright.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.InventoryPage;
import com.automation.playwright.pages.LoginPage;

/**
 * Logging in and out of Sauce Demo, and pages that need a login.
 * The checks use Playwright's assertThat, which retries until the page
 * matches or the timeout ends, so no waits are needed.
 */
@Test(groups = "login")
public class LoginTest extends UiTest {

	/**
	 * Logs in as the standard user and checks the product list: the URL, the
	 * title and all 6 products.
	 */
	@Test(groups = "smoke", description = "[TC-101] Standard user logs in and sees the products")
	public void tc101_standardUserSeesProducts() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);

		assertThat(inventory.page()).hasURL(Pattern.compile(".*/inventory\\.html"));
		assertThat(inventory.title()).hasText("Products");
		assertThat(inventory.items()).hasCount(6);
	}

	/**
	 * Submits each rejected login from {@link TestData#rejectedLogins()} and
	 * checks the exact error banner; the user must stay on the login page.
	 *
	 * @param caseName  what is wrong with the login, shown in the report.
	 * @param user      the user name to submit.
	 * @param password  the password to submit.
	 * @param expected  the error banner text.
	 */
	@Test(dataProvider = "rejectedLogins", dataProviderClass = TestData.class, description = "[TC-102] Login is rejected")
	public void tc102_loginIsRejected(String caseName, String user, String password, String expected) {
		LoginPage login = openLoginPage().submit(user, password);

		assertThat(login.error()).hasText(expected);
		assertThat(login.loginButton()).isVisible();
	}

	/**
	 * Logs in, logs out through the side menu and checks the login form is back.
	 */
	@Test(groups = "smoke", description = "[TC-103] User logs out and returns to the login page")
	public void tc103_userCanLogOut() {
		LoginPage login = loginAs(TestData.STANDARD_USER).logout();

		assertThat(login.loginButton()).isVisible();
		assertThat(login.page()).not().hasURL(Pattern.compile(".*/inventory\\.html"));
	}

	/**
	 * Logs out, then presses the browser's Back button. The shop must not show
	 * the products again: the login cookie is gone, so it shows the login form
	 * with an error that names the page.
	 */
	@Test(description = "[TC-104] Back after logout does not show the products")
	public void tc104_backAfterLogoutDoesNotShowTheProducts() {
		LoginPage login = loginAs(TestData.STANDARD_USER).logout();
		assertThat(login.loginButton()).isVisible();

		login.goBack();
		assertThat(login.loginButton()).isVisible();
		assertThat(login.error()).containsText("when you are logged in");
	}

	/**
	 * Opens the product list URL without logging in. The shop must refuse it
	 * and name the page in its error.
	 */
	@Test(description = "[TC-105] The product list requires a login")
	public void tc105_productListRequiresLogin() {
		LoginPage login = openProductListLoggedOut();

		assertThat(login.error()).hasText("Epic sadface: You can only access '/inventory.html' when you are logged in.");
	}
}
