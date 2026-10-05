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
	@Test(groups = "smoke", description = "Standard user logs in and sees the products")
	public void standardUserSeesProducts() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER);

		assertThat(page()).hasURL(Pattern.compile(".*/inventory\\.html"));
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
	@Test(dataProvider = "rejectedLogins", dataProviderClass = TestData.class, description = "Login is rejected")
	public void loginIsRejected(String caseName, String user, String password, String expected) {
		LoginPage login = openLoginPage().submit(user, password);

		assertThat(login.error()).hasText(expected);
		assertThat(login.loginButton()).isVisible();
	}

	/**
	 * Logs in, logs out through the side menu and checks the login form is back.
	 */
	@Test(groups = "smoke", description = "User logs out and returns to the login page")
	public void userCanLogOut() {
		LoginPage login = loginAs(TestData.STANDARD_USER).logout();

		assertThat(login.loginButton()).isVisible();
		assertThat(page()).not().hasURL(Pattern.compile(".*/inventory\\.html"));
	}

	/**
	 * Opens the product list URL without logging in. The shop must refuse it
	 * and name the page in its error.
	 */
	@Test(description = "The product list requires a login")
	public void productListRequiresLogin() {
		new InventoryPage(page()).open();

		LoginPage login = new LoginPage(page());
		assertThat(login.error()).hasText("Epic sadface: You can only access '/inventory.html' when you are logged in.");
	}
}
