package dev.vernite.vernite.utils;

import java.security.SecureRandom;

public final class SecureStringUtils {
    private static final char[] CHARS = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateRandomSecureString() {
        char[] b = new char[128];
        for (int i = 0; i < b.length; i++) {
            b[i] = CHARS[SECURE_RANDOM.nextInt(CHARS.length)];
        }
        return new String(b);
    }

    private SecureStringUtils() {
    }
}
