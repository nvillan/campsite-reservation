package com.campsite.service.impl;

import com.campsite.common.generator.ExternalIdentifierGenerator;
import com.campsite.common.generator.impl.ExternalIdentifierGeneratorImpl;
import com.campsite.controller.utils.ReservationRequest;
import com.campsite.exceptions.InvalidParameterException;
import com.campsite.exceptions.NoAvailabilityException;
import com.campsite.exceptions.ResourceNotFoundException;
import com.campsite.model.Reservation;
import com.campsite.model.Status;
import com.campsite.persistence.entity.ReservationEntity;
import com.campsite.persistence.repository.ReservationRepository;
import com.campsite.service.ReservationService;
import ma.glasnost.orika.BoundMapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger logger = LogManager.getLogger(ReservationServiceImpl.class);
    private static final int DEFAULT_NUM_OF_GUESTS = 4;
    private static final ExternalIdentifierGenerator externalIdentifierGenerator = new ExternalIdentifierGeneratorImpl();
    final DefaultMapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
    final BoundMapperFacade<ReservationEntity, Reservation> reservationEntityBoundMapper = mapperFactory.getMapperFacade(ReservationEntity.class, Reservation.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<LocalDate> findAvailableDates(LocalDate startDate, LocalDate endDate) {
        startDate = ObjectUtils.firstNonNull(startDate, LocalDate.now().plusDays(1));
        endDate = ObjectUtils.firstNonNull(endDate, LocalDate.now().plusDays(1).plusMonths(1));

        // 1. Validate date range
        if (!isPeriodValid(startDate, endDate)) {
            throw new InvalidParameterException("The date range entered is incorrect.");
        }

        // 2. Find reserved dates that are within date range
        Map<LocalDate, Boolean> busyDates = retrieveReservedDates(startDate, endDate);

        // 3. Compare reserved dates with date range to know available dates
        List<LocalDate> availableDates = Collections.synchronizedList(new ArrayList<>());
        LocalDate tempStartDate = startDate;

        while (tempStartDate.isBefore(endDate.plusDays(1))) {
            if (busyDates.isEmpty() || !busyDates.containsKey(tempStartDate)) {
                availableDates.add(tempStartDate);
            }
            tempStartDate = tempStartDate.plusDays(1);
        }
        logger.info(new StringBuilder().append("Found the following available dates: ").append(availableDates).toString());
        return availableDates;
    }

    @Transactional(readOnly = true)
    public List<ReservationEntity> retrieveAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Reservation> retrieveReservation(String id) {
        return Optional.ofNullable(reservationEntityBoundMapper.map(reservationRepository.findActiveReservationByExternalIdentifier(id)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = {NoAvailabilityException.class, InvalidParameterException.class}, isolation = Isolation.SERIALIZABLE)
    public synchronized String createReservation(ReservationRequest reservationRequest) {
        // 1. Validate date range for reservation
        validateDatesForReservation(reservationRequest.getCheckinDate(), reservationRequest.getCheckoutDate());

        //2. Create reservation entity
        ReservationEntity reservationEntity = populateReservationEntity(reservationRequest);
        reservationRepository.saveAndFlush(reservationEntity);

        logger.debug(new StringBuilder().append("Successfully created reservation: ").append(reservationEntity).toString());
        return reservationEntity.getExternalIdentifier();
    }

    @Transactional
    public Reservation updateReservation(String id, ReservationRequest reservationRequested) {
        // 1. Fetch reservation
        ReservationEntity existingReservation = reservationRepository.findActiveReservationByExternalIdentifier(id);
        if (existingReservation == null) {
            throw new ResourceNotFoundException("Reservation with ID: " + id + " does not exist.");
        }
        logger.info("Found reservation with ID : " + id);

        // 2. Validate date range for reservation if it applies
        if (reservationRequested.getCheckinDate() != null || reservationRequested.getCheckoutDate() != null) {
            LocalDate newCheckinDate = ObjectUtils.firstNonNull(reservationRequested.getCheckinDate(), existingReservation.getCheckinDate());
            LocalDate newCheckoutDate = ObjectUtils.firstNonNull(reservationRequested.getCheckoutDate(), existingReservation.getCheckoutDate());
            validateDatesForReservation(newCheckinDate, newCheckoutDate);
        }

        // 3. Update reservation info
        updateExistingReservation(existingReservation, reservationRequested);
        reservationRepository.saveAndFlush(existingReservation);
        logger.debug(new StringBuilder().append("Successfully updated reservation: ").append(existingReservation).toString());
        return reservationEntityBoundMapper.map(existingReservation);
    }

    @Transactional
    public void cancelReservation(String id) {
        // 1. Fetch reservation
        ReservationEntity existingReservation = reservationRepository.findActiveReservationByExternalIdentifier(id);
        if (existingReservation == null) {
            throw new ResourceNotFoundException("Reservation with ID: " + id + " does not exist.");
        }
        logger.info("Found reservation with ID : " + id);

        // 2. Update reservation status
        existingReservation.setStatus(Status.CANCELLED.name());
        logger.info(new StringBuilder().append("Successfully cancelled reservation with external identifier: ").append(existingReservation.getExternalIdentifier()).toString());
        reservationRepository.saveAndFlush(existingReservation);
    }

    public synchronized boolean isSlotAvailableForNewReservation(LocalDate startDate, LocalDate endDate) {
       return reservationRepository.findReservationsForGivenPeriodForCreation(startDate, endDate).isEmpty();
    }

    private ReservationEntity populateReservationEntity(ReservationRequest reservation) {
        //Can be changed to use the orika mapper instead
        ReservationEntity reservationEntity = new ReservationEntity();
        reservationEntity.setFirstName(reservation.getFirstName());
        reservationEntity.setLastName(reservation.getLastName());
        reservationEntity.setEmail(reservation.getEmail());
        reservationEntity.setStatus(Status.ACTIVE.name());
        reservationEntity.setCheckinDate(reservation.getCheckinDate());
        reservationEntity.setCheckoutDate(reservation.getCheckoutDate());
        reservationEntity.setNumOfGuests(reservation.getNumOfGuests() == 0 ? DEFAULT_NUM_OF_GUESTS : reservation.getNumOfGuests());
        reservationEntity.setExternalIdentifier(externalIdentifierGenerator.getNext());
        return reservationEntity;
    }
    private void updateExistingReservation(ReservationEntity existingReservation, ReservationRequest reservationRequested) {
        //Can be changed to use the orika mapper instead
        existingReservation.setFirstName(ObjectUtils.firstNonNull(reservationRequested.getFirstName(), existingReservation.getFirstName()));
        existingReservation.setLastName(ObjectUtils.firstNonNull(reservationRequested.getLastName(), existingReservation.getLastName()));
        existingReservation.setEmail(ObjectUtils.firstNonNull(reservationRequested.getEmail(), existingReservation.getEmail()));
        existingReservation.setNumOfGuests(reservationRequested.getNumOfGuests() == 0 ? DEFAULT_NUM_OF_GUESTS : reservationRequested.getNumOfGuests());
        existingReservation.setCheckinDate(ObjectUtils.firstNonNull(reservationRequested.getCheckinDate(), existingReservation.getCheckinDate()));
        existingReservation.setCheckoutDate(ObjectUtils.firstNonNull(reservationRequested.getCheckoutDate(), existingReservation.getCheckoutDate()));
    }

    private Map<LocalDate, Boolean> retrieveReservedDates(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Boolean> reservedDates = new ConcurrentHashMap();
        List<ReservationEntity> reservationsList = reservationRepository.findReservationsForGivenPeriod(startDate, endDate);

        reservationsList.forEach(reservation -> {
            LocalDate tempCheckinDate = reservation.getCheckinDate().isBefore(startDate) ? startDate : reservation.getCheckinDate();
            LocalDate tempCheckoutDate = reservation.getCheckoutDate().isAfter(endDate) ? endDate : reservation.getCheckoutDate();

            while (tempCheckinDate.isBefore(tempCheckoutDate.plusDays(1))) {
                reservedDates.put(tempCheckinDate, true);
                tempCheckinDate = tempCheckinDate.plusDays(1);
            }
        });
        return reservedDates;
    }
    private synchronized void validateDatesForReservation(LocalDate newCheckinDate, LocalDate newCheckoutDate) {
        // 1. Check if dates are valid and respect maximum duration.
        if (!isDateRangeValidForReservation(newCheckinDate, newCheckoutDate)) {
            throw new InvalidParameterException("The date range entered is incorrect.");
        }

        // 2. Check if dates are available
        if (!isSlotAvailableForNewReservation(newCheckinDate, newCheckoutDate)) {
            throw new NoAvailabilityException("There are no availabilities for the dates provided.");
        }
    }

    private boolean isDateRangeValidForReservation(LocalDate startDate, LocalDate endDate) {
        LocalDate todayDate = LocalDate.now();
        if (startDate.isAfter(todayDate) && endDate.isAfter(startDate) && startDate.isBefore(todayDate.plusDays(2).plusMonths(1))) {
            Long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
            return daysDiff > 0 && daysDiff <= 3;
        }
        return false;
    }

    private boolean isPeriodValid(LocalDate startDate, LocalDate endDate) {
        LocalDate todayDate = LocalDate.now();
        return startDate.isAfter(todayDate) && endDate.isAfter(startDate) && startDate.isBefore(todayDate.plusDays(2).plusMonths(1)) && endDate.isBefore(todayDate.plusDays(5).plusMonths(1));
    }
}
