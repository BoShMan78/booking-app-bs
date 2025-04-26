package com.example.bookingappbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import com.example.bookingappbs.dto.address.AddressDto;
import com.example.bookingappbs.dto.address.CreateAddressRequestDto;
import com.example.bookingappbs.mapper.AccommodationMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Accommodation.Type;
import com.example.bookingappbs.model.Address;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.service.accommodation.AccommodationServiceImpl;
import com.example.bookingappbs.service.notification.NotificationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class AccommodationServiceTest {

    @InjectMocks
    private AccommodationServiceImpl accommodationService;

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private AccommodationMapper accommodationMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RedisService redisService;

    private Address address;
    private AddressDto addressDto;
    private CreateAddressRequestDto createAddressRequestDto;
    private Long accommodationId;
    private Accommodation accommodation;
    private AccommodationDto accommodationDto;

    @BeforeEach
    void setUp() {
        address = new Address()
                .setCountry("Ukraine")
                .setCity("Odesa")
                .setStreet("Deribasovskaya str.")
                .setHouse("1a")
                .setApartment(1);

        addressDto = new AddressDto(
                1L,
                address.getCountry(),
                address.getCity(),
                address.getStreet(),
                address.getHouse(),
                address.getApartment()
        );

        createAddressRequestDto = new CreateAddressRequestDto(
                address.getCountry(),
                address.getCity(),
                address.getStreet(),
                address.getHouse(),
                address.getApartment()
        );

        accommodationId = 1L;
        accommodation = new Accommodation()
                .setId(accommodationId)
                .setType(Type.APARTMENT)
                .setLocation(address)
                .setSize("Double")
                .setAmenities(List.of("Wifi", "Breakfast"))
                .setDailyRate(BigDecimal.valueOf(100.00))
                .setAvailability(10);

        accommodationDto = new AccommodationDto(
                accommodationId,
                accommodation.getType(),
                addressDto,
                accommodation.getSize(),
                accommodation.getAmenities(),
                accommodation.getDailyRate(),
                accommodation.getAvailability()
        );
    }

    @Test
    @DisplayName("Verify save() method works")
    public void save_ValidCreateAccommodationRequestDto_ReturnAccommodationDto() {
        // Given
        CreateAccommodationRequestDto requestDto = new CreateAccommodationRequestDto(
                Type.APARTMENT,
                createAddressRequestDto,
                "2 bedroom",
                List.of("TV"),
                BigDecimal.valueOf(50.0),
                1
        );
        Accommodation accommodationToSave = new Accommodation()
                .setType(Type.APARTMENT)
                .setLocation(address)
                .setSize(requestDto.size())
                .setAmenities(requestDto.amenities())
                .setDailyRate(requestDto.dailyRate());
        Accommodation savedAccommodation = new Accommodation()
                .setId(1L)
                .setType(accommodationToSave.getType())
                .setLocation(accommodationToSave.getLocation())
                .setSize(accommodationToSave.getSize())
                .setAmenities(accommodationToSave.getAmenities())
                .setDailyRate(accommodationToSave.getDailyRate());
        AccommodationDto expectedDto = new AccommodationDto(
                savedAccommodation.getId(),
                savedAccommodation.getType(),
                addressDto,
                savedAccommodation.getSize(),
                savedAccommodation.getAmenities(),
                savedAccommodation.getDailyRate(),
                savedAccommodation.getAvailability()
        );

        when(accommodationMapper.toModel(requestDto)).thenReturn(accommodationToSave);
        when(accommodationRepository.save(accommodationToSave)).thenReturn(savedAccommodation);
        when(accommodationMapper.toDto(savedAccommodation)).thenReturn(expectedDto);
        doNothing().when(redisService).deletePattern("accommodations::all::*");
        doNothing().when(notificationService).sendNotification(anyString());

        // When
        AccommodationDto actualDto = accommodationService.save(requestDto);

        // Then
        assertThat(actualDto).isEqualTo(expectedDto);
        verify(accommodationMapper, times(1)).toModel(requestDto);
        verify(accommodationRepository, times(1)).save(accommodationToSave);
        verify(accommodationMapper, times(1)).toDto(savedAccommodation);
        verify(redisService, times(1)).deletePattern("accommodations::all::*");
        verify(notificationService, times(1)).sendNotification(anyString());
        verifyNoMoreInteractions(accommodationRepository, accommodationMapper,
                redisService, notificationService);
    }

    @Test
    @DisplayName("Verify findAll() method works and returns cached data")
    public void findAll_ExistingCache_ReturnCachedAccommodations() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String key = "accommodations::all::page:0::size:10";
        List<AccommodationDto> cachedAccommodations = List.of(
                new AccommodationDto(
                        1L,
                        Type.APARTMENT,
                        null,
                        "2 bedroom",
                        List.of(),
                        BigDecimal.TEN,
                        1)
        );
        when(redisService.findAll(key, AccommodationDto.class)).thenReturn(cachedAccommodations);

        // When
        List<AccommodationDto> result = accommodationService.findAll(pageable);

        // Then
        assertThat(result).isEqualTo(cachedAccommodations);
        verify(redisService, times(1)).findAll(key, AccommodationDto.class);
        verifyNoMoreInteractions(accommodationRepository, accommodationMapper, redisService);
    }

    @Test
    void findAccommodationById_existingId_returnsAccommodationDto() {
        // When
        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(accommodation));
        when(accommodationMapper.toDto(accommodation)).thenReturn(accommodationDto);

        AccommodationDto result = accommodationService.findAccommodationById(accommodationId);

        // Then
        assertNotNull(result);
        assertEquals(accommodationDto, result);
        verify(accommodationRepository, times(1)).findById(accommodationId);
        verify(accommodationMapper, times(1)).toDto(accommodation);
    }

    @Test
    @DisplayName("Verify updateAccommodationById() method works")
    public void updateAccommodationById_ValidIdAndRequestDto_ReturnUpdatedAccommodationDto() {
        // Given
        Long accommodationId = 1L;
        UpdateAccommodationRequestDto requestDto = new UpdateAccommodationRequestDto(
                Type.HOUSE,
                address,
                "2 bedroom",
                null,
                BigDecimal.valueOf(80.0),
                1
        );
        Accommodation existingAccommodation = new Accommodation()
                .setId(accommodationId)
                .setType(Type.APARTMENT)
                .setLocation(address)
                .setSize("1 bedroom")
                .setDailyRate(BigDecimal.valueOf(60.0))
                .setAvailability(2);
        Accommodation savedAccommodation = new Accommodation()
                .setId(accommodationId)
                .setType(requestDto.type())
                .setLocation(requestDto.location())
                .setSize(requestDto.size())
                .setDailyRate(requestDto.dailyRate())
                .setAvailability(requestDto.availability());
        AccommodationDto expectedDto = new AccommodationDto(
                savedAccommodation.getId(),
                savedAccommodation.getType(),
                addressDto,
                savedAccommodation.getSize(),
                savedAccommodation.getAmenities(),
                savedAccommodation.getDailyRate(),
                savedAccommodation.getAvailability()
        );

        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(existingAccommodation));
        when(accommodationRepository.save(existingAccommodation))
                .thenReturn(savedAccommodation);
        when(accommodationMapper.toDto(savedAccommodation)).thenReturn(expectedDto);
        doNothing().when(redisService).deletePattern("accommodations::all::*");

        // When
        AccommodationDto actualDto = accommodationService
                .updateAccommodationById(accommodationId, requestDto);

        // Then
        assertThat(actualDto).isEqualTo(expectedDto);
        verify(accommodationRepository, times(1)).findById(accommodationId);
        verify(accommodationRepository, times(1)).save(existingAccommodation);
        verify(accommodationMapper, times(1))
                .updateAccommodationFromDto(requestDto, existingAccommodation);
        verify(accommodationMapper, times(1)).toDto(savedAccommodation);
        verify(redisService, times(1)).deletePattern("accommodations::all::*");
        verifyNoMoreInteractions(accommodationRepository, accommodationMapper, redisService);
        verify(accommodationMapper, times(1))
                .updateAccommodationFromDto(requestDto, existingAccommodation);
    }

    @Test
    @DisplayName("Verify deleteAccommodationById() method works")
    public void deleteAccommodationById_ValidId_DeletesAccommodationAndSendsNotification() {
        // Given
        Long accommodationId = 1L;
        Accommodation accommodationToDelete = new Accommodation()
                .setId(accommodationId)
                .setType(Type.VACATION_HOME)
                .setLocation(address)
                .setDailyRate(BigDecimal.valueOf(200.0));
        when(accommodationRepository.getAccommodationById(accommodationId))
                .thenReturn(accommodationToDelete);
        doNothing().when(accommodationRepository).deleteById(accommodationId);
        doNothing().when(redisService).deletePattern("accommodations::all::*");
        doNothing().when(notificationService).sendNotification(anyString());

        // When
        accommodationService.deleteAccommodationById(accommodationId);

        // Then
        verify(redisService, times(1)).deletePattern("accommodations::all::*");
        verify(accommodationRepository, times(1)).getAccommodationById(accommodationId);
        verify(accommodationRepository, times(1)).deleteById(accommodationId);
        verify(notificationService, times(1)).sendNotification(anyString());
        verifyNoMoreInteractions(accommodationRepository, redisService, notificationService);
    }
}
