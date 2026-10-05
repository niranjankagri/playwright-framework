package com.automation.playwright.pages.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fills a {@code Locator} field with {@code page.locator(value)} for a CSS selector.
 * <p>
 * Example: {@code @Css("form") private Locator form;}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Css {

	/**
	 * @return String the CSS selector, e.g. "#user-name".
	 */
	String value();
}
