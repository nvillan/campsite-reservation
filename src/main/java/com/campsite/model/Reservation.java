package com.campsite.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
    @Setter
    @Getter
    private String firstName;
    @Setter
    @Getter
    private String lastName;
    @Setter
    @Getter
    private String email;
    @Setter
    @Getter
    private LocalDate checkinDate;
    @Setter
    @Getter
    private LocalDate checkoutDate;
    @Getter
    @Setter
    private Status status;
    @Setter
    @Getter
    private String externalIdentifier;
    @Getter
    @Setter
    private int numOfGuests;

}