package com.example.bookingappbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.mapper.AccommodationMapper;
import com.example.bookingappbs.mapper.BookingMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Accommodation.Type;
import com.example.bookingappbs.model.Address;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.repository.UserRepository;
import com.example.bookingappbs.service.booking.BookingServiceImpl;
import com.example.bookingappbs.service.notification.NotificationService;
import com.example.bookingappbs.service.payment.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
    @InjectMocks
    private BookingServiceImpl bookingService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private AccommodationRepository accommodationRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RedisService redisService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccommodationMapper accommodationMapper;

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Verify save() method works")
    public void save_ValidCreateBookingRequestDto_ReturnBookingDto() {
        //Given
        Address address = new Address();
        address.setCountry("Ukraine");
        address.setCity("Odesa");
        address.setStreet("Deribasovskaya str.");
        address.setHouse("1a");
        address.setApartment(1);

        Long accommodationId = 1L;
        Accommodation accommodation = new Accommodation().setId(accommodationId)
                .setType(Type.APARTMENT)
                .setLocation(address)
                .setDailyRate(BigDecimal.valueOf(75.50));

        CreateBookingRequestDto requestDto = new CreateBookingRequestDto(
                LocalDate.of(2027, 01, 15),
                LocalDate.of(2027, 01, 18),
                accommodationId
        );

        User user = new User();
        user.setId(1L);

        Booking booking = new Booking().setCheckInDate(requestDto.checkInDate())
                .setCheckOutDate(requestDto.checkOutDate())
                .setAccommodation(accommodation)
                .setUser(user);

        BookingDto bookingDto = new BookingDto(
                null,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                accommodationId,
                user.getId(),
                Status.PENDING.toString()
        );

        //When
        when(paymentService.countPendingPaymentsForUser(user.getId())).thenReturn(0L);
        when(bookingMapper.toModel(requestDto)).thenReturn(booking);
        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(accommodation));
        when(bookingRepository
                .existsByAccommodationAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                        eq(accommodation),
                        eq(booking.getCheckOutDate()),
                        eq(booking.getCheckInDate())
                )).thenReturn(false);

        Mockito.when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        BookingDto savedBookingDto = bookingService.save(user, requestDto);

        //Then
        assertThat(savedBookingDto).isEqualTo(bookingDto);
        verify(paymentService, times(1)).countPendingPaymentsForUser(user.getId());
        verify(bookingMapper, times(1)).toModel(requestDto);
        verify(accommodationRepository, times(1)).findById(accommodationId);
        verify(bookingRepository, times(1))
                .existsByAccommodationAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                        eq(accommodation),
                        eq(booking.getCheckOutDate()),
                        eq(booking.getCheckInDate())
                );
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(bookingMapper, times(1)).toDto(booking);
        verify(redisService, times(1)).deletePattern("bookings::all::*");
        verify(redisService, times(1)).deletePattern("bookings::user::*");
        verify(redisService, times(1))
                .save(eq("booking::" + booking.getId()), eq(bookingDto));
        verify(notificationService, times(1)).sendNotification(anyString());

        verifyNoMoreInteractions(bookingRepository, bookingMapper, paymentService, redisService,
                accommodationRepository, notificationService);
    }

    @Test
    @DisplayName("Verify getBookingsByUserAndStatus() method works and fetches from DB and caches")
    public void getBookingsByUserAndStatus_NoCache_FetchFromDbAndCache() {
        // Given
        Long userId = 1L;
        Status status = Status.PENDING;
        Pageable pageable = PageRequest.of(0, 10);
        String key = "bookings::user::" + userId + "::status::" + status
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();
        when(redisService.findAll(key, BookingDto.class)).thenReturn(null);
        Page<Booking> bookingPage = new PageImpl<>(List.of(new Booking().setId(1L)));
        when(bookingRepository.findByUserIdAndStatus(userId, status, pageable))
                .thenReturn(bookingPage);
        List<BookingDto> bookingDtos = List.of(new BookingDto(
                userId,
                null,
                null,
                null,
                null,
                null
        ));
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(bookingDtos.get(0));

        // When
        List<BookingDto> result = bookingService
                .getBookingsByUserAndStatus(userId, status, pageable);

        // Then
        assertThat(result).isEqualTo(bookingDtos);
        verify(redisService, times(1)).findAll(key, BookingDto.class);
        verify(bookingRepository, times(1)).findByUserIdAndStatus(userId, status, pageable);
        verify(bookingMapper, times(1)).toDto(any(Booking.class));
        verify(redisService, times(1)).save(key, bookingDtos);
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService);
    }

    @Test
    @DisplayName("Verify getBookingsByUser() method works and returns cached data")
    public void getBookingsByUser_ExistingCache_ReturnCachedBookings() {
        // Given
        User user = new User().setId(1L);
        Pageable pageable = PageRequest.of(0, 10);
        String key = "bookings::user::" + user.getId()
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();
        List<BookingDto> cachedBookings = List.of(new BookingDto(
                null,
                null,
                null,
                null,
                null,
                null
        ));
        when(redisService.findAll(key, BookingDto.class)).thenReturn(cachedBookings);

        // When
        List<BookingDto> result = bookingService.getBookingsByUser(user, pageable);

        // Then
        assertThat(result).isEqualTo(cachedBookings);
        verify(redisService, times(1)).findAll(key, BookingDto.class);
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService);
    }

    @Test
    @DisplayName("Verify getBookingById() method works and fetches from DB and caches")
    public void getBookingById_NoCache_FetchFromDbAndCache() {
        // Given
        User user = new User().setId(1L);
        Long bookingId = 1L;
        String key = "booking::" + bookingId;
        when(redisService.find(key, BookingDto.class)).thenReturn(null);
        Booking booking = new Booking().setId(bookingId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        BookingDto bookingDto = new BookingDto(
                bookingId,
                null,
                null,
                null,
                null,
                null
        );
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // When
        BookingDto result = bookingService.getBookingById(user, bookingId);

        // Then
        assertThat(result).isEqualTo(bookingDto);
        verify(redisService, times(1)).find(key, BookingDto.class);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingMapper, times(1)).toDto(booking);
        verify(redisService, times(1)).save(key, bookingDto);
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService);
    }

    @Test
    @DisplayName("Verify updateBookingById() method works for admin updating status")
    public void updateBookingById_AdminUpdateStatus_Success() {
        // Given
        User admin = new User().setId(1L).setRole(Role.ADMIN);
        Long bookingId = 1L;
        UpdateBookingRequestDto requestDto = new UpdateBookingRequestDto(
                null,
                null,
                null,
                Status.CONFIRMED.toString()
        );
        Booking existingBooking = new Booking().setId(bookingId).setStatus(Status.PENDING);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existingBooking));
        Booking updatedBooking = new Booking().setId(bookingId).setStatus(Status.CONFIRMED);
        when(bookingRepository.save(any(Booking.class))).thenReturn(updatedBooking);
        BookingDto updatedBookingDto = new BookingDto(
                bookingId,
                null,
                null,
                null,
                null,
                Status.CONFIRMED.toString()
        );
        when(bookingMapper.toDto(updatedBooking)).thenReturn(updatedBookingDto);

        // When
        BookingDto result = bookingService.updateBookingById(admin, bookingId, requestDto);

        // Then
        assertThat(result).isEqualTo(updatedBookingDto);
        assertThat(existingBooking.getStatus()).isEqualTo(Status.CONFIRMED);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingRepository, times(1)).save(existingBooking);
        verify(bookingMapper, times(1)).toDto(existingBooking);
        verify(redisService, times(1)).deletePattern("bookings::all::*");
        verify(redisService, times(1)).deletePattern("bookings::user::*");
        verify(redisService, times(1))
                .save(eq("booking::" + bookingId), eq(updatedBookingDto));
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService);
    }

    @Test
    @DisplayName("Verify deleteBookingById() method cancels booking and sends notification")
    public void deleteBookingById_ValidId_CancelBookingAndSendNotification() {
        // Given
        User user = new User().setId(1L);
        Long bookingId = 1L;
        Accommodation accommodation = new Accommodation().setId(2L).setType(Type.APARTMENT)
                .setLocation(new Address().setStreet("Test").setHouse("1"));
        Booking existingBooking = new Booking().setId(bookingId)
                .setStatus(Status.PENDING)
                .setAccommodation(accommodation)
                .setCheckInDate(LocalDate.now())
                .setCheckOutDate(LocalDate.now().plusDays(2));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existingBooking));
        Booking canceledBooking = new Booking().setId(bookingId)
                .setStatus(Status.CANCELED)
                .setAccommodation(accommodation)
                .setCheckInDate(LocalDate.now())
                .setCheckOutDate(LocalDate.now().plusDays(2));
        when(bookingRepository.save(any(Booking.class))).thenReturn(canceledBooking);

        // When
        bookingService.deleteBookingById(user, bookingId);

        // Then
        assertThat(existingBooking.getStatus()).isEqualTo(Status.CANCELED);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingRepository, times(1)).save(existingBooking);
        verify(redisService, times(1)).deletePattern("bookings::all::*");
        verify(redisService, times(1)).deletePattern("bookings::user::*");
        verify(redisService, times(1)).delete("booking::" + bookingId);
        verify(notificationService, times(1)).sendNotification(anyString());
        verifyNoMoreInteractions(bookingRepository, redisService, notificationService);
    }
}
