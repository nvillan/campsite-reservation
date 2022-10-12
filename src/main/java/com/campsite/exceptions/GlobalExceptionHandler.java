package com.campsite.exceptions;


import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidParameterException(InvalidParameterException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(NoAvailabilityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNoAvailabilityException(NoAvailabilityException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
        return errors;
    }

    @ResponseBody
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDateTimeParseException(DateTimeParseException ex) {
        return new StringBuffer("Please use format YYYY-MM-DD for date parameter.\n").append(ex.getLocalizedMessage()).toString();
    }
//
//    @ResponseBody
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus (HttpStatus.BAD_REQUEST)
//    public String handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){
//        return ex.getMessage();
//    }
}