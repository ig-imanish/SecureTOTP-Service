package com.twofactorauth.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "user_totp")
public class UserTotp {
    @Id
    private String id;

    private String username;
    private String pin;
    private List<Account> accounts = new ArrayList<>();
}
