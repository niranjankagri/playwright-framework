package com.automation.playwright.tests.interview;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.automation.playwright.api.ApiResponse;
import com.automation.playwright.base.ApiTest;
import com.automation.playwright.base.RetryOnce;
import com.automation.playwright.config.Config;

/**
 * TestNG features that come up in interviews, shown on the Restful Booker
 * API (fast, no browser): soft assertions, expected exceptions, priority and
 * dependencies, repeated runs, time limits, retries and suite parameters.
 */
@Test(groups = "interview")
public class TestNgFeaturesTest extends ApiTest {

	/**
	 * SoftAssert collects every failed check and reports them together at
	 * assertAll(), instead of stopping at the first one.
	 */
	@Test(description = "SoftAssert checks several things before failing")
	public void softAssertChecksEverything() {
		ApiResponse ping = bookings.ping();
		ApiResponse auth = bookings.auth(Config.get().apiUser(), Config.get().apiPassword());

		SoftAssert soft = new SoftAssert();
		soft.assertEquals(ping.status(), 201, "Status of /ping");
		soft.assertEquals(auth.status(), 200, "Status of /auth");
		soft.assertFalse(auth.json().path("token").asText("").isEmpty(), "A token should be returned");
		soft.assertTrue(auth.millis() < 30_000, "/auth answered in " + auth.millis() + " ms");
		// Without assertAll() the test would pass even if a check above failed
		soft.assertAll();
	}

	/**
	 * expectedExceptions passes only if the test throws that exception, and
	 * expectedExceptionsMessageRegExp checks its message too.
	 */
	@Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "No token from /auth.*Bad credentials.*",
			description = "A wrong password throws the expected exception")
	public void wrongPasswordThrowsTheExpectedException() {
		bookings.token(Config.get().apiUser(), "wrong-password");
	}

	/**
	 * Runs first in this class (lowest priority number); the next test depends on it.
	 */
	@Test(priority = 1, description = "Priority 1: the API is up")
	public void apiIsUp() {
		assertEquals(bookings.ping().status(), 201, "Status of /ping");
	}

	/**
	 * dependsOnMethods: runs only after apiIsUp passed, and is skipped (not
	 * failed) if it did not, so one outage does not show as many failures.
	 */
	@Test(priority = 2, dependsOnMethods = "apiIsUp", description = "Depends on the API being up")
	public void adminGetsATokenOnceTheApiIsUp() {
		assertFalse(adminToken().isEmpty(), "Token");
	}

	/**
	 * invocationCount runs the same test several times, e.g. to catch flaky answers.
	 */
	@Test(invocationCount = 2, description = "invocationCount runs the health check twice")
	public void healthCheckAnswersEveryTime() {
		assertEquals(bookings.ping().status(), 201, "Status of /ping");
	}

	/**
	 * timeOut fails the test if it runs longer than the limit (here 30 s).
	 */
	@Test(timeOut = 30_000, description = "timeOut: a token within 30 seconds")
	public void tokenArrivesWithinThirtySeconds() {
		assertFalse(adminToken().isEmpty(), "Token");
	}

	/**
	 * retryAnalyzer runs the test once more if it fails, for answers from a
	 * public demo API that can be slow or briefly unavailable.
	 */
	@Test(retryAnalyzer = RetryOnce.class, description = "retryAnalyzer: an unknown booking is not found")
	public void unknownBookingIsNotFoundWithRetry() {
		assertEquals(bookings.get(999_999_999).status(), 404, "GET of an unknown id");
	}

	/**
	 * Reads the user name from testng.xml ({@code <parameter name="apiUser">});
	 * runs without a suite file (Maven) use the @Optional default.
	 *
	 * @param user the API user from the suite, else "admin".
	 */
	@Parameters("apiUser")
	@Test(description = "@Parameters: the suite's API user gets a token")
	public void suiteParameterUserGetsAToken(@Optional("admin") String user) {
		assertFalse(bookings.token(user, Config.get().apiPassword()).isEmpty(), "Token for " + user);
	}
}
