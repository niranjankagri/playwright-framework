package com.automation.playwright.base;

import org.testng.annotations.AfterSuite;

import com.automation.playwright.browser.BrowserManager;

/**
 * Parent of all test classes. Closes every Playwright instance (and with
 * them the browsers) once the whole suite has finished.
 */
public abstract class PlaywrightTest {

	/**
	 * Runs once after all tests, whichever classes ran.
	 */
	@AfterSuite(alwaysRun = true)
	public void closePlaywright() {
		BrowserManager.closeAll();
	}
}
