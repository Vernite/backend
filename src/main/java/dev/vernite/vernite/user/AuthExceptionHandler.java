package dev.vernite.vernite.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@ControllerAdvice
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthExceptionHandler {

    /**
     * 
     * @param ex
     * @return no user logged in
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static @ResponseBody String handleException(AuthException ex) {
        return ex.getMessage();
    }

}
