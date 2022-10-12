package com.campsite.persistence.repository;

import com.campsite.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    @Query("select r from ReservationEntity r where external_identifier = ?1 and status ='ACTIVE'")
    Optional<ReservationEntity> findActiveReservationByExternalIdentifier(String id);

    @Modifying
    @Query("update ReservationEntity set status ='CANCELLED' where external_identifier = ?1")
    void deleteReservation(String id);

    @Query("select r from ReservationEntity r where status ='ACTIVE' and ((checkinDate between ?1 and ?2) or (checkoutDate between ?1 and ?2)) order by  checkinDate")
   //         "(checkinDate > ?1) and (checkoutDate < ?2  or checkoutDate > ?2");
    List<ReservationEntity> findReservationsForGivenPeriod(LocalDate startDate, LocalDate endDate);


//
//    Optional<ReservationEntity> findByExternalId(Long uuid);
////
//    List<Date> findAvailableDates(Long uuid);
}
