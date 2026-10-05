package com.automation.playwright.base;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.automation.playwright.api.ApiClient;
import com.automation.playwright.api.BookingApi;
import com.automation.playwright.api.model.Booking;
import com.automation.playwright.browser.BrowserManager;
import com.automation.playwright.config.Config;
import com.automation.playwright.utils.Steps;

/**
 * Parent of the API test classes. Every test gets its own HTTP client
 * (Playwright's request context, no browser needed). Bookings created with
 * {@link #givenBooking(Booking)} are deleted again after the test, so the
 * shared demo API is left as it was.
 */
public abstract class ApiTest extends PlaywrightTest {

	// The endpoints of the running test
	protected BookingApi bookings;

	// The HTTP client of the running test
	private ApiClient client;
	// Bookings this test created and has not deleted yet
	private final List<Integer> created = new ArrayList<>();

	/**
	 * Opens a request context for the test.
	 */
	@BeforeMethod(alwaysRun = true)
	public void openClient() {
		client = new ApiClient(BrowserManager.playwright(), Config.get().apiUrl());
		bookings = new BookingApi(client);
	}

	/**
	 * Deletes the bookings the test created, then closes the request context.
	 */
	@AfterMethod(alwaysRun = true)
	public void closeClient() {
		try {
			if (!created.isEmpty()) {
				String token = adminToken();
				for (int id : created) {
					Steps.log("Cleanup: delete booking " + id);
					bookings.delete(id, token);
				}
			}
		} catch (RuntimeException e) {
			System.err.println("Cleanup of bookings " + created + " failed: " + e.getMessage());
		} finally {
			created.clear();
			client.close();
		}
	}

	/**
	 * Creates a booking through the API as test data and registers it for cleanup.
	 *
	 * @param booking the booking to create.
	 * @return int    its id.
	 */
	protected int givenBooking(Booking booking) {
		int id = bookings.createBooking(booking);
		registerForCleanup(id);
		return id;
	}

	/**
	 * Registers a booking the test created itself, so it is deleted after the test.
	 *
	 * @param id the booking id.
	 */
	protected void registerForCleanup(int id) {
		created.add(id);
	}

	/**
	 * Tells the cleanup that a booking is already gone (the test deleted it).
	 *
	 * @param id the deleted booking.
	 */
	protected void forget(int id) {
		created.remove(Integer.valueOf(id));
	}

	/**
	 * @return String an auth token for the admin from config.properties.
	 */
	protected String adminToken() {
		return bookings.token(Config.get().apiUser(), Config.get().apiPassword());
	}
}
