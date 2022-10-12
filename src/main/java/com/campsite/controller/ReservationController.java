package com.campsite.controller;

import com.campsite.controller.utils.OnCreate;
import com.campsite.controller.utils.OnUpdate;
import com.campsite.controller.utils.ReservationRequest;
import com.campsite.exceptions.ResourceNotFoundException;
import com.campsite.model.Reservation;
import com.campsite.persistence.entity.ReservationEntity;
import com.campsite.service.ReservationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.groups.Default;
import java.time.LocalDate;
import java.util.List;

/**
 * This controller is responsible for handling all the requests related to the campsite reservations.
 *
 * @author Natalie Villanueva
 * @version 1.0
 */

@RestController
@RequestMapping("/api/v1")
@Validated
public class ReservationController {
    private static final Logger logger = LogManager.getLogger(ReservationController.class);

    @Autowired
    private ReservationService reservationService;

    /**
     * Checks the availability for a given date range (period).
     *
     * @param startDate the start date for availability check
     * @param endDate   the end date for availability check
     * @return the list of available dates
     */
    @GetMapping("/availabilities")
    public List<LocalDate> checkAvailability(@RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
                                             LocalDate startDate, @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        // Find all available dates for the given date range
        List<LocalDate> availableDates = reservationService.findAvailableDates(startDate, endDate);
        logger.info("Available dates found between the period of " + startDate + " and " + endDate + " are: " + availableDates.toString());
        return availableDates;
    }

    /**
     * **Exposing this method solely for the purpose of this challenge in order to all reservations entities and their versions.**
     * Retrieve all the reservations entities
     *
     * @return the list of reservations
     */
    @GetMapping("/reservation/")
    public List<ReservationEntity> retrieveAllReservations() {
        // Fetch all the reservations
        return reservationService.retrieveAllReservations();
    }

    /**
     * Retrieve a specific reservation
     *
     * @param id of the reservation
     * @return the reservation
     * @throws ResourceNotFoundException
     */
    @GetMapping("/reservation/{id}")
    public Reservation retrieveReservation(@PathVariable String id) {
        // Find the specific reservation
        Reservation reservation = reservationService.retrieveReservation(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with this ID: " + id + " does not exist."));

        logger.info("Found reservation with ID : " + id);
        return reservation;
    }


    /**
     * Creates a reservation with the given information.
     *
     * @param reservationRequest the requested reservation information
     * @return the unique identifier for the reservation
     */
    @PostMapping("/reservation")
    @ResponseStatus(HttpStatus.CREATED)
    public String createReservation(@RequestBody @Validated({OnCreate.class, Default.class}) ReservationRequest reservationRequest) {
        // Create reservation with specified information
        String reservationCreatedExternalIdentifier = reservationService.createReservation(reservationRequest);
        logger.info("Successfully created reservation with ID : " + reservationCreatedExternalIdentifier);
        return "Successfully created reservation with ID : " + reservationCreatedExternalIdentifier;
    }


    /**
     * Updates an existing reservation with the given information.
     *
     * @param reservationRequest the request reservation information
     * @return the unique identifier for the reservation
     */
    @PatchMapping("/reservation/{id}")
    public Reservation updateReservation(@PathVariable String id, @Validated({OnUpdate.class, Default.class}) @RequestBody ReservationRequest reservationRequest) {

        // 1. Update reservation
        Reservation updatedReservation = reservationService.updateReservation(id, reservationRequest);
        logger.info("Successfully updated reservation with ID : " + updatedReservation.getExternalIdentifier());
        return updatedReservation;
    }

    /**
     * Cancels an existing reservation.
     *
     * @param id the request reservation ID
     * @return the unique identifier for the reservation
     */
    @DeleteMapping("/reservation/{id}")
    ResponseEntity<String> cancelReservation(@PathVariable String id) {
        // Cancel reservation
        reservationService.cancelReservation(id);
        logger.info("Successfully cancelled reservation with ID : " + id);
        return ResponseEntity.ok("Successfully cancelled reservation with ID : " + id);
    }


}