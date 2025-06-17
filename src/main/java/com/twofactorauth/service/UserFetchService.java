package com.twofactorauth.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserFetchService {

    @Value("${auth.server.url")
    private String authServerUrl;

    /**
     * Fetch user details by email
     * @param email the email of the user to fetch
     * @return Map containing user data if found, null otherwise
     */
    public Map<String, Object> fetchUserByEmailOrUsername(String email) {
        System.out.println("Fetching user by email or username: " + email);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = authServerUrl + "/api/v1/auth/byEmailUsername/" + email;
            
            System.out.println("Calling API URL: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Use exchange method with Map.class
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = response.getBody();
                System.out.println("User found by email: " + email);
                return userData;
            } else {
                System.out.println("User not found with email: " + email);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error fetching user by email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetch user details by username
     * @param username the username of the user to fetch
     * @return Map containing user data if found, null otherwise
     */
    public Map<String, Object> fetchUserByUsername(String username) {
        System.out.println("Fetching user by username: " + username);
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = authServerUrl + "/api/v1/auth/byEmailUsername/" + username;
            
            System.out.println("Calling API URL: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Use exchange method with Map.class
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = response.getBody();
                System.out.println("User found by username: " + username);
                return userData;
            } else {
                System.out.println("User not found with username: " + username);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error fetching user by username: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Fetch user by token - similar to JwtService but returns complete user data
     * @param token JWT token
     * @return Map containing user data if token valid, null otherwise
     */
    public Map<String, Object> fetchUserByToken(String token) {
        System.out.println("Fetching user by token");
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = authServerUrl + "/api/v1/auth/validateToken";
            
            System.out.println("Calling API URL: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Use exchange method with Map.class
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = response.getBody();
                System.out.println("User found by token");
                return userData;
            } else {
                System.out.println("Invalid token or user not found");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error fetching user by token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Helper method to get user ID
     */
    public String getUserId(Map<String, Object> userData) {
        if (userData != null && userData.containsKey("id")) {
            return String.valueOf(userData.get("id"));
        }
        return null;
    }
    
    /**
     * Helper method to get email
     */
    public String getEmail(Map<String, Object> userData) {
        if (userData != null && userData.containsKey("email")) {
            return String.valueOf(userData.get("email"));
        }
        return null;
    }
    
    /**
     * Helper method to get username
     */
    public String getUsername(Map<String, Object> userData) {
        if (userData != null && userData.containsKey("username")) {
            return String.valueOf(userData.get("username"));
        }
        return null;
    }
}

