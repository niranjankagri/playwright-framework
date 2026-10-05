package com.automation.playwright.data;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import org.testng.annotations.DataProvider;

import com.automation.playwright.api.model.Booking;
import com.automation.playwright.api.model.BookingDates;

/**
 * Test data: Sauce Demo users and products, the data providers shared by the
 * tests, and generated bookings for the API.
 */
public final class TestData {

	// Sauce Demo users (all share the password in config.properties)
	public static final String STANDARD_USER = "standard_user";
	public static final String LOCKED_OUT_USER = "locked_out_user";
	// Sees the wrong picture on every product (a deliberately broken user of the demo shop)
	public static final String PROBLEM_USER = "problem_user";
	// Products used in cart and checkout tests
	public static final String BACKPACK = "Sauce Labs Backpack";
	public static final String BIKE_LIGHT = "Sauce Labs Bike Light";
	public static final String BOLT_SHIRT = "Sauce Labs Bolt T-Shirt";
	// The message for a user name and password that do not match
	private static final String NO_MATCH = "Epic sadface: Username and password do not match any user in this service";

	// Static helper only
	private TestData() {
	}

	/**
	 * Logins the shop must reject, with the banner it must show.
	 *
	 * @return Object[][] rows of { case name, username, password, expected error }.
	 */
	@DataProvider(name = "rejectedLogins")
	public static Object[][] rejectedLogins() {
		return new Object[][] {
				{ "wrong password", STANDARD_USER, "wrong_password", NO_MATCH },
				{ "unknown user", "no_such_user", "secret_sauce", NO_MATCH },
				{ "missing username", "", "secret_sauce", "Epic sadface: Username is required" },
				{ "missing password", STANDARD_USER, "", "Epic sadface: Password is required" },
				{ "locked-out user", LOCKED_OUT_USER, "secret_sauce", "Epic sadface: Sorry, this user has been locked out." },
		};
	}

	/**
	 * The sort options of the product list.
	 *
	 * @return Object[][] rows of { label, option value }.
	 */
	@DataProvider(name = "sortOptions")
	public static Object[][] sortOptions() {
		return new Object[][] {
				{ "name A to Z", "az" },
				{ "name Z to A", "za" },
				{ "price low to high", "lohi" },
				{ "price high to low", "hilo" },
		};
	}

	/**
	 * Checkout forms with a missing field, and the error each must show.
	 *
	 * @return Object[][] rows of { case name, first name, last name, postal code, expected error }.
	 */
	@DataProvider(name = "incompleteCheckouts")
	public static Object[][] incompleteCheckouts() {
		return new Object[][] {
				{ "missing first name", "", "Wright", "10115", "Error: First Name is required" },
				{ "missing last name", "Play", "", "10115", "Error: Last Name is required" },
				{ "missing postal code", "Play", "Wright", "", "Error: Postal Code is required" },
		};
	}

	/**
	 * A new booking with a unique last name (so a search by name finds only
	 * this one) and dates in the future.
	 *
	 * @return Booking a booking that does not exist yet.
	 */
	public static Booking newBooking() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		LocalDate checkin = LocalDate.now().plusDays(30 + random.nextInt(60));
		return new Booking("Playwright", "Test" + String.format("%08d", random.nextInt(100_000_000)),
				100 + random.nextInt(900), true,
				new BookingDates(checkin.toString(), checkin.plusDays(3).toString()), "Breakfast");
	}
}
