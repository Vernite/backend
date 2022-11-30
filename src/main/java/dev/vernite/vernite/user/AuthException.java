package dev.vernite.vernite.user;

public class AuthException extends RuntimeException {
    
    public AuthException() {
        super("user not logged");
    }

}
