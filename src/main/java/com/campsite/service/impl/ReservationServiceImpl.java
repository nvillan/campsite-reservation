package com.campsite.service.impl;

import com.campsite.common.ExternalIdentifierGenerator;
import com.campsite.common.ExternalIdentifierGeneratorImpl;
import com.campsite.controller.utils.ReservationRequest;
import com.campsite.exceptions.InvalidParameterException;
import com.campsite.exceptions.NoAvailabilityException;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger logger = LogManager.getLogger(ReservationServiceImpl.class);
    private static final int DEFAULT_NUM_OF_GUESTS = 4;
    private static final ExternalIdentifierGenerator externalIdentifierGenerator = new ExternalIdentifierGeneratorImpl();
    @Autowired
    private ReservationRepository reservationRepository;

    final DefaultMapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
    final BoundMapperFacade<ReservationEntity, Reservation> reservationEntityBoundMapper = mapperFactory.getMapperFacade(ReservationEntity.class, Reservation.class);
    final BoundMapperFacade<ReservationRequest, ReservationEntity> reservationRequestBoundMapper = mapperFactory.getMapperFacade(ReservationRequest.class, ReservationEntity.class);

    public List<LocalDate> findAvailableDates(LocalDate startDate, LocalDate endDate) {
        startDate = ObjectUtils.firstNonNull(startDate, LocalDate.now().plusDays(1));
        endDate = ObjectUtils.firstNonNull(endDate, LocalDate.now().plusMonths(1));

        // 1. Validate date range
        if (!isPeriodValid(startDate, endDate)) {
            throw new InvalidParameterException("The date range entered is incorrect.");
        }

        // 2. Find reserved dates that are within date range
        Map<LocalDate, Boolean> busyDates = retrieveReservedDates(startDate, endDate);

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate tempStartDate = startDate;
        while (tempStartDate.isBefore(endDate.plusDays(1))) {
            if (!busyDates.containsKey(tempStartDate)) {
                availableDates.add(tempStartDate);
            }
            tempStartDate = tempStartDate.plusDays(1);
        }
        return availableDates;
    }

    public List<Reservation> retrieveAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        reservationRepository.findAll().forEach(
                reservationEntity -> {
                    reservations.add(reservationEntityBoundMapper.map(reservationEntity));

                }
        );
        return reservations;
    }

    public Optional<Reservation> retrieveReservation(String id) {
        Optional<ReservationEntity> reservationEntityOpt = reservationRepository.findActiveReservationByExternalIdentifier(id);
        if (reservationEntityOpt.isPresent()) {
            Optional.of(reservationEntityBoundMapper.map(reservationEntityOpt.get()));
        }
        return Optional.empty();
    }

    @Transactional
    public String createReservation(ReservationRequest reservationRequest) {
        // 1. Validate date range for reservation
        validateDatesForReservation(reservationRequest.getCheckinDate(), reservationRequest.getCheckoutDate());

        //2. Create reservation entity
        ReservationEntity reservationEntity = populateReservationEntity(reservationRequest);
        return reservationRepository.saveAndFlush(reservationEntity).getExternalIdentifier();
//        ReservationEntity reservationEntity = reservationRequestBoundMapper.map(reservation);
//        return reservationBoundMapper.mapReverse(reservationRepository.saveAndFlush(reservationEntity));
    }

    @Transactional
    public Reservation updateReservation(Reservation existingReservation, ReservationRequest reservationRequested) {
        // 1. Validate date range for reservation if it applies
        if (reservationRequested.getCheckinDate() != null || reservationRequested.getCheckoutDate() != null) {
            LocalDate newCheckinDate = ObjectUtils.firstNonNull(reservationRequested.getCheckinDate(), existingReservation.getCheckinDate());
            LocalDate newCheckoutDate = ObjectUtils.firstNonNull(reservationRequested.getCheckoutDate(), existingReservation.getCheckoutDate());
            validateDatesForReservation(newCheckinDate, newCheckoutDate);
        }

        // 2. Validate date range for reservation if it applies
        ReservationEntity re = updateReservationEntity(existingReservation, reservationRequested);
        reservationRepository.saveAndFlush(re);

        return null;
        //ReservationEntity entity = reservationBoundMapper.map(existingReservation);
//       return reservationBoundMapper.mapReverse(entity);
    }

    @Transactional
    public void cancelReservation(String reservationExternalIdentifier) {
        reservationRepository.deleteReservation(reservationExternalIdentifier);
    }

    private boolean isSlotAvailable(LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findReservationsForGivenPeriod(startDate, endDate).isEmpty();
    }

    private ReservationEntity populateReservationEntity(ReservationRequest reservation) {
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

    private ReservationEntity updateReservationEntity(Reservation existingReservation, ReservationRequest reservationRequested) {
        existingReservation.setFirstName(ObjectUtils.firstNonNull(reservationRequested.getFirstName(), existingReservation.getFirstName()));
        existingReservation.setLastName(ObjectUtils.firstNonNull(reservationRequested.getLastName(), existingReservation.getLastName()));
        existingReservation.setEmail(ObjectUtils.firstNonNull(reservationRequested.getEmail(), existingReservation.getEmail()));
        existingReservation.setNumOfGuests(reservationRequested.getNumOfGuests() == 0 ? DEFAULT_NUM_OF_GUESTS : reservationRequested.getNumOfGuests());
        existingReservation.setCheckinDate(ObjectUtils.firstNonNull(reservationRequested.getCheckinDate(), existingReservation.getCheckinDate()));
        existingReservation.setCheckoutDate(ObjectUtils.firstNonNull(reservationRequested.getCheckoutDate(), existingReservation.getCheckoutDate()));

        return null;
    }


    private Map<LocalDate, Boolean> retrieveReservedDates(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Boolean> reservedDates = new HashMap();
        List<ReservationEntity> reservationsList = reservationRepository.findReservationsForGivenPeriod(startDate, endDate);
        reservationsList.forEach(reservationEntity -> {
            LocalDate tempCheckinDate = reservationEntity.getCheckinDate().isBefore(startDate) ? startDate : reservationEntity.getCheckinDate();
            LocalDate tempCheckoutDate = reservationEntity.getCheckoutDate().isAfter(endDate) ? endDate : reservationEntity.getCheckoutDate();
            while (tempCheckinDate.isBefore(tempCheckoutDate.plusDays(1))) {
                reservedDates.put(tempCheckinDate, true);
                tempCheckinDate = tempCheckinDate.plusDays(1);
            }
        });
        return reservedDates;
    }

    //
//    private static AtomicLong idCounter = new AtomicLong();
//    private static final String prefix = "RSV";
//
//    public static String createID()
//    {
//        StringBuilder sb = new StringBuilder();
//        SecureRandom aw
//        return sb.append(prefix).append(valueOf(idCounter.getAndIncrement())).append()
//    }
    private void validateDatesForReservation(LocalDate newCheckinDate, LocalDate newCheckoutDate) {
        // 2. Check if dates are valid and respect maximum duration.
        if (!isDateRangeValidForReservation(newCheckinDate, newCheckoutDate)) {
            throw new InvalidParameterException("The date range entered is incorrect.");
        }

        // 2. Check if dates are available
        if (!isSlotAvailable(newCheckinDate, newCheckoutDate)) {
            throw new NoAvailabilityException("There are no availabilities for the dates provided.");
        }
    }

    private boolean isDateRangeValidForReservation(LocalDate startDate, LocalDate endDate) {
        LocalDate todayDate = LocalDate.now();
        if (startDate.isAfter(todayDate) && endDate.isAfter(startDate) && startDate.isBefore(todayDate.plusDays(1).plusMonths(1))) {
            Long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
            return daysDiff > 0 && daysDiff <= 3;
        }
        return false;
    }

    private boolean isPeriodValid(LocalDate startDate, LocalDate endDate) {
        LocalDate todayDate = LocalDate.now();
        return startDate.isAfter(todayDate) && endDate.isAfter(startDate) && startDate.isBefore(todayDate.plusDays(1).plusMonths(1))
                && endDate.isBefore(todayDate.plusDays(4).plusMonths(1));

    }
}
