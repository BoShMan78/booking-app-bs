package com.example.bookingappbs.service.accommodation;

import com.example.bookingappbs.model.Accommodation;
import org.springframework.stereotype.Component;

@Component
public class AccommodationNotificationBuilder {
    public String buildAccommodationNotificationMessage(
            String title,
            Accommodation accommodation
    ) {
        String location = buildAccommodationLocation(accommodation);
        return String.format(
                "%s:\nAccommodation ID: %d\nType: %s\nLocation: %s\nDaily rate: %.2f",
                title,
                accommodation.getId(),
                accommodation.getType(),
                location,
                accommodation.getDailyRate()
        );
    }

    private String buildAccommodationLocation(Accommodation accommodation) {
        if (accommodation != null && accommodation.getLocation() != null) {
            return String.format("%s %s, %s, %s",
                    accommodation.getLocation().getStreet(),
                    accommodation.getLocation().getHouse(),
                    accommodation.getLocation().getCity(),
                    accommodation.getLocation().getCountry());
        }
        return "Location information not available";
    }
}
