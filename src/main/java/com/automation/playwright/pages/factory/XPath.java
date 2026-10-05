package com.automation.playwright.pages.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fills a {@code Locator} field with {@code page.locator("xpath=" + value)}.
 * <p>
 * Example: {@code @XPath("//input[@name='user-name']") private Locator username;}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface XPath {

	/**
	 * @return String the XPath expression.
	 */
	String value();
}
