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
    private final AccommodationService accommodationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new accommodation",
            description = "Permits the addition of new accommodations"
    )
    public AccommodationDto createAccommodation(
            @RequestBody @Valid CreateAccommodationRequestDto requestDto) {
        return accommodationService.save(requestDto);
    }

    @GetMapping
    @Operation(
            summary = "Get all accommodations",
            description = "Provides a list of available accommodations"
    )
    public List<AccommodationDto> getAccommodations(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        return accommodationService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get accommodation information by id",
            description = "Retrieves detailed information about a specific accommodation"
    )
    public AccommodationDto getAccommodationById(@PathVariable @Positive Long id) {
        return accommodationService.findAccommodationById(id);
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
        return accommodationService.updateAccommodationById(id, requestDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an accommodation by id",
            description = "Enables the removal of accommodations"
    )
    public ResponseEntity<Void> deleteAccommodationById(@PathVariable @Positive Long id) {
        accommodationService.deleteAccommodationById(id);
        return ResponseEntity.noContent().build();
    }
}
