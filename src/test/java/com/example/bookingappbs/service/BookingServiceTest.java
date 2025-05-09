package com.example.bookingappbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.exception.AccessDeniedException;
import com.example.bookingappbs.exception.PendingPaymentException;
import com.example.bookingappbs.mapper.AccommodationMapper;
import com.example.bookingappbs.mapper.BookingMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Accommodation.Type;
import com.example.bookingappbs.model.Address;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.Role;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.repository.UserRepository;
import com.example.bookingappbs.service.booking.BookingCacheKeyBuilder;
import com.example.bookingappbs.service.booking.BookingNotificationBuilder;
import com.example.bookingappbs.service.booking.BookingServiceImpl;
import com.example.bookingappbs.service.notification.NotificationService;
import com.example.bookingappbs.service.payment.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @Mock
    private BookingCacheKeyBuilder cacheKeyBuilder;
    @Mock
    private BookingNotificationBuilder notificationBuilder;

    private Address address;
    private Long accommodationId;
    private Accommodation accommodation;
    private CreateBookingRequestDto createBookingRequestDto;
    private User user;
    private Booking booking;
    private BookingDto bookingDto;
    private Long userId;
    private Pageable pageable;
    private String userBookingsCacheKey;
    private Long bookingId;
    private UpdateBookingRequestDto updateBookingRequestDto;
    private User admin;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        address = new Address()
                .setCountry("Ukraine")
                .setCity("Odesa")
                .setStreet("Deribasovskaya str.")
                .setHouse("1a")
                .setApartment(1);

        accommodationId = 1L;
        accommodation = new Accommodation().setId(accommodationId)
                .setType(Type.APARTMENT)
                .setLocation(address)
                .setDailyRate(BigDecimal.valueOf(75.50))
                .setAvailability(1);

        createBookingRequestDto = new CreateBookingRequestDto(
                LocalDate.of(2027, 01, 15),
                LocalDate.of(2027, 01, 18),
                accommodationId
        );

        customerRole = new Role("CUSTOMER");
        adminRole = new Role("ADMIN");

        userId = 1L;
        user = new User().setId(userId).setRoles(Set.of(customerRole));

        booking = new Booking().setCheckInDate(createBookingRequestDto.checkInDate())
                .setCheckOutDate(createBookingRequestDto.checkOutDate())
                .setAccommodation(accommodation)
                .setUser(user);

        bookingDto = new BookingDto(
                null,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                accommodationId,
                user.getId(),
                Status.PENDING.toString()
        );

        pageable = PageRequest.of(0, 10);
        userBookingsCacheKey = "bookings::user::" + userId
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();

        bookingId = 1L;

        updateBookingRequestDto = new UpdateBookingRequestDto(
                null,
                null,
                null,
                Status.CONFIRMED.toString()
        );

        admin = new User().setId(2L).setRoles(Set.of(adminRole));
    }

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Verify save() method works")
    public void save_ValidCreateBookingRequestDto_ReturnBookingDto() {
        //Given
        when(paymentService.countPendingPaymentsForUser(userId)).thenReturn(0L);
        when(bookingMapper.toModel(createBookingRequestDto)).thenReturn(booking);
        when(accommodationRepository.findById(accommodationId))
                .thenReturn(Optional.of(accommodation));
        when(bookingRepository.countOverLappingBookings(any(), any(), any(), any())).thenReturn(0);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);
        String notificationMessage = "New booking created: \n"
                + "Booking ID: null\n"
                + "Accommodation ID: 1\n"
                + "Type: APARTMENT\n"
                + "Location: Ukraine Odesa, Deribasovskaya str. 1a, Odesa, Ukraine\n"
                + "Check-in Date: 2027-01-15\n"
                + "Check-out Date: 2027-01-18";
        when(notificationBuilder.buildBookingNotificationMessage(
                "New booking created", booking, accommodation)).thenReturn(notificationMessage);
        doNothing().when(notificationService).sendNotification(notificationMessage);

        //When
        BookingDto savedBookingDto = bookingService.save(user, createBookingRequestDto);

        //Then
        assertThat(savedBookingDto).isEqualTo(bookingDto);
        verify(paymentService, times(1)).countPendingPaymentsForUser(userId);
        verify(bookingMapper, times(1)).toModel(createBookingRequestDto);
        verify(accommodationRepository, times(1)).findById(accommodationId);
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(bookingMapper, times(1)).toDto(booking);
        verify(redisService, times(1)).deletePattern("bookings*");
        verify(notificationBuilder, times(1)).buildBookingNotificationMessage(
                "New booking created", booking, accommodation);
        verify(notificationService, times(1)).sendNotification(anyString());

        verifyNoMoreInteractions(bookingRepository, bookingMapper, paymentService, redisService,
                notificationService, notificationBuilder);
    }

    @Test
    @DisplayName("save should throw PendingPaymentException if user has pending payments")
    void save_UserWithPendingPayments_ThrowsPendingPaymentException() {
        // When
        when(paymentService.countPendingPaymentsForUser(user.getId())).thenReturn(1L);

        PendingPaymentException exception = assertThrows(PendingPaymentException.class,
                () -> bookingService.save(user, createBookingRequestDto));

        // Then
        assertEquals("There are already pending payments for the user", exception.getMessage());
        verify(paymentService).countPendingPaymentsForUser(user.getId());
        verifyNoInteractions(bookingRepository, bookingMapper, accommodationRepository,
                redisService, notificationService);
    }

    @Test
    @DisplayName("Verify getBookingsByUserAndStatus() method works and fetches from DB and caches")
    public void getBookingsByUserAndStatus_NoCache_FetchFromDbAndCache() {
        // Given
        Status status = Status.PENDING;
        String key = "bookings::user::" + userId + "::status::" + status
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();
        when(cacheKeyBuilder.buildBookingsPageKey(userId, status.toString(), pageable))
                .thenReturn(key);
        when(redisService.findAll(key, BookingDto.class)).thenReturn(null);
        Page<Booking> bookingPage = new PageImpl<>(List.of(new Booking().setId(bookingId)));
        when(bookingRepository.findByUserIdAndStatusOptional(userId, status, pageable))
                .thenReturn(bookingPage);
        List<BookingDto> bookingDtos = List.of(new BookingDto(
                bookingId,
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
        verify(cacheKeyBuilder, times(1))
                .buildBookingsPageKey(userId, status.toString(), pageable);
        verify(redisService, times(1)).findAll(key, BookingDto.class);
        verify(bookingRepository, times(1))
                .findByUserIdAndStatusOptional(userId, status, pageable);
        verify(bookingMapper, times(1)).toDto(any(Booking.class));
        verify(redisService, times(1)).save(key, bookingDtos);
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService, cacheKeyBuilder);
    }

    @Test
    @DisplayName("Verify getBookingsByUser() method works and returns cached data")
    public void getBookingsByUser_ExistingCache_ReturnCachedBookings() {
        // Given
        when(cacheKeyBuilder.buildBookingsPageKey(user.getId(), null, pageable))
                .thenReturn(userBookingsCacheKey);
        when(redisService.findAll(userBookingsCacheKey, BookingDto.class))
                .thenReturn(List.of(bookingDto));

        // When
        List<BookingDto> result = bookingService.getBookingsByUser(user, pageable);

        // Then
        assertThat(result).isEqualTo(List.of(bookingDto));
        verify(cacheKeyBuilder, times(1)).buildBookingsPageKey(user.getId(), null, pageable);
        verify(redisService, times(1)).findAll(userBookingsCacheKey, BookingDto.class);
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService, cacheKeyBuilder);
    }

    @Test
    @DisplayName("Verify getBookingById() method works and fetches from DB and caches")
    public void getBookingById_NoCache_FetchFromDbAndCache() {
        // Given
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        // When
        BookingDto result = bookingService.getBookingById(user, bookingId);

        // Then
        assertThat(result).isEqualTo(bookingDto);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingMapper, times(1)).toDto(booking);
        verifyNoMoreInteractions(bookingRepository, bookingMapper, redisService);
    }

    @Test
    @DisplayName("Verify updateBookingById() method works for admin updating status")
    public void updateBookingById_AdminUpdateUserStatus_Success() {
        // Given
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository
                .countOverlappingBookingsExcludingCurrent(any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(bookingDto);

        System.out.println("Admin roleIds: " + admin.getRoles());

        // When
        BookingDto result = bookingService
                .updateBookingByAdmin(bookingId, updateBookingRequestDto);

        // Then
        assertThat(result).isEqualTo(bookingDto);
        assertThat(booking.getStatus()).isEqualTo(Status.CONFIRMED);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingRepository, times(1))
                .countOverlappingBookingsExcludingCurrent(any(), any(), any(), any(), any());
        verify(bookingRepository, times(1)).save(booking);
        verify(bookingMapper, times(1)).toDto(booking);
        verify(redisService, times(1)).deletePattern("bookings*");
        verifyNoMoreInteractions(bookingMapper, redisService);
    }

    @Test
    @DisplayName("updateBookingById should throw AccessDeniedException "
            + "for CUSTOMER trying to update status")
    void updateBookingById_Customer_UpdateUserStatus_ThrowsAccessDeniedException() {
        // Given
        Long bookingIdToUpdate = 1L;
        UpdateBookingRequestDto updateDto = new UpdateBookingRequestDto(
                LocalDate.of(2027, 01, 16),
                LocalDate.of(2027, 01, 19),
                2L,
                Status.CONFIRMED.toString()
        );
        Booking existingBooking = new Booking()
                .setId(bookingIdToUpdate)
                .setCheckInDate(LocalDate.now())
                .setCheckOutDate(LocalDate.now().plusDays(3))
                .setAccommodation(new Accommodation())
                .setUser(user)
                .setStatus(Status.PENDING);

        when(bookingRepository.findById(bookingIdToUpdate))
                .thenReturn(Optional.of(existingBooking));

        // When
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> bookingService.updateUserBookingById(user, bookingIdToUpdate, updateDto));

        // Then
        assertEquals("The user does not have permission to change the booking status. "
                        + "Please contact the administrator.",
                exception.getMessage());
        verify(bookingRepository).findById(bookingIdToUpdate);
        verifyNoInteractions(bookingMapper, accommodationRepository, paymentService, redisService,
                notificationService, userRepository, accommodationMapper);
        assertEquals(Status.PENDING, existingBooking.getStatus(), "Status should not be updated");
    }

    @Test
    @DisplayName("Verify deleteBookingById() method cancels booking and sends notification")
    public void deleteBookingById_ValidId_CancelBookingAndSendNotification() {
        // Given
        Accommodation accommodationToDelete = new Accommodation().setId(2L).setType(Type.APARTMENT)
                .setLocation(new Address().setStreet("Test").setHouse("1"));
        Booking existingBookingToDelete = new Booking().setId(bookingId)
                .setStatus(Status.PENDING)
                .setAccommodation(accommodationToDelete)
                .setCheckInDate(LocalDate.now())
                .setCheckOutDate(LocalDate.now().plusDays(2));
        Booking canceledBooking = new Booking().setId(bookingId)
                .setStatus(Status.CANCELED)
                .setAccommodation(accommodationToDelete)
                .setCheckInDate(LocalDate.now())
                .setCheckOutDate(LocalDate.now().plusDays(2));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(existingBookingToDelete));
        when(bookingRepository.save(any(Booking.class))).thenReturn(canceledBooking);
        String notificationMessage = "Booking canceled: \n"
                + "Booking ID: 1\n"
                + "Accommodation ID: 2\n"
                + "Type: APARTMENT\n"
                + "Location: Test 1, null, null\n"
                + "Check-in Date: " + LocalDate.now() + "\n"
                + "Check-out Date: " + LocalDate.now().plusDays(2);
        when(notificationBuilder
                .buildBookingNotificationMessage(
                        "Booking canceled",
                        existingBookingToDelete,
                        accommodationToDelete
                )).thenReturn(notificationMessage);
        doNothing().when(notificationService).sendNotification(notificationMessage);

        // When
        bookingService.deleteBookingById(user, bookingId);

        // Then
        assertThat(existingBookingToDelete.getStatus()).isEqualTo(Status.CANCELED);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingRepository, times(1)).save(existingBookingToDelete);
        verify(bookingRepository, times(1)).delete(existingBookingToDelete);
        verify(redisService, times(1)).deletePattern("bookings*");
        verify(notificationService, times(1)).sendNotification(anyString());
        verifyNoMoreInteractions(bookingRepository, redisService, notificationService,
                notificationBuilder);
    }
}
