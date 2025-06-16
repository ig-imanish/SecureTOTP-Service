package com.twofactorauth.controller;

import com.twofactorauth.model.UserTotp;
import com.twofactorauth.repo.UserTotpRepository;
import com.twofactorauth.service.EncryptionService;
import com.twofactorauth.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class PinController {

    @Autowired
    private UserTotpRepository userTotpRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private EncryptionService encryptionService;

    @GetMapping("/get-pin")
    public ResponseEntity<?> getPin(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsernameFromToken(authHeader);
        
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Pin not found"));
        }
        
        UserTotp user = userOptional.get();
        String encryptedPin = user.getPin();
        
        if (encryptedPin == null) {
            return ResponseEntity.ok(Map.of("pin", ""));
        }
        
        // We don't decrypt the PIN here - just check if it exists
        return ResponseEntity.ok(Map.of("pin", encryptedPin));
    }

    @PostMapping("/create-pin")
    public ResponseEntity<?> createPin(
            @RequestBody PinRequest pinRequest,
            @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromToken(authHeader);
        
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No Accounts found"));
        }
        
        UserTotp user = userOptional.get();
        
        if (user.getPin() != null && !user.getPin().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "PIN already exists. Use update-pin endpoint to change it."
            ));
        }
        
        // Encrypt the PIN before storing
        String encryptedPin = encryptionService.encrypt(pinRequest.getPin());
        user.setPin(encryptedPin);
        userTotpRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "PIN created successfully"
        ));
    }
    
    @PostMapping("/verify-pin")
    public ResponseEntity<?> verifyPin(
            @RequestBody PinRequest pinRequest,
            @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromToken(authHeader);
        
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        
        UserTotp user = userOptional.get();
        String encryptedPin = user.getPin();
        
        if (encryptedPin == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "PIN not set"
            ));
        }
        
        // Decrypt the stored PIN and compare with the provided PIN
        String storedPin = encryptionService.decrypt(encryptedPin);
        System.out.println("Comparing stored PIN with provided PIN" + storedPin + " vs " + pinRequest.getPin());
        if (storedPin == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to decrypt stored PIN"
            ));
        }
        boolean verified = storedPin.equals(pinRequest.getPin());
        
        return ResponseEntity.ok(Map.of("verified", verified));
    }
    
    @PutMapping("/update-pin")
    public ResponseEntity<?> updatePin(
            @RequestBody UpdatePinRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromToken(authHeader);
        
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        
        UserTotp user = userOptional.get();
        String encryptedPin = user.getPin();
        
        if (encryptedPin == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "No PIN exists. Use create-pin endpoint to set a PIN."
            ));
        }
        
        // Verify the old PIN
        String storedPin = encryptionService.decrypt(encryptedPin);
        if (!storedPin.equals(request.getOldPin())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Current PIN is incorrect"
            ));
        }
        
        // Update and encrypt the new PIN
        String encryptedNewPin = encryptionService.encrypt(request.getNewPin());
        user.setPin(encryptedNewPin);
        userTotpRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "PIN updated successfully"
        ));
    }
    
    private String extractUsernameFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7); // Remove "Bearer "
        return jwtService.extractUsername(token);
    }
    
    static class PinRequest {
        private String pin;

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }
    }
    
    static class UpdatePinRequest {
        private String oldPin;
        private String newPin;

        public String getOldPin() {
            return oldPin;
        }

        public void setOldPin(String oldPin) {
            this.oldPin = oldPin;
        }

        public String getNewPin() {
            return newPin;
        }

        public void setNewPin(String newPin) {
            this.newPin = newPin;
        }
    }
}
