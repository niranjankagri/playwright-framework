package com.automation.playwright.data;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import org.testng.annotations.DataProvider;

import com.automation.playwright.api.model.Booking;
import com.automation.playwright.api.model.BookingDates;

/**
 * Test data: Pattern Shop users and products, the data providers shared by the
 * tests, and generated bookings for the API.
 */
public final class TestData {

	// Pattern Shop users (all share the password in config.properties)
	public static final String STANDARD_USER = "standard_user";
	public static final String LOCKED_OUT_USER = "locked_user";
	// Sees the same placeholder picture on every product (a deliberately broken user of the demo shop)
	public static final String PROBLEM_USER = "problem_user";
	// The shop has four products
	public static final int PRODUCT_COUNT = 4;
	// Products used in cart and checkout tests
	public static final String BACKPACK = "Backpack";
	public static final String BIKE_LIGHT = "Bike Light";
	public static final String BOLT_SHIRT = "Bolt T-Shirt";
	// The messages for a user name and password that do not match, and for an empty field
	private static final String NO_MATCH = "User name and password do not match any user.";
	private static final String REQUIRED = "User name and password are required.";

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
				{ "missing username", "", "secret_sauce", REQUIRED },
				{ "missing password", STANDARD_USER, "", REQUIRED },
				{ "locked-out user", LOCKED_OUT_USER, "secret_sauce", "This user has been locked out." },
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
				{ "name A to Z", "name-asc" },
				{ "name Z to A", "name-desc" },
				{ "price low to high", "price-asc" },
				{ "price high to low", "price-desc" },
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
				{ "missing first name", "", "Wright", "10115", "First name is required." },
				{ "missing last name", "Play", "", "10115", "Last name is required." },
				{ "missing postal code", "Play", "Wright", "", "Postal code is required." },
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
