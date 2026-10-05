package com.automation.playwright.tests.api;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;

import com.automation.playwright.api.ApiResponse;
import com.automation.playwright.api.model.Booking;
import com.automation.playwright.api.model.CreatedBooking;
import com.automation.playwright.base.ApiTest;
import com.automation.playwright.data.TestData;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Restful Booker bookings: create, read, search, update, patch and delete,
 * plus the error cases. Every test creates its own booking through the API
 * (test data setup without a UI), and ApiTest deletes it afterwards, so the
 * tests are independent and can run in any order.
 */
@Test(groups = "api")
public class BookingApiTest extends ApiTest {

	/**
	 * Creates a booking: the response echoes it with a new id, and reading it
	 * back returns exactly the booking that was sent (records compare by value).
	 */
	@Test(groups = "smoke", description = "A created booking can be read back")
	public void createdBookingCanBeReadBack() {
		Booking booking = TestData.newBooking();

		ApiResponse created = bookings.create(booking);
		assertEquals(created.status(), 200, "Status of POST /booking");
		CreatedBooking body = created.as(CreatedBooking.class);
		registerForCleanup(body.bookingid());
		assertTrue(body.bookingid() > 0, "New booking id");
		assertEquals(body.booking(), booking, "Booking in the create response");

		ApiResponse read = bookings.get(body.bookingid());
		assertEquals(read.status(), 200, "Status of GET /booking/{id}");
		assertEquals(read.as(Booking.class), booking, "Booking read back");
	}

	/**
	 * Searches by first and last name: the unique last name finds the booking.
	 */
	@Test(description = "A booking is found by name")
	public void bookingIsFoundByName() {
		Booking booking = TestData.newBooking();
		int id = givenBooking(booking);

		ApiResponse response = bookings.search(booking.firstname(), booking.lastname());
		assertEquals(response.status(), 200, "Status of GET /booking?firstname&lastname");
		boolean found = false;
		for (JsonNode entry : response.json()) {
			found |= entry.path("bookingid").asInt() == id;
		}
		assertTrue(found, "Booking " + id + " should be in " + response.body());
	}

	/**
	 * PUT replaces the whole booking; reading it back shows the new values.
	 */
	@Test(description = "PUT replaces a booking")
	public void putReplacesABooking() {
		Booking booking = TestData.newBooking();
		int id = givenBooking(booking);
		Booking changed = booking.withFirstname("Updated").withTotalprice(booking.totalprice() + 50);

		ApiResponse response = bookings.update(id, changed, adminToken());
		assertEquals(response.status(), 200, "Status of PUT");
		assertEquals(response.as(Booking.class), changed, "Booking in the PUT response");
		assertEquals(bookings.get(id).as(Booking.class), changed, "Booking read back");
	}

	/**
	 * PATCH changes only the given field; every other field stays the same.
	 */
	@Test(description = "PATCH changes only the given fields")
	public void patchChangesOnlyTheGivenFields() {
		Booking booking = TestData.newBooking();
		int id = givenBooking(booking);

		ApiResponse response = bookings.patch(id, Map.of("firstname", "Patched"), adminToken());
		assertEquals(response.status(), 200, "Status of PATCH");
		assertEquals(response.as(Booking.class), booking.withFirstname("Patched"), "Only the first name should change");
	}

	/**
	 * Changes without a token are refused with 403, and the booking stays as it was.
	 */
	@Test(description = "Changes need a token")
	public void changesNeedAToken() {
		Booking booking = TestData.newBooking();
		int id = givenBooking(booking);

		assertEquals(bookings.patch(id, Map.of("firstname", "Hacker"), null).status(), 403, "PATCH without a token");
		assertEquals(bookings.delete(id, null).status(), 403, "DELETE without a token");
		assertEquals(bookings.get(id).as(Booking.class), booking, "The booking should be unchanged");
	}

	/**
	 * Deletes a booking: the API answers 201 (its quirk), and the booking is
	 * then gone (404).
	 */
	@Test(description = "A deleted booking is gone")
	public void deletedBookingIsGone() {
		int id = givenBooking(TestData.newBooking());

		ApiResponse response = bookings.delete(id, adminToken());
		forget(id);
		assertEquals(response.status(), 201, "Status of DELETE (the API answers 201 Created)");
		assertEquals(bookings.get(id).status(), 404, "GET after DELETE");
	}

	/**
	 * A booking id that does not exist returns 404.
	 */
	@Test(description = "An unknown booking is not found")
	public void unknownBookingIsNotFound() {
		assertEquals(bookings.get(Integer.MAX_VALUE).status(), 404, "GET of an unknown id");
	}
}
