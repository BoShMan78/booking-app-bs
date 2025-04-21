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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private static final Logger logger = LogManager.getLogger(AccommodationServiceImpl.class);
    private static final String ACCOMMODATION_KEY_PREFIX = "accommodation::";
    private static final String ACCOMMODATIONS_PAGE_KEY_PREFIX = "accommodations::all::";

    private final AccommodationRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;
    private final NotificationService notificationService;
    private final RedisService redisService;

    @Override
    @Transactional
    public AccommodationDto save(CreateAccommodationRequestDto requestDto) {
        logger.info("Processing request to save a new accommodation: {}", requestDto);
        Accommodation accommodation = accommodationMapper.toModel(requestDto);
        Accommodation savedAccommodation = accommodationRepository.save(accommodation);
        AccommodationDto dto = accommodationMapper.toDto(savedAccommodation);

        clearAccommodationCache();
        saveToCache(savedAccommodation.getId(), dto);
        sendAccommodationNotification("New accommodation created", savedAccommodation);

        logger.info("Accommodation saved successfully with ID: {}", savedAccommodation.getId());
        return dto;
    }

    @Override
    public List<AccommodationDto> findAll(Pageable pageable) {
        logger.info("Processing request to find all accommodations with pagination: {}", pageable);
        String key = ACCOMMODATIONS_PAGE_KEY_PREFIX + "page:" + pageable.getPageNumber()
                + "::size:" + pageable.getPageSize();

        List<AccommodationDto> accommodationDtos = redisService
                .findAll(key, AccommodationDto.class);

        if (accommodationDtos == null || accommodationDtos.isEmpty()) {
            logger.info("Accommodations not found in cache. Fetching from database.");
            accommodationDtos = accommodationRepository.findAll(pageable).stream()
                    .map(accommodationMapper::toDto)
                    .toList();

            redisService.save(key, accommodationDtos);
            logger.info("Accommodations fetched from database and saved to cache. Count: {}",
                    accommodationDtos.size());
        } else {
            logger.info("Accommodations found in cache. Count: {}", accommodationDtos.size());
        }
        return accommodationDtos;
    }

    @Override
    public AccommodationDto findAccommodationById(Long id) {
        logger.info("Processing request to find accommodation by ID: {}", id);
        String key = ACCOMMODATION_KEY_PREFIX + id;
        AccommodationDto accommodationDto = redisService
                .find(key, AccommodationDto.class);

        if (accommodationDto == null) {
            logger.info("Accommodation with ID {} not found in cache. Fetching from database.",
                    id);
            Accommodation accommodation = accommodationRepository.findById(id).orElseThrow(
                    () -> new EntityNotFoundException("Accommodation with id " + id + " not found")
            );
            accommodationDto = accommodationMapper.toDto(accommodation);

            redisService.save(key, accommodationDto);
            logger.info("Accommodation with ID {} fetched from database and saved to cache.", id);
        } else {
            logger.info("Accommodation with ID {} found in cache.", id);
        }
        return accommodationDto;
    }

    @Override
    @Transactional
    public AccommodationDto updateAccommodationById(
            Long id, UpdateAccommodationRequestDto requestDto
    ) {
        logger.info("Processing request to update accommodation with ID: {}. Update data: {}",
                id, requestDto);
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

        logger.info("Accommodation with ID {} updated successfully.", id);
        return dto;
    }

    @Override
    @Transactional
    public void deleteAccommodationById(Long id) {
        logger.info("Processing request to delete accommodation with ID: {}", id);
        clearAccommodationCache();
        redisService.delete(ACCOMMODATION_KEY_PREFIX + id);

        Accommodation accommodation = accommodationRepository.getAccommodationById(id);
        accommodationRepository.deleteById(id);

        sendAccommodationNotification("Accommodation deleted", accommodation);
        logger.info("Accommodation with ID {} deleted successfully.", id);
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
