package com.automation.playwright.tests.demo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.automation.playwright.base.UiTest;
import com.automation.playwright.data.TestData;
import com.automation.playwright.pages.InventoryPage;

/**
 * Tests that fail or skip <b>on purpose</b>, so the HTML report shows what a
 * failure looks like: the error, a screenshot, a video and a Playwright trace.
 * <p>
 * They are in the {@code demo} group, which a normal run leaves out (see
 * {@code excludedGroups} in pom.xml and testng.xml). Include them with
 * {@code mvn clean test -Pdemo}; that build then ends as failed.
 */
@Test(groups = "demo")
public class ReportDemoTest extends UiTest {

	/**
	 * Adds one product and expects the badge to show 2. Playwright's
	 * assertion retries until its timeout, then fails with the expected and
	 * actual text; the report gets the screenshot, video and trace.
	 */
	@Test(description = "Demo failure: the cart badge is checked against a wrong count")
	public void cartBadgeCheckFails() {
		InventoryPage inventory = loginAs(TestData.STANDARD_USER).addToCart(TestData.BACKPACK);

		assertThat(inventory.cartBadge()).hasText("2");
	}

	/**
	 * Logs in with the locked-out user through the login helper, which expects
	 * success. The helper fails at once with the shop's error text (an
	 * IllegalStateException), not with a timeout.
	 */
	@Test(description = "Demo failure: the login helper is used with a locked-out user")
	public void loginHelperFailsForLockedOutUser() {
		loginAs(TestData.LOCKED_OUT_USER);
	}

	/**
	 * Skips itself because a precondition is missing; the report shows the reason.
	 */
	@Test(description = "Demo skip: the check needs a payment sandbox the demo shop does not have")
	public void skippedWhenPreconditionIsMissing() {
		openLoginPage();
		throw new SkipException("Sauce Demo has no payment provider sandbox, so payment errors cannot be tested");
	}

	/**
	 * Depends on a test that fails, so TestNG skips it without running it.
	 */
	@Test(dependsOnMethods = "cartBadgeCheckFails", description = "Demo skip: depends on a failed test")
	public void skippedBecauseDependencyFailed() {
		loginAs(TestData.STANDARD_USER).openCart();
	}
}
