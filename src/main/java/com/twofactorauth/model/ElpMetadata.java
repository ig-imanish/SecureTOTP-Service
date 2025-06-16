package com.twofactorauth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Data
@Document(collection = "elp_metadata")
public class ElpMetadata {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String elpId;
    
    private String userId;
    private String username;
    private String email;
    
    // Default constructor required by MongoDB
    public ElpMetadata() {}

    public ElpMetadata(String elpId, String userId, String username, String email) {
        this.elpId = elpId;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public String getElpId() {
        return elpId;
    }
    
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
