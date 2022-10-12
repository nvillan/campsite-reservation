package com.campsite.persistence.db;

import com.campsite.persistence.repository.ReservationRepository;
import com.campsite.persistence.entity.ReservationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Date;
import java.time.LocalDate;

@Configuration
public class LoadDatabase {

        private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

        @Bean
        CommandLineRunner initDatabase(ReservationRepository repository) {

            return args -> {
                ReservationEntity reservation1 = new ReservationEntity();
                reservation1.setFirstName("Natalie");
                reservation1.setLastName("Villan");
                reservation1.setEmail("abc@mouse.com");
                reservation1.setCheckinDate(LocalDate.now().plusDays(1));
                reservation1.setCheckoutDate(LocalDate.now().plusDays(4));

                ReservationEntity reservation2 = new ReservationEntity();
                reservation2.setFirstName("Bob");
                reservation2.setLastName("Burg");
                reservation2.setEmail("bob@mouse.com");
                reservation2.setCheckinDate(LocalDate.now().plusDays(1));
                reservation2.setCheckoutDate(LocalDate.now().plusDays(2));

                log.info("Preloading " + repository.save(reservation1));
                log.info("Preloading " + repository.save(reservation2));
            };
        }

}
