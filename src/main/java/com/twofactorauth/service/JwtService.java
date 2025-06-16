package com.twofactorauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JwtService {
    
    @Value("${admin.token}")
    private String adminToken;
    
    @Value("${api.auth.validateToken}")
    private String validateTokenUrl;
    
    @Value("${api.admin.generateJwtToken}")
    private String generateJwtTokenUrl;
    
    /**
     * Extract username from JWT token
     * @param token JWT token string
     * @return username extracted from token
     */
    public String extractUsername(String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                validateTokenUrl,
                HttpMethod.POST,
                requestEntity,
                TokenResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Token valid for user: " + response.getBody().getEmail());
                return response.getBody().getEmail();
            } else {
                System.out.println("Invalid token");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate a JWT token for a user using admin privileges
     * @param emailOrUsername The email or username of the user
     * @return Generated JWT token string or null if generation fails
     */
    public String generateJwtToken(String emailOrUsername) {
        System.out.println("Generating JWT token for user: " + emailOrUsername);
        
        try {
            // Construct the URL with path variable
            String url = generateJwtTokenUrl + "/" + emailOrUsername;
            
            System.out.println("Calling admin API: " + url);
            
            // Set up headers with admin token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + adminToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Make the request expecting a string response
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String token = response.getBody();
                System.out.println("JWT token generated successfully");
                return token;
            } else {
                System.out.println("Failed to generate token. Status: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error generating JWT token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

class TokenResponse {
    private String email;
    private HttpStatus httpStatus;

    public String getEmail() {
        return email;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
