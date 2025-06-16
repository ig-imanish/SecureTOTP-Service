package com.twofactorauth.controller;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.twofactorauth.model.UserTotp;
import com.twofactorauth.repo.UserTotpRepository;
import com.twofactorauth.service.ElpService;
import com.twofactorauth.service.JwtService;
import com.twofactorauth.service.UserFetchService;

@RestController
@RequestMapping("/api/v1/elp")
public class ElpController {

    @Autowired
    private ElpService elpService;

    @Autowired
    private UserTotpRepository userTotpRepository;
    
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserFetchService userFetchService;
    
    // Constructor with logging
    public ElpController() {
        System.out.println("===============================================");
        System.out.println("ElpController initialized");
        System.out.println("===============================================");
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateElp(@RequestHeader("Authorization") String authHeader) {
        System.out.println("POST /api/v1/elp/generate - Received request");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Invalid authorization header format");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token format"));
        }
        
        String token = authHeader.substring(7); // Remove "Bearer "
        String username = jwtService.extractUsername(token);
        
        if (username == null) {
            System.out.println("Invalid token provided for ELP generation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        System.out.println("Looking up user: " + username);
        UserTotp user = userTotpRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found in repository: " + username);
                    return new RuntimeException("User not found");
                });
        
        System.out.println("Found user in repository. User ID: " + user.getId());
        
        System.out.println("Fetching additional user details from auth service");
        Map<String, Object> userData = userFetchService.fetchUserByUsername(username);
        
        if (userData == null) {
            System.out.println("Failed to fetch user details from auth service");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch user details"));
        }
        
        String email = userFetchService.getEmail(userData);
        System.out.println("User details fetched. Email: " + email);

        try {
            System.out.println("Generating ELP file for user: " + username + " (ID: " + user.getId() + ")");
            ElpService.ELPResult elpResult = elpService.generateElpFile(user.getId(), username, email);
            byte[] elpFile = elpResult.getFileContent();
            String filename = elpResult.getFilename();
            
            System.out.println("ELP file generated successfully. Size: " + elpFile.length + " bytes");
            System.out.println("Using filename: " + filename);
            
            // Encode the file content as Base64
            String encodedContent = Base64.getEncoder().encodeToString(elpFile);
            
            // Return both the filename and the file content as JSON
            Map<String, Object> responseBody = Map.of(
                "filename", filename,
                "fileContent", encodedContent
            );
            
            System.out.println("Sending ELP file data and filename in JSON response");
            return ResponseEntity.ok(responseBody);
        } catch (GeneralSecurityException | IOException e) {
            System.out.println("Error generating ELP file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate ELP file: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> uploadElp(@RequestParam("file") MultipartFile file) {
        System.out.println("POST /api/v1/elp/login - Received ELP file upload");
        
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".elp")) {
            System.out.println("Invalid ELP file: " + 
                (file.isEmpty() ? "File is empty" : "Filename doesn't end with .elp"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid ELP file"));
        }
        
        // Updated to handle the new simple userId.elp filename format
        String originalFilename = file.getOriginalFilename();
        System.out.println("Uploaded filename: " + originalFilename);
        
        String userId = originalFilename.replace(".elp", "");

        System.out.println("Extracted user ID from filename: " + userId);

        try {
            System.out.println("Validating ELP file for user ID: " + userId);
            boolean isValid = elpService.validateElpFile(file, userId);

            if (isValid) {
                System.out.println("ELP file validation successful for user ID: " + userId);

                // Find user by ID to return user details
                UserTotp user = userTotpRepository.findById(userId).orElse(null);
                
                if (user != null) {
                    String username = user.getUsername();
                    System.out.println("Found user in repository: " + username);
                    
                    // Generate JWT token for the authenticated user
                    System.out.println("Generating JWT token for user: " + username);
                    String jwtToken = jwtService.generateJwtToken(username);
                    
                    if (jwtToken != null) {
                        System.out.println("JWT token generated successfully");
                        // Return success with token and user info
                        return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "ELP authentication successful",
                            "token", jwtToken,
                            "username", username,
                            "userId", userId
                        ));
                    } else {
                        System.out.println("Failed to generate JWT token");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of(
                                "success", false,
                                "error", "Authentication successful but token generation failed"
                            ));
                    }
                } else {
                    System.out.println("User not found in local DB for ID: " + userId);
                    // If user not found in local DB, we can't get the username to generate a token
                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .body(Map.of(
                            "success", true,
                            "message", "ELP file is valid but user not found in database",
                            "userId", userId
                        ));
                }
            } else {
                System.out.println("ELP file validation failed for user ID: " + userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "error", "Invalid ELP file"
                    ));
            }
        } catch (GeneralSecurityException | IOException e) {
            System.out.println("Error processing ELP file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "Error processing ELP file: " + e.getMessage()
                    ));
        }
    }

    // @PostMapping("/login")
    // public ResponseEntity<String> loginWithElp(@RequestParam("file") MultipartFile file) {
    //     System.out.println("POST /api/v1/elp/login - Received login attempt with ELP file");
        
    //     if (file.isEmpty() || !file.getOriginalFilename().endsWith(".elp")) {
    //         System.out.println("Invalid ELP file: " + 
    //             (file.isEmpty() ? "File is empty" : "Filename doesn't end with .elp"));
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ELP file");
    //     }
        
    //     // Updated to handle the new simple userId.elp filename format
    //     String originalFilename = file.getOriginalFilename();
    //     System.out.println("Uploaded filename: " + originalFilename);
        
    //     String userId = originalFilename.replace(".elp", "");
    //     if (originalFilename.startsWith("emergency_login_protocol_")) {
    //         // Handle the old format for backward compatibility
    //         userId = originalFilename.replace(".elp", "").replace("emergency_login_protocol_", "");
    //     }
        
    //     System.out.println("Extracted user ID from filename: " + userId);
        
    //     try {
    //         System.out.println("Validating ELP file for login, user ID: " + userId);
    //         boolean isValid = elpService.validateElpFile(file, userId);
            
    //         if (isValid) {
    //             System.out.println("Login successful with ELP for user ID: " + userId);
    //             return ResponseEntity.ok("Login successful with ELP");
    //         } else {
    //             System.out.println("Login failed - Invalid ELP file for user ID: " + userId);
    //             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ELP file");
    //         }
    //     } catch (GeneralSecurityException | IOException e) {
    //         System.out.println("Authentication failed: " + e.getMessage());
    //         e.printStackTrace();
    //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
    //                 .body("Authentication failed: " + e.getMessage());
    //     }
    // }

    // private String extractUsernameFromToken(String authHeader) {
    //     System.out.println("Extracting username from authorization header");
    //     if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    //         System.out.println("Invalid authorization header format");
    //         return null;
    //     }
    //     String token = authHeader.substring(7); // Remove "Bearer "
    //     System.out.println("Extracted token, calling JWT service to validate");
    //     String username = jwtService.extractUsername(token);
    //     System.out.println("Username extracted from token: " + 
    //         (username != null ? username : "null (invalid token)"));
    //     return username;
    // }

    static class PinRequest {
        private String pin;

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }
    }
}
