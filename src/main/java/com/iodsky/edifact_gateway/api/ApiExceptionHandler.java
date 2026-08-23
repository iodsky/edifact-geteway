package com.iodsky.edifact_gateway.api;

import com.iodsky.edifact_gateway.edifact.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ErrorDto> handleParseException(ParseException ex) {
        ErrorDto errorDto = new ErrorDto(
                400,
                ex.getCode(),
                ex.getMessage(),
                ex.getSegmentIndex(),
                ex.getElementIndex()
        );

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

}
