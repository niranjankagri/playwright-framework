package com.automation.playwright.base;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.automation.playwright.utils.Steps;

/**
 * Runs a failed test one more time. Use it with
 * {@code @Test(retryAnalyzer = RetryOnce.class)} on tests that talk to
 * public demo sites, which sometimes answer slowly or with a 5xx error.
 * TestNG reports the first attempt as skipped and the retry as the result.
 */
public class RetryOnce implements IRetryAnalyzer {

	// How often a failed test is run again
	private static final int MAX_RETRIES = 1;

	// Retries done so far; TestNG creates one analyzer per test method
	private int retries;

	/**
	 * @param result   the failed attempt.
	 * @return boolean true to run the test again.
	 */
	@Override
	public boolean retry(ITestResult result) {
		if (retries < MAX_RETRIES) {
			retries++;
			Steps.log("Failed, retrying (" + retries + " of " + MAX_RETRIES + "): " + result.getThrowable());
			return true;
		}
		return false;
	}
}
