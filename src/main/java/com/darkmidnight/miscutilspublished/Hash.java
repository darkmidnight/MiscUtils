package com.darkmidnight.miscutilspublished;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author Dark Midnight Studios
 */
public class Hash {
    /**
     * Simple wrapper for creating a SHA-256 Hash
     * @param b The bytes to hash
     * @return
     * @throws NoSuchAlgorithmException 
     */
    public static String createSHA256(byte[] b) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(b);
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
