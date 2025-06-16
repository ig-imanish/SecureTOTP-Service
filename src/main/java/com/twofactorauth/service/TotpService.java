package com.twofactorauth.service;


import java.nio.ByteBuffer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    // private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // public int getCurrentCode(String secret) {
    //     return gAuth.getTotpPassword(secret);
    // }


     public static int generateTOTP(String base32Secret, long timeMillis) throws Exception {
        // Decode Base32 secret
        byte[] key = decodeBase32(base32Secret.toUpperCase());
        
        // TOTP uses 30-second intervals
        long timeStep = 30;
        long timeCounter = timeMillis / (timeStep * 1000);

        // Convert time counter to bytes
        byte[] data = ByteBuffer.allocate(8).putLong(timeCounter).array();

        // Generate HMAC-SHA1 hash
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA1");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data);

        // Dynamic truncation
        int offset = hash[hash.length - 1] & 0xF;
        long truncatedHash = ((hash[offset] & 0x7F) << 24) |
                            ((hash[offset + 1] & 0xFF) << 16) |
                            ((hash[offset + 2] & 0xFF) << 8) |
                            (hash[offset + 3] & 0xFF);

        // Get 6-digit code
        int code = (int) (truncatedHash % 1000000);
        return code;
    }

    // Simple Base32 decoder (handles A-Z, 2-7)
    private static byte[] decodeBase32(String base32) {
        String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        byte[] bytes = new byte[base32.length() * 5 / 8]; // Base32: 8 chars = 5 bytes
        int buffer = 0;
        int bitsLeft = 0;
        int byteIndex = 0;

        for (char c : base32.toUpperCase().toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("Invalid Base32 character: " + c);
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[byteIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
