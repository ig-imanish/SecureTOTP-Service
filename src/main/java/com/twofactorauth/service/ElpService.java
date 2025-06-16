package com.twofactorauth.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twofactorauth.model.ElpMetadata;
import com.twofactorauth.repo.ElpMetadataRepository;

@Service
public class ElpService {

    @Value("${elp.encryption.key")
    private String encryptionKey;

    @Value("${elp.hmac.key")
    private String hmacKey;

    private static final String AES_ALGORITHM = "AES";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // Replace in-memory storage with MongoDB repository
    @Autowired
    private ElpMetadataRepository elpMetadataRepository;
    
    // Constructor to log key information
    public ElpService() {
        System.out.println("===============================================");
        System.out.println("ElpService initialized");
        System.out.println("===============================================");
    }

    /**
     * Generate ELP file
     * @param userId User ID
     * @param username Username
     * @param email Email
     * @return ELPResult containing file content and filename
     */
    public ELPResult generateElpFile(String userId, String username, String email) 
            throws GeneralSecurityException, IOException {
        System.out.println("Generating ELP file for user ID: " + userId);
        
        // Generate unique ELP ID
        String elpId = generateUniqueId();
        System.out.println("Generated ELP ID: " + elpId);

        // Create ELP data
        ElpData elpData = new ElpData(userId, username, email, elpId);
        String elpJson = new ObjectMapper().writeValueAsString(elpData);
        System.out.println("Created ELP data JSON");

        // Encrypt the ELP data
        byte[] encryptedData = encrypt(elpJson.getBytes(StandardCharsets.UTF_8));
        System.out.println("Encrypted ELP data: " + encryptedData.length + " bytes");

        // Generate HMAC for integrity
        byte[] hmac = generateHmac(elpJson);
        System.out.println("Generated HMAC: " + hmac.length + " bytes");

        // Combine encrypted data and HMAC
        byte[] elpFileContent = new byte[encryptedData.length + hmac.length];
        System.arraycopy(encryptedData, 0, elpFileContent, 0, encryptedData.length);
        System.arraycopy(hmac, 0, elpFileContent, encryptedData.length, hmac.length);
        System.out.println("Combined ELP file content: " + elpFileContent.length + " bytes");

        // Store metadata in MongoDB instead of the local map
        ElpMetadata metadata = new ElpMetadata(elpId, userId, username, email);
        elpMetadataRepository.save(metadata);
        System.out.println("Stored metadata for ELP ID: " + elpId + " in MongoDB");

        // Create filename - just userId.elp as requested
        String filename = userId + ".elp";
        System.out.println("Created filename: " + filename);

        return new ELPResult(elpFileContent, filename);
    }

    /**
     * Class to hold ELP generation result
     * Contains both file content and filename
     */
    public static class ELPResult {
        private final byte[] fileContent;
        private final String filename;
        
        public ELPResult(byte[] fileContent, String filename) {
            this.fileContent = fileContent;
            this.filename = filename;
        }
        
        public byte[] getFileContent() {
            return fileContent;
        }
        
        public String getFilename() {
            return filename;
        }
    }

    public boolean validateElpFile(MultipartFile file, String userId) throws GeneralSecurityException, IOException {
        System.out.println("Validating ELP file for user ID: " + userId);
        
        byte[] fileContent = file.getBytes();
        if (fileContent.length < 32) { // Minimum HMAC size
            System.out.println("Invalid file: too small");
            return false;
        }

        // Split encrypted data and HMAC
        byte[] encryptedData = new byte[fileContent.length - 32];
        byte[] receivedHmac = new byte[32];
        System.arraycopy(fileContent, 0, encryptedData, 0, encryptedData.length);
        System.arraycopy(fileContent, encryptedData.length, receivedHmac, 0, 32);
        System.out.println("Split file into encrypted data and HMAC");

        // Decrypt the data
        byte[] decryptedData;
        try {
            decryptedData = decrypt(encryptedData);
            System.out.println("Successfully decrypted data");
        } catch (Exception e) {
            System.out.println("Failed to decrypt data: " + e.getMessage());
            return false;
        }
        
        String elpJson = new String(decryptedData, StandardCharsets.UTF_8);

        // Verify HMAC
        byte[] calculatedHmac = generateHmac(elpJson);
        if (!MessageDigest.isEqual(receivedHmac, calculatedHmac)) {
            System.out.println("HMAC verification failed");
            return false;
        }
        System.out.println("HMAC verification successful");

        // Parse ELP data
        ElpData elpData;
        try {
            elpData = new ObjectMapper().readValue(elpJson, ElpData.class);
            System.out.println("Parsed ELP data for ELP ID: " + elpData.getElpId());
        } catch (Exception e) {
            System.out.println("Failed to parse ELP data: " + e.getMessage());
            return false;
        }

        // Validate metadata - now using MongoDB repository
        ElpMetadata storedMetadata = elpMetadataRepository.findByElpId(elpData.getElpId());
        if (storedMetadata == null) {
            System.out.println("No stored metadata found for ELP ID: " + elpData.getElpId());
            return false;
        }
        
        if (!storedMetadata.getUserId().equals(userId) ||
                !storedMetadata.getUsername().equals(elpData.getUsername()) ||
                !storedMetadata.getEmail().equals(elpData.getEmail())) {
            System.out.println("Metadata validation failed");
            return false;
        }
        System.out.println("Metadata validation successful");

        return true;
    }

    private byte[] encrypt(byte[] data) throws GeneralSecurityException {
        // Use derived key instead of trying to decode as Base64
        byte[] keyBytes = deriveKey(encryptionKey);
        System.out.println("Using " + keyBytes.length + "-byte key for encryption");
        
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data) throws GeneralSecurityException {
        // Use derived key instead of trying to decode as Base64
        byte[] keyBytes = deriveKey(encryptionKey);
        System.out.println("Using " + keyBytes.length + "-byte key for decryption");
        
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    private byte[] generateHmac(String data) throws GeneralSecurityException {
        // Use derived key instead of trying to decode as Base64
        byte[] keyBytes = deriveKey(hmacKey);
        System.out.println("Using " + keyBytes.length + "-byte key for HMAC");
        
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(keySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Derives a cryptographic key from a string
     * This avoids Base64 decoding issues by always creating a valid key
     */
    private byte[] deriveKey(String input) {
        try {
            // Check if it's valid Base64 first
            try {
                byte[] decoded = Base64.getDecoder().decode(input);
                // If we get here, it was valid Base64
                // Make sure it's the right length for AES (16, 24, or 32 bytes)
                if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                    System.out.println("Using provided Base64 key");
                    return decoded;
                }
            } catch (IllegalArgumentException e) {
                // Not valid Base64, will use key derivation below
                System.out.println("Invalid Base64 key, using key derivation");
            }
            
            // Use SHA-256 to derive a 32-byte key (256 bits) suitable for AES-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("Error deriving key: " + e.getMessage());
            // Fallback to a secure random key if all else fails
            byte[] key = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(key);
            System.out.println("Using fallback random key");
            return key;
        }
    }

    private String generateUniqueId() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Inner classes for data handling
    private static class ElpData {
        // Existing implementation...
        private String userId;
        private String username;
        private String email;
        private String elpId;

        public ElpData() {}

        public ElpData(String userId, String username, String email, String elpId) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.elpId = elpId;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getElpId() { return elpId; }
        public void setElpId(String elpId) { this.elpId = elpId; }
    }
}
