package com.automation.playwright.api.model;

/**
 * The response to creating a booking: its new id and the stored booking.
 *
 * @param bookingid the id to use in later requests.
 * @param booking   the booking as the server stored it.
 */
public record CreatedBooking(int bookingid, Booking booking) {
}
