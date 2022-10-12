package com.campsite.controller.utils;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class ReservationRequest {

    private static final int DEFAULT_NUMBER_OF_GUEST = 4;

    @NotNull(groups = OnCreate.class, message = "First name is required.")
    private String firstName;

    @NotNull(groups = OnCreate.class, message = "Last name is required.")
    private String lastName;

    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "Email is format is invalid.")
    @NotNull(groups = OnCreate.class, message = "Email is required.")
    private String email;

    @Future(groups = {OnCreate.class, OnUpdate.class}, message = "The checkin date must be in the future.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(groups = OnCreate.class, message = "Checkin date is required.")
    private LocalDate checkinDate;

    @Future(groups = {OnCreate.class, OnUpdate.class}, message = "The checkout date must be in the future.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(groups = OnCreate.class, message = "Checkout date is required.")
    private LocalDate checkoutDate;

    @PositiveOrZero(message = "The number of guests must be greater than 0.")
    private int numOfGuests;

}