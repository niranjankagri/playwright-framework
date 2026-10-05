package com.automation.playwright.utils;

import org.testng.Reporter;

/**
 * Records the steps of the running test. Each step is attached to the
 * current TestNG result and shown, in order, in the HTML report.
 */
public final class Steps {

	// Static helper only
	private Steps() {
	}

	/**
	 * @param step a short description of what the test is doing, e.g. "Log in as standard_user".
	 */
	public static void log(String step) {
		Reporter.log(step);
	}
}
