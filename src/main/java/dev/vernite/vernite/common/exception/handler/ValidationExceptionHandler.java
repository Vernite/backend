/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.common.exception.handler;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import dev.vernite.vernite.common.exception.error.ValidationError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

/**
 * This class contains exception handlers for exceptions thrown during java bean
 * validation.
 */
@ControllerAdvice
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationExceptionHandler {

    private static ValidationError.FieldError getFieldError(FieldError error) {
        return new ValidationError.FieldError(error.getField(), error.getDefaultMessage());
    }

    private static List<ValidationError.FieldError> getFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getFieldErrors().stream().map(ValidationExceptionHandler::getFieldError).toList();
    }

    private static ValidationError.FieldError getFieldViolation(ConstraintViolation<?> violation) {
        return new ValidationError.FieldError(violation.getPropertyPath().toString(), violation.getMessage());
    }

    private static List<ValidationError.FieldError> getFieldViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream().map(ValidationExceptionHandler::getFieldViolation).toList();
    }

    /**
     * Exception handler for {@link MethodArgumentNotValidException}.
     * Maps exception to {@link ValidationError}.
     * 
     * @param ex exception thrown during request processing
     * @return error message with all violation of constraints in received data
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public static @ResponseBody ValidationError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return new ValidationError("validation of received object failed", getFieldErrors(ex));
    }

    /**
     * Exception handler for {@link ConstraintViolationException}.
     * Maps exception to {@link ValidationError}.
     * 
     * @param ex exception thrown during entity processing
     * @return error message with all violations of constraints in entity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public static @ResponseBody ValidationError handleConstraintViolation(ConstraintViolationException ex) {
        return new ValidationError("validation of entity failed", getFieldViolations(ex));
    }

}
