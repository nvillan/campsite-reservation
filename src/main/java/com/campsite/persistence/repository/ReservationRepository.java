package com.campsite.persistence.repository;

import com.campsite.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    @Query("select r from ReservationEntity r where external_identifier = ?1 and status ='ACTIVE'")
    ReservationEntity findActiveReservationByExternalIdentifier(String id);

    @Query("select r from ReservationEntity r where status ='ACTIVE' and ((checkinDate between ?1 and ?2) or (checkoutDate between ?1 and ?2)) order by  checkinDate")
    List<ReservationEntity> findReservationsForGivenPeriod(LocalDate startDate, LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select r from ReservationEntity r where status ='ACTIVE' and ((checkinDate between ?1 and ?2) or (checkoutDate between ?1 and ?2)) order by  checkinDate")
    List<ReservationEntity> findReservationsForGivenPeriodForCreation(LocalDate startDate, LocalDate endDate);
}
