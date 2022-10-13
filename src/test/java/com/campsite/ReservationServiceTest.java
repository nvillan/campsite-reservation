package com.campsite;

import com.campsite.controller.utils.ReservationRequest;
import com.campsite.persistence.entity.ReservationEntity;
import com.campsite.persistence.repository.ReservationRepository;
import com.campsite.service.impl.ReservationServiceImpl;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(OutputCaptureExtension.class)
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ReservationServiceTest {
    @Autowired
    private ReservationServiceImpl reservationService;
    @SpyBean
    private ReservationRepository reservationRepository;

    /**
    *
    * Test: handle concurrent requests to reservations for the campsite.
     * */
    @Test
    public void testConcurrentCreateReservationRequests() throws InterruptedException {

        // create reservation request
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        ArrayList<ReservationRequest> requests = new ArrayList<>();
        requests.add(createReservationRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4)));
        requests.add(createReservationRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4)));
        requests.add(createReservationRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));

        //execute threads
        requests.forEach(reservationRequest -> {
            executor.execute(() -> reservationService.createReservation(reservationRequest));
        });

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // then fetch all reservations
        List<ReservationEntity> reservations = reservationRepository.findAll();

        assertAll(
                () -> assertEquals(1, reservations.size()),
                () -> verify(reservationRepository, times(1)).saveAndFlush(any(ReservationEntity.class))
        );
    }

    @AfterEach
    void after(CapturedOutput output) {
        assertThat(output.getErr()).contains("NoAvailabilityException");
    }
    private ReservationRequest createReservationRequest(LocalDate checkinDate, LocalDate checkout) {
        ReservationRequest re1 = new ReservationRequest();
        re1.setFirstName("Nat");
        re1.setLastName("V");
        re1.setEmail("n.v2@gm.com");
        re1.setCheckinDate(checkinDate);
        re1.setCheckoutDate(checkout);
        re1.setNumOfGuests(4);
        return re1;
    }
}