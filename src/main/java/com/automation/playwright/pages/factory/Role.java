package com.automation.playwright.pages.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.microsoft.playwright.options.AriaRole;

/**
 * Fills a {@code Locator} field with {@code page.getByRole(value, name)}: an
 * element found the way a user or a screen reader sees it.
 * <p>
 * Example: {@code @Role(value = AriaRole.BUTTON, name = "Open Menu") private Locator menuButton;}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Role {

	/**
	 * @return AriaRole the element's role, e.g. BUTTON, TEXTBOX, LINK.
	 */
	AriaRole value();

	/**
	 * @return String the accessible name; empty matches any name.
	 */
	String name() default "";

	/**
	 * @return boolean true to match the name exactly (case and whole string).
	 */
	boolean exact() default false;
}
