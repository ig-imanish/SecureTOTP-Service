package com.twofactorauth.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class Account {
    private String nickname;
    private String issuer;
    private String secret;
    private String logoUrl;

    public Account() {}

    public Account( String issuer, String secret , String nickname, String logoUrl) {
        this.issuer = issuer;
        this.secret = secret;
        this.nickname = nickname;
        this.logoUrl = logoUrl;
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

}
