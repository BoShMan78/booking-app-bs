package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import com.example.bookingappbs.service.accommodation.AccommodationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Managing accommodation inventory", description = "CRUD for Accommodations")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/accommodations")
public class AccommodationController {
    private static final Logger logger = LogManager.getLogger(AccommodationController.class);

    private final AccommodationService accommodationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new accommodation",
            description = "Permits the addition of new accommodations"
    )
    public AccommodationDto createAccommodation(
            @RequestBody @Valid CreateAccommodationRequestDto requestDto
    ) {
        logger.info("Processing request to create a new accommodation. "
                        + "Type: {}, Address: {}",
                requestDto.type(), requestDto.location());
        AccommodationDto savedAccommodation = accommodationService.save(requestDto);

        logger.info("Accommodation successfully created with ID: {}", savedAccommodation.id());
        return savedAccommodation;
    }

    @GetMapping
    @Operation(
            summary = "Get all accommodations",
            description = "Provides a list of available accommodations"
    )
    public List<AccommodationDto> getAccommodations(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        logger.info("Received request to get all accommodations. "
                        + "Page number: {}, Page size: {}, Sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        List<AccommodationDto> accommodationDtos = accommodationService.findAll(pageable);

        logger.info("Retrieved {} accommodations.", accommodationDtos.size());
        return accommodationDtos;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get accommodation information by id",
            description = "Retrieves detailed information about a specific accommodation"
    )
    public AccommodationDto getAccommodationById(@PathVariable @Positive Long id) {
        logger.info("Received request to get accommodation with ID: {}", id);
        AccommodationDto accommodationDto = accommodationService.findAccommodationById(id);

        logger.info("Accommodation information with ID {} successfully retrieved.", id);
        return accommodationDto;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    @Operation(
            summary = "updates to accommodation details",
            description = "Allows updates to accommodation details, including inventory management"
    )
    public AccommodationDto updateAccommodationById(
            @PathVariable @Positive Long id,
            @RequestBody UpdateAccommodationRequestDto requestDto
    ) {
        logger.info("Received request to update accommodation with ID: {}. "
                        + "Type: {}, Address: {}",
                id, requestDto.type(), requestDto.location());
        AccommodationDto accommodationDto = accommodationService
                .updateAccommodationById(id, requestDto);

        logger.info("Accommodation information with ID {} successfully updated.", id);
        return accommodationDto;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an accommodation by id",
            description = "Enables the removal of accommodations"
    )
    public ResponseEntity<Void> deleteAccommodationById(@PathVariable @Positive Long id) {
        logger.info("Received request to delete accommodation with ID: {}", id);
        accommodationService.deleteAccommodationById(id);

        logger.info("Accommodation with ID {} successfully deleted.", id);
        return ResponseEntity.noContent().build();
    }
}
