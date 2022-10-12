package com.campsite.service;

import com.campsite.controller.utils.ReservationRequest;
import com.campsite.model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface ReservationService {
    List<LocalDate> findAvailableDates(LocalDate startDate, LocalDate endDate);
    List<Reservation> retrieveAllReservations();
    Optional<Reservation> retrieveReservation(String id);
    String createReservation(ReservationRequest reservationRequest);
    Reservation updateReservation(Reservation existingReservation, ReservationRequest reservationRequested);
    void cancelReservation(String id);
}
