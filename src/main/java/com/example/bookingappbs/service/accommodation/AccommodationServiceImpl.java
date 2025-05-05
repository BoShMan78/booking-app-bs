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
import java.util.concurrent.Future;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccommodationServiceImpl implements AccommodationService {
    private static final Logger logger = LogManager.getLogger(AccommodationServiceImpl.class);
    private static final String ACCOMMODATIONS_PAGE_KEY_PREFIX = "accommodations::all::";

    private final AccommodationRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final AccommodationNotificationBuilder notificationBuilder;

    @Override
    public AccommodationDto save(CreateAccommodationRequestDto requestDto) {
        logger.info("Processing request to save a new accommodation: {}", requestDto);
        Accommodation accommodation = accommodationMapper.toModel(requestDto);
        Accommodation savedAccommodation = accommodationRepository.save(accommodation);

        clearAccommodationCache();
        sendAccommodationNotification("New accommodation created", savedAccommodation);

        AccommodationDto dto = accommodationMapper.toDto(savedAccommodation);
        logger.info("Accommodation saved successfully with ID: {}", savedAccommodation.getId());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccommodationDto> findAll(Pageable pageable) {
        logger.info("Processing request to find all accommodations with pagination: {}", pageable);
        String key = ACCOMMODATIONS_PAGE_KEY_PREFIX + "page:" + pageable.getPageNumber()
                + "::size:" + pageable.getPageSize();

        Optional<List<AccommodationDto>> cachedDtos = Optional
                .ofNullable(findAllAccommodationsCache(key))
                .filter(list -> !list.isEmpty());

        return cachedDtos.orElseGet(() -> {
            logger.info("Accommodations not found in cache. Fetching from database.");
            List<AccommodationDto> dbDtos = accommodationRepository.findAll(pageable).stream()
                    .map(accommodationMapper::toDto)
                    .toList();
            saveToCacheDtos(key, dbDtos);
            logger.info("Accommodations fetched from database and saved to cache. Count: {}",
                    dbDtos.size());
            return dbDtos;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AccommodationDto findAccommodationById(Long id) {
        logger.info("Processing request to find accommodation by ID: {}", id);

        Accommodation accommodation = accommodationRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Accommodation with id " + id
                        + " not found"));
        AccommodationDto dto = accommodationMapper.toDto(accommodation);
        logger.info("Accommodation with ID {} fetched from database.", id);
        return dto;
    }

    @Override
    public AccommodationDto updateAccommodationById(
            Long id, UpdateAccommodationRequestDto requestDto
    ) {
        logger.info("Processing request to update accommodation with ID: {}. Update data: {}",
                id, requestDto);
        Accommodation existedAccommodation = accommodationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find accommodation with id " + id));

        accommodationMapper.updateAccommodationFromDto(requestDto, existedAccommodation);

        Accommodation savedAccommodation = accommodationRepository.save(existedAccommodation);
        AccommodationDto dto = accommodationMapper.toDto(savedAccommodation);

        clearAccommodationCache();

        logger.info("Accommodation with ID {} updated successfully.", id);
        return dto;
    }

    @Override
    public void deleteAccommodationById(Long id) {
        logger.info("Processing request to delete accommodation with ID: {}", id);
        clearAccommodationCache();

        Accommodation accommodation = accommodationRepository.getAccommodationById(id);
        accommodationRepository.deleteById(id);

        sendAccommodationNotification("Accommodation deleted", accommodation);
        logger.info("Accommodation with ID {} deleted successfully.", id);
    }

    private List<AccommodationDto> findAllAccommodationsCache(String key) {
        return redisService.findAll(key, AccommodationDto.class);
    }

    @Async
    public void clearAccommodationCache() {
        redisService.deletePattern(ACCOMMODATIONS_PAGE_KEY_PREFIX + "*");
    }

    @Async
    public void saveToCacheDtos(String key, List<AccommodationDto> dbDtos) {
        redisService.save(key, dbDtos);
    }

    private void sendAccommodationNotification(String title, Accommodation accommodation) {
        String message = notificationBuilder
                .buildAccommodationNotificationMessage(title, accommodation);
        notificationService.sendNotification(message);
    }
}
