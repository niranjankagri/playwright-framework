package com.automation.playwright.api.model;

/**
 * A booking of the Restful Booker API. The field names match the JSON, so
 * Jackson maps the record without annotations.
 *
 * @param firstname       the guest's first name.
 * @param lastname        the guest's last name.
 * @param totalprice      the price in whole units.
 * @param depositpaid     whether a deposit was paid.
 * @param bookingdates    check-in and check-out dates.
 * @param additionalneeds free text, e.g. "Breakfast".
 */
public record Booking(String firstname, String lastname, int totalprice, boolean depositpaid,
		BookingDates bookingdates, String additionalneeds) {

	/**
	 * @param first    the new first name.
	 * @return Booking a copy with that first name.
	 */
	public Booking withFirstname(String first) {
		return new Booking(first, lastname, totalprice, depositpaid, bookingdates, additionalneeds);
	}

	/**
	 * @param price    the new price.
	 * @return Booking a copy with that price.
	 */
	public Booking withTotalprice(int price) {
		return new Booking(firstname, lastname, price, depositpaid, bookingdates, additionalneeds);
	}
}
