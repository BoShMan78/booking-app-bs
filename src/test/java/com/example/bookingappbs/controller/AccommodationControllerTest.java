package com.example.bookingappbs.controller;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import com.example.bookingappbs.dto.address.AddressDto;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Accommodation.Type;
import com.example.bookingappbs.model.Address;
import com.example.bookingappbs.service.accommodation.AccommodationService;
import com.example.bookingappbs.service.notification.TelegramService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AccommodationControllerTest {
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
    private AccommodationService accommodationService;


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
        }
    }

    @AfterEach
    void afterEach(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/accommodations/drop-all-accommodations.sql")
            );
        }
    }

    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    @DisplayName("Create accommodation")
    void createAccommodation_ValidCreateAccommodationRequestDto_Ok() throws Exception {
        // Given
        Address address = new Address();
        address.setCountry("Ukraine");
        address.setCity("Odesa");
        address.setStreet("Deribasovskaya str.");
        address.setHouse("1a");
        address.setApartment(1);

        CreateAccommodationRequestDto requestDto = new CreateAccommodationRequestDto(
                "APARTMENT",
                address,
                "1 bedroom",
                List.of("parking", "wi-fi"),
                BigDecimal.valueOf(50.50),
                1
        );

        AddressDto addressDto = new AddressDto(
                address.getId(),
                address.getCountry(),
                address.getCity(),
                address.getStreet(),
                address.getHouse(),
                address.getApartment()
        );
        AccommodationDto expected = new AccommodationDto(
                1L,
                Accommodation.Type.valueOf(requestDto.type()),
                addressDto,
                requestDto.size(),
                requestDto.amenities(),
                requestDto.dailyRate(),
                requestDto.availability()
        );

        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        //When
        MvcResult result = mockMvc.perform(
                post("/accommodations")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        AccommodationDto actual = objectMapper
                .readValue(result.getResponse().getContentAsString(), AccommodationDto.class);
        Assertions.assertNotNull(actual);
        Assertions.assertNotNull(actual.id());
        EqualsBuilder.reflectionEquals(expected, actual, "id");
    }

    @Test
    @DisplayName("Get all accommodations")
    void getAccommodations_GivenAccommodationsInCatalog_Ok() throws Exception {
        //Given
        AddressDto address1 = new AddressDto(
                1L,
                "Ukraine",
                "Odesa"
                ,"Deribasovskaya",
                "1",
                1
        );
        AddressDto address2 = new AddressDto(
                2L,
                "Ukraine",
                "Kyiv"
                ,"Khreshchatyk",
                "2",
                null
        );
        AddressDto address3 = new AddressDto(
                3L,
                "Ukraine",
                "Lviv"
                ,"Rynok Square",
                "4",
                null
        );

        List<AccommodationDto> expected = new ArrayList<>();
        expected.add(new AccommodationDto(
            1L,
                Type.APARTMENT,
                address1,
                "1 bedroom",
                List.of(),
                BigDecimal.valueOf(55),
                10
        ));
        expected.add(new AccommodationDto(
                2L,
                Type.HOUSE,
                address2,
                "3 bedrooms",
                List.of(),
                BigDecimal.valueOf(120.50),
                5
        ));
        expected.add(new AccommodationDto(
                1L,
                Type.CONDO,
                address3,
                "2 bedrooms",
                List.of(),
                BigDecimal.valueOf(80),
                2
        ));

        //When
        MvcResult result = mockMvc.perform(get("/accommodations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        AccommodationDto[] actual = objectMapper.readValue(result.getResponse()
                .getContentAsByteArray(), AccommodationDto[].class);
        Assertions.assertEquals(3, actual.length);
        for (int i = 0; i < expected.size() ; i++) {
            AccommodationDto expectedAccommodation = expected.get(i);
            AccommodationDto actualAccommodation = actual[i];
            EqualsBuilder.reflectionEquals(expectedAccommodation, actualAccommodation, "id");
        }
    }

    @Test
    @DisplayName("Get accommodation information by id")
    void getAccommodationById_GivenAccommodationInCatalog_Ok() throws Exception {
        //Given
        Long id = 1L;
        AddressDto address = new AddressDto(
                1L,
                "Ukraine",
                "Odesa"
                ,"Deribasovskaya",
                "1",
                1
        );
        AccommodationDto expected = new AccommodationDto(
                1L,
                Type.APARTMENT,
                address,
                "1 bedroom",
                List.of(),
                BigDecimal.valueOf(55),
                10
        );

        //When
        MvcResult result = mockMvc.perform(get("/accommodations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        AccommodationDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                AccommodationDto.class);
        Assertions.assertNotNull(actual);
        Assertions.assertNotNull(actual.id());
        EqualsBuilder.reflectionEquals(expected, actual, "id");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Update accommodation by id")
    void updateAccommodationById_GivenValidAccommodation_Ok() throws Exception {
        UpdateAccommodationRequestDto requestDto = new UpdateAccommodationRequestDto(
                null,
                null,
                null,
                List.of(),
                BigDecimal.valueOf(99),
                null
        );
        AddressDto address = new AddressDto(
                1L,
                "Ukraine",
                "Odesa"
                ,"Deribasovskaya",
                "1",
                1
        );
        AccommodationDto expected = new AccommodationDto(
                1L,
                Type.APARTMENT,
                address,
                "1 bedroom",
                List.of(),
                requestDto.dailyRate(),
                10
        );

        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        //When
        Long id = 1L;
        MvcResult result = mockMvc.perform(patch("/accommodations/{id}", id)
                        .content(jsonRequest)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        AccommodationDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                AccommodationDto.class);
        Assertions.assertNotNull(actual);
        Assertions.assertNotNull(actual.id());
        Assertions.assertEquals(expected.dailyRate(), actual.dailyRate());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Delete accommodation by id")
    void deleteAccommodationById_existingId_NoContent() throws Exception {
        Long id = 1L;
        willDoNothing().given(accommodationService).deleteAccommodationById(id);

        mockMvc.perform(delete("/accommodations/{id}", id))
                .andExpect(status().isNoContent());

        verify(accommodationService, times(1)).deleteAccommodationById(id);
    }
}
