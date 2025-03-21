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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public AccommodationDto save(CreateAccommodationRequestDto requestDto) {
        Accommodation accommodation = accommodationMapper.toModel(requestDto);

        Accommodation savedAccommodation = accommodationRepository.save(accommodation);
        redisService.deletePattern("accommodations::all::*");
        notificationService.sendNotification(
                "New accommodation created: \n"
                + "Accommodation id: " + savedAccommodation.getId() + "\n"
                + "Accommodation type: " + savedAccommodation.getType() + "\n"
                + "Daily rate: " + savedAccommodation.getDailyRate()
        );
        return accommodationMapper.toDto(savedAccommodation);
    }

    @Override
    public List<AccommodationDto> findAll(Pageable pageable) {
        String key = "accommodations::all::page:"
                + pageable.getPageNumber()
                + "::size:" + pageable.getPageSize();

        List<AccommodationDto> accommodationDtos = redisService.findAll(key, AccommodationDto.class);

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
        String key = "accommodation::" + id;
        AccommodationDto accommodationDto = (AccommodationDto) redisService.find(key, AccommodationDto.class);
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
        redisService.delete("accommodation::" + id);
        return accommodationMapper.toDto(savedAccommodation);
    }


    @Override
    public void deleteAccommodationById(Long id) {
        accommodationRepository.deleteById(id);
        redisService.delete("accommodation::" + id);
        notificationService.sendNotification("Accommodation deleted. Id: " + id);
    }
}
