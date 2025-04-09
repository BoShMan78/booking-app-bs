package com.example.bookingappbs.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.mapper.UserMapper;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.service.booking.BookingService;
import com.example.bookingappbs.service.notification.TelegramService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class BookingControllerTest {
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Filter springSecurityFilterChain;
    @MockBean
    private TelegramBotsApi telegramBotsApi;
    @MockBean
    private TelegramService telegramService;
    @MockBean
    private BookingService bookingService;
    @Autowired
    private UserMapper userMapper;

    private User mockUser;
    private BookingDto expectedBookingDto;
    private CreateBookingRequestDto createBookingRequestDto;
    private UpdateBookingRequestDto updateBookingRequestDto;

    @BeforeEach
    void beforeEach(@Autowired WebApplicationContext applicationContext,
                    @Autowired DataSource dataSource) throws SQLException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
        teardown(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/accommodations/add-three-accommodations.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/users/add-test-user.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/bookings/add-three-bookings.sql")
            );
        }

        mockUser = new User().setId(1L)
                .setEmail("user@example.com")
                .setPassword("password")
                .setFirstName("John")
                .setLastName("Smith")
                .setRole(Role.CUSTOMER);

        createBookingRequestDto = new CreateBookingRequestDto(
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                1L
        );

        expectedBookingDto = new BookingDto(
                1L,
                createBookingRequestDto.checkInDate(),
                createBookingRequestDto.checkOutDate(),
                createBookingRequestDto.accommodationId(),
                mockUser.getId(),
                Booking.Status.PENDING.name()
        );

        updateBookingRequestDto = new UpdateBookingRequestDto(
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                2L,
                "CONFIRMED"
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void afterEach(@Autowired DataSource dataSource) {
        teardown(dataSource);
        SecurityContextHolder.clearContext();
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/bookings/drop-all-bookings.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/users/drop-all-test-users.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/accommodations/drop-all-accommodations.sql")
            );
        }
    }

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Create booking")
    void createBooking_ValidCreateBookingRequestDto_Ok() throws Exception {
        //Given
        when(bookingService.save(eq(mockUser), any(CreateBookingRequestDto.class)))
                .thenReturn(expectedBookingDto);

        String jsonRequest = objectMapper.writeValueAsString(createBookingRequestDto);

        //When
        MvcResult result = mockMvc.perform(
                        post("/bookings")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        BookingDto actual = objectMapper.readValue(result.getResponse()
                .getContentAsString(), BookingDto.class);
        Assertions.assertNotNull(actual);
        Assertions.assertNotNull(actual.id());
        EqualsBuilder.reflectionEquals(expectedBookingDto, actual, "id");
        verify(bookingService, times(1)).save(eq(mockUser), any(CreateBookingRequestDto.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Get bookings by user ID and status - ADMIN")
    void getBookingsByUserIdAndStatus_Admin_Ok() throws Exception {
        //Given
        Long userId = 2L;
        Booking.Status status = Status.CONFIRMED;
        List<BookingDto> bookingDtos = List.of(
                new BookingDto(
                        2L,
                        LocalDate.now().plusDays(7),
                        LocalDate.now().plusDays(10),
                        3L,
                        userId,
                        status.name()
                )
        );

        Pageable mockPageable = PageRequest.of(0, 10);

        //When
        when(bookingService.getBookingsByUserAndStatus(eq(userId), eq(status), eq(mockPageable)))
                .thenReturn(bookingDtos);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        //Then
        mockMvc.perform(get("/bookings")
                        .param("userId", userId.toString())
                        .param("status", status.name())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)));

        verify(bookingService, times(1))
                .getBookingsByUserAndStatus(eq(userId), eq(status), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Get current user bookings")
    void getBookingsByUserId_Customer_Ok() throws Exception {
        //Given
        List<BookingDto> bookingDtos = List.of(
                new BookingDto(
                        3L,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(5),
                        1L, mockUser.getId(),
                        Booking.Status.PENDING.name())
        );

        //When
        when(bookingService.getBookingsByUser(eq(mockUser), any(Pageable.class)))
                .thenReturn(bookingDtos);

        //Then
        mockMvc.perform(get("/bookings/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(mockUser.getId().intValue())));
        verify(bookingService, times(1)).getBookingsByUser(eq(mockUser), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Get booking by ID - ADMIN")
    void getBookingById_Admin_Ok() throws Exception {
        //Given
        Long bookingId = 4L;
        BookingDto expectedBookingWithId = new BookingDto(
                bookingId,
                expectedBookingDto.checkInDate(),
                expectedBookingDto.checkOutDate(),
                expectedBookingDto.accommodationId(),
                expectedBookingDto.userId(),
                expectedBookingDto.status()
        );

        //When
        when(bookingService.getBookingById(any(), eq(bookingId))).thenReturn(expectedBookingWithId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        //Then
        mockMvc.perform(get("/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingId.intValue())));
        verify(bookingService, times(1)).getBookingById(any(), eq(bookingId));
    }

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Update booking by ID - CUSTOMER (can modify)")
    void updateBookingById_Customer_Ok() throws Exception {
        //Given
        Long bookingId = 6L;
        BookingDto updatedBookingDto = new BookingDto(
                bookingId,
                updateBookingRequestDto.checkInDate(),
                updateBookingRequestDto.checkOutDate(),
                updateBookingRequestDto.accommodationId(),
                mockUser.getId(),
                updateBookingRequestDto.status()
        );

        //When
        when(bookingService.canUserModifyBooking(eq(mockUser), eq(bookingId))).thenReturn(true);
        when(bookingService.updateBookingById(eq(mockUser), eq(bookingId),
                any(UpdateBookingRequestDto.class)))
                .thenReturn(updatedBookingDto);

        String jsonRequest = objectMapper.writeValueAsString(updateBookingRequestDto);

        //Then
        mockMvc.perform(patch("/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingId.intValue())))
                .andExpect(jsonPath("$.checkInDate",
                        is(updateBookingRequestDto.checkInDate().toString())))
                .andExpect(jsonPath("$.checkOutDate",
                        is(updateBookingRequestDto.checkOutDate().toString())))
                .andExpect(jsonPath("$.accommodationId",
                        is(updateBookingRequestDto.accommodationId().intValue())))
                .andExpect(jsonPath("$.status", is(updateBookingRequestDto.status())));

        verify(bookingService, times(1)).canUserModifyBooking(eq(mockUser), eq(bookingId));
        verify(bookingService, times(1)).updateBookingById(eq(mockUser), eq(bookingId),
                any(UpdateBookingRequestDto.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Delete booking by ID - CUSTOMER (can modify)")
    void deleteAccommodationById_Customer_NoContent() throws Exception {
        //Given
        Long bookingId = 7L;

        //When
        when(bookingService.canUserModifyBooking(eq(mockUser), eq(bookingId))).thenReturn(true);
        doNothing().when(bookingService).deleteBookingById(eq(mockUser), eq(bookingId));

        //Then
        mockMvc.perform(delete("/bookings/{id}", bookingId))
                .andExpect(status().isNoContent());

        verify(bookingService, times(1)).canUserModifyBooking(eq(mockUser), eq(bookingId));
        verify(bookingService, times(1)).deleteBookingById(eq(mockUser), eq(bookingId));
    }

    @Test
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    @DisplayName("Update booking by ID - forbidden (no access)")
    void updateBookingById_Forbidden_NoAccess() throws Exception {
        //Given
        Long bookingId = 8L;

        //When
        when(bookingService.canUserModifyBooking(eq(mockUser), eq(bookingId))).thenReturn(false);
        String jsonRequest = objectMapper.writeValueAsString(updateBookingRequestDto);

        //Then
        mockMvc.perform(patch("/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden());

        verify(bookingService, times(1)).canUserModifyBooking(eq(mockUser), eq(bookingId));
        verify(bookingService, never()).updateBookingById(any(), any(), any());
    }
}
