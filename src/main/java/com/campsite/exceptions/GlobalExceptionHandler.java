package com.campsite.exceptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.StaleStateException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    private static final Logger logger  = LogManager.getLogger(GlobalExceptionHandler.class);
    @ResponseBody
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.error(ex.getMessage());
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidParameterException(InvalidParameterException ex) {
        logger.error(ex.getMessage());
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(NoAvailabilityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNoAvailabilityException(NoAvailabilityException ex){
        logger.error(ex.getMessage());
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
        logger.error(ex.getMessage());
        return errors;
    }

    @ResponseBody
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDateTimeParseException(DateTimeParseException ex) {
        String msg = new StringBuffer("Please use format YYYY-MM-DD for date parameter.\n").append(ex.getLocalizedMessage()).toString();
        logger.error(msg);
        return msg;
    }
    @ResponseBody
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, StaleStateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleObjectOptimisticLockingFailureException (ObjectOptimisticLockingFailureException ex) {
        String msg = new StringBuffer("The reservation has already been updated in a concurrent transaction. Please try again.").toString();
        logger.error(msg);
        return msg;
    }
}