package com.example.bookingappbs.service.accommodation;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.AccommodationMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.service.RedisService;
import com.example.bookingappbs.service.notification.NotificationService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private static final String ACCOMMODATION_KEY_PREFIX = "accommodation::";
    private static final String ACCOMMODATIONS_PAGE_KEY_PREFIX = "accommodations::all::";

    private final AccommodationRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public AccommodationDto save(CreateAccommodationRequestDto requestDto) {
        Accommodation accommodation = accommodationMapper.toModel(requestDto);
        Accommodation savedAccommodation = accommodationRepository.save(accommodation);
        AccommodationDto dto = accommodationMapper.toDto(savedAccommodation);

        clearAccommodationCache();
        saveToCache(savedAccommodation.getId(), dto);
        sendAccommodationNotification("New accommodation created", savedAccommodation);

        return dto;
    }

    @Override
    public List<AccommodationDto> findAll(Pageable pageable) {
        String key = ACCOMMODATIONS_PAGE_KEY_PREFIX + "page:" + pageable.getPageNumber()
                + "::size:" + pageable.getPageSize();

        List<AccommodationDto> accommodationDtos = redisService
                .findAll(key, AccommodationDto.class);

        if (accommodationDtos == null || accommodationDtos.isEmpty()) {
            accommodationDtos = accommodationRepository.findAll(pageable).stream()
                    .map(accommodationMapper::toDto)
                    .toList();

            redisService.save(key, accommodationDtos);
        }
        return accommodationDtos;
    }

    @Override
    public AccommodationDto findAccommodationById(Long id) {
        String key = ACCOMMODATION_KEY_PREFIX + id;
        AccommodationDto accommodationDto = redisService
                .find(key, AccommodationDto.class);

        if (accommodationDto == null) {
            Accommodation accommodation = accommodationRepository.findById(id).orElseThrow(
                    () -> new EntityNotFoundException("Accommodation with id " + id + " not found")
            );
            accommodationDto = accommodationMapper.toDto(accommodation);

            redisService.save(key, accommodationDto);
        }
        return accommodationDto;
    }

    @Override
    @Transactional
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
        if (requestDto.availability() != null && requestDto.availability() != 0) {
            existedAccommodation.setAvailability(requestDto.availability());
        }

        Accommodation savedAccommodation = accommodationRepository.save(existedAccommodation);
        AccommodationDto dto = accommodationMapper.toDto(savedAccommodation);

        clearAccommodationCache();
        saveToCache(id, dto);

        return dto;
    }

    @Override
    @Transactional
    public void deleteAccommodationById(Long id) {
        clearAccommodationCache();
        redisService.delete(ACCOMMODATION_KEY_PREFIX + id);

        Accommodation accommodation = accommodationRepository.getAccommodationById(id);
        accommodationRepository.deleteById(id);

        sendAccommodationNotification("Accommodation deleted", accommodation);
    }

    private void clearAccommodationCache() {
        redisService.deletePattern(ACCOMMODATIONS_PAGE_KEY_PREFIX + "*");
    }

    private void saveToCache(Long id, AccommodationDto dto) {
        redisService.save(ACCOMMODATION_KEY_PREFIX + id, dto);
    }

    private void sendAccommodationNotification(String title, Accommodation accommodation) {
        String location = String.format("%s %s, %s, %s",
                accommodation.getLocation().getStreet(),
                accommodation.getLocation().getHouse(),
                accommodation.getLocation().getCity(),
                accommodation.getLocation().getCountry());

        String message = String.format(
                "%s:\nAccommodation ID: %d\nType: %s\nLocation: %s\nDaily rate: %.2f",
                title,
                accommodation.getId(),
                accommodation.getType(),
                location,
                accommodation.getDailyRate()
        );

        notificationService.sendNotification(message);
    }
}
