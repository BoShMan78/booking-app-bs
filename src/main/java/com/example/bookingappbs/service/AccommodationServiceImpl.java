package com.example.bookingappbs.service;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.AccommodationMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.repository.AccommodationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;
    private final NotificationService notificationService;

    @Override
    public AccommodationDto save(CreateAccommodationRequestDto requestDto) {
        Accommodation accommodation = accommodationMapper.toModel(requestDto);

        Accommodation savedAccommodation = accommodationRepository.save(accommodation);
        notificationService.sendNotification("New accommodation created: "
                + savedAccommodation.getId());
        return accommodationMapper.toDto(savedAccommodation);
    }

    @Override
    public List<AccommodationDto> findAll(Pageable pageable) {
        return accommodationRepository.findAll(pageable).stream()
                .map(accommodationMapper::toDto)
                .toList();
    }

    @Override
    public AccommodationDto findAccommodationById(Long id) {
        Accommodation accommodation = accommodationRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Accommodation with id " + id + " not found"));
        return accommodationMapper.toDto(accommodation);
    }

    @Override
    public AccommodationDto updateAccommodationById(
            Long id, UpdateAccommodationRequestDto requestDto
    ) {
        Accommodation existedAccommodation = accommodationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find accommodation with id " + id));

        Optional.ofNullable(requestDto.type()).ifPresent(existedAccommodation::setType);
        Optional.ofNullable(requestDto.location()).ifPresent(existedAccommodation::setLocation);
        Optional.ofNullable(requestDto.size()).ifPresent(existedAccommodation::setSize);
        Optional.ofNullable(requestDto.amenities()).ifPresent(existedAccommodation::setAmenities);
        Optional.ofNullable(requestDto.dailyRate()).ifPresent(existedAccommodation::setDailyRate);
        if (requestDto.availability() != 0) {
            existedAccommodation.setAvailability(requestDto.availability());
        }

        Accommodation savedAccommodation = accommodationRepository.save(existedAccommodation);
        return accommodationMapper.toDto(savedAccommodation);
    }

    @Override
    public void deleteAccommodationById(Long id) {
        accommodationRepository.deleteById(id);
        notificationService.sendNotification("Accommodation deleted: " + id);
    }
}
