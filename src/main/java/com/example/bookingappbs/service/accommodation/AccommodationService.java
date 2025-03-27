package com.example.bookingappbs.service.accommodation;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AccommodationService {
    AccommodationDto save(CreateAccommodationRequestDto requestDto);

    List<AccommodationDto> findAll(Pageable pageable);

    AccommodationDto findAccommodationById(Long id);

    AccommodationDto updateAccommodationById(Long id, UpdateAccommodationRequestDto requestDto);

    void deleteAccommodationById(Long id);
}
