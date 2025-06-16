package com.twofactorauth.controller;

import com.twofactorauth.model.Account;
import com.twofactorauth.model.UserTotp;
import com.twofactorauth.repo.UserTotpRepository;
import com.twofactorauth.service.EncryptionService;
import com.twofactorauth.service.JwtService;
import com.twofactorauth.service.TotpService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class AccountController {

    @Autowired
    private UserTotpRepository userTotpRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private EncryptionService encryptionService;

    @PostMapping("/add-account")
    public ResponseEntity<?> addAccount(
            @RequestBody AccountRequest accountRequest,
            @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        // Sanitize secret (remove spaces and convert to uppercase)
        String secret = accountRequest.getSecret().replace(" ", "").toUpperCase();
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        UserTotp userTotp;
        
        if (userOptional.isEmpty()) {
            // Create new user if not exists
            userTotp = new UserTotp();
            userTotp.setUsername(username);
            userTotp.setAccounts(new ArrayList<>());
        } else {
            userTotp = userOptional.get();
            if (userTotp.getAccounts() == null) {
                userTotp.setAccounts(new ArrayList<>());
            }
        }
        
        // Create new account
        Account account = new Account();
        account.setNickname(accountRequest.getNickname());
        account.setIssuer(accountRequest.getIssuer());
        
        // Encrypt the secret before storing
        if (!secret.isEmpty()) {
            account.setSecret(encryptionService.encrypt(secret));
        } else {
            account.setSecret("");
        }
        
        account.setLogoUrl(secret.isEmpty() ? null : accountRequest.getLogoUrl());

        System.out.println("Adding account: " + account.getIssuer() + " (" + account.getNickname() + ")");
        
        userTotp.getAccounts().add(account);
        userTotpRepository.save(userTotp);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true, 
            "message", "Account added successfully"
        ));
    }
    
    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty() || userOptional.get().getAccounts() == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Account account : userOptional.get().getAccounts()) {
            try {
                // Decrypt the secret before use
                String decryptedSecret = encryptionService.decrypt(account.getSecret());
                int code = TotpService.generateTOTP(decryptedSecret, System.currentTimeMillis());
                
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("nickname", account.getNickname());
                accountData.put("issuer", account.getIssuer());
                accountData.put("code", String.format("%06d", code));
                
                result.add(accountData);
            } catch (Exception e) {
                // Skip accounts with invalid secrets
                System.out.println("Error processing account: " + e.getMessage());
                continue;
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/account/{issuer}/{nickname}")
    public ResponseEntity<?> removeAccount(
            @PathVariable String issuer,
            @PathVariable String nickname,
            @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty() || userOptional.get().getAccounts() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No accounts found"));
        }
        
        UserTotp user = userOptional.get();
        List<Account> accounts = user.getAccounts();

        // Use removeIf which is safe for concurrent modification
        // Now checking both issuer and nickname for more precise deletion
        accounts.removeIf(account -> 
            account.getNickname().equals(nickname) && 
            account.getIssuer().equals(issuer)
        );
        userTotpRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Account removed successfully"
        ));
    }
    
    @GetMapping("/current-codes")
    public ResponseEntity<?> getCurrentCodes(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        
        Optional<UserTotp> userOptional = userTotpRepository.findByUsername(username);
        if (userOptional.isEmpty() || userOptional.get().getAccounts() == null || userOptional.get().getAccounts().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No accounts found"));
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Account account : userOptional.get().getAccounts()) {
            try {
                // Decrypt the secret before generating code
                String decryptedSecret = encryptionService.decrypt(account.getSecret());
                int code = TotpService.generateTOTP(decryptedSecret, System.currentTimeMillis());
                
                Map<String, Object> codeData = new HashMap<>();
                codeData.put("key", account.getIssuer() + " (" + account.getNickname() + ")");
                codeData.put("code", String.format("%06d", code));
                codeData.put("issuer", account.getIssuer());
                codeData.put("nickname", account.getNickname());
                codeData.put("logoUrl", account.getLogoUrl());
                
                result.add(codeData);
                
            } catch (Exception e) {
                // Skip accounts with invalid secrets
                System.out.println("Error generating code: " + e.getMessage());
                continue;
            }
        }
        System.out.println(result.size() + " accounts found for user: " + username);
        
        return ResponseEntity.ok(result);
    }
    
    private String extractUsernameFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7); // Remove "Bearer "
        return jwtService.extractUsername(token);
    }
    
    static class AccountRequest {
        private String nickname;
        private String issuer;
        private String secret;
        private String logoUrl;
        
        
        public String getNickname() {
            return nickname;
        }
        
        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
        
        public String getIssuer() {
            return issuer;
        }
        
        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
        
        public String getSecret() {
            return secret;
        }
        
        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        @Override
        public String toString() {
            return "AccountRequest [nickname=" + nickname + ", issuer=" + issuer + ", secret=" + secret + ", logoUrl="
                    + logoUrl + "]";
        }
    }
}
