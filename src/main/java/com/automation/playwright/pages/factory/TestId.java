package com.automation.playwright.pages.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fills a {@code Locator} field with {@code page.getByTestId(value)}.
 * Sauce Demo's test id attribute is {@code data-test} (set in BrowserManager).
 * <p>
 * Example: {@code @TestId("checkout") private Locator checkout;}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestId {

	/**
	 * @return String the test id, e.g. "login-button".
	 */
	String value();
}
