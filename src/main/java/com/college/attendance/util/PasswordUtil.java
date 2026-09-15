package com.college.attendance.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility for hashing and verifying passwords using BCrypt.
 *
 * <p>
 * Uses a work factor of 12 by default, which provides strong resistance
 * against brute-force attacks while maintaining sub-second latency for desktop
 * interactive login.
 * </p>
 *
 * <p>
 * <b>Security:</b> Raw passwords are never logged, stored in plain text,
 * or exposed in exceptions.
 * </p>
 */
public final class PasswordUtil {

    /** Default BCrypt logarithmic work factor (rounds = 2^12 = 4096). */
    private static final int BCRYPT_LOG_ROUNDS = 12;

    private PasswordUtil() {
        // prevent instantiation
    }

    /**
     * Hashes a plain-text password using BCrypt with a secure, random salt.
     *
     * @param plainPassword the plain-text password to hash
     * @return the resulting 60-character BCrypt hash string
     * @throws IllegalArgumentException if the password is null or blank
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        String salt = BCrypt.gensalt(BCRYPT_LOG_ROUNDS);
        return BCrypt.hashpw(plainPassword, salt);
    }

    /**
     * Verifies whether a candidate plain-text password matches a stored BCrypt
     * hash.
     *
     * @param plainPassword  the candidate password to test
     * @param hashedPassword the stored BCrypt hash to verify against
     * @return {@code true} if the password matches the hash; {@code false}
     *         otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty() || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Malformed hash or invalid salt
            return false;
        }
    }
}
