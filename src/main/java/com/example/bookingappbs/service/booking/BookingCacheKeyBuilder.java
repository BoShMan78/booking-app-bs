package com.example.bookingappbs.service.booking;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class BookingCacheKeyBuilder {
    private static final String BOOKINGS_PAGE_KEY_PREFIX = "bookings";

    public String buildBookingsPageKey(Long userId, String status, Pageable pageable) {
        StringBuilder cacheKeyBuilder = new StringBuilder(BOOKINGS_PAGE_KEY_PREFIX);
        if (userId != null) {
            cacheKeyBuilder.append("::user::").append(userId);
        }
        if (status != null) {
            cacheKeyBuilder.append("::status::").append(status);
        }
        cacheKeyBuilder.append("::page::").append(pageable.getPageNumber())
                .append("::size::").append(pageable.getPageSize())
                .append("::sort::").append(pageable.getSort());
        return cacheKeyBuilder.toString();
    }
}
