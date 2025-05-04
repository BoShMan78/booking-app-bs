package com.example.bookingappbs.service.booking;

import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationBuilder {
    public String buildBookingNotificationMessage(
            String title,
            Booking booking,
            Accommodation accommodation
    ) {
        return title + ": \n"
                + "Booking ID: " + booking.getId() + "\n"
                + "Accommodation ID: " + accommodation.getId() + "\n"
                + "Type: " + accommodation.getType() + "\n"
                + "Location: " + buildAccommodationLocation(accommodation) + "\n"
                + "Check-in Date: " + booking.getCheckInDate() + "\n"
                + "Check-out Date: " + booking.getCheckOutDate();
    }

    private String buildAccommodationLocation(Accommodation accommodation) {
        if (accommodation != null && accommodation.getLocation() != null) {
            return accommodation.getLocation().getStreet() + " "
                    + accommodation.getLocation().getHouse() + ", "
                    + accommodation.getLocation().getCity() + ", "
                    + accommodation.getLocation().getCountry();
        }
        return "Location information not available";
    }
}
