package com.twofactorauth.model;

import java.time.LocalDateTime;
import java.util.Date;

public class UserDTO {

    String id;
    String fullName;
    String username;
    String email;
    String provider;
    private boolean isPremium;

    private Date accountCreatedAt;
    // Email verification
    private boolean verified;
    private String otp;
    private LocalDateTime otpGeneratedTime;

    private String userAvatar;
    private String userAvatarpublicId;

    private String userBanner;
    private String userBannerpublicId;
    public UserDTO() {
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getProvider() {
        return provider;
    }
    public void setProvider(String provider) {
        this.provider = provider;
    }
    public boolean isPremium() {
        return isPremium;
    }
    public void setPremium(boolean isPremium) {
        this.isPremium = isPremium;
    }
    public Date getAccountCreatedAt() {
        return accountCreatedAt;
    }
    public void setAccountCreatedAt(Date accountCreatedAt) {
        this.accountCreatedAt = accountCreatedAt;
    }
    public boolean isVerified() {
        return verified;
    }
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    public String getOtp() {
        return otp;
    }
    public void setOtp(String otp) {
        this.otp = otp;
    }
    public LocalDateTime getOtpGeneratedTime() {
        return otpGeneratedTime;
    }
    public void setOtpGeneratedTime(LocalDateTime otpGeneratedTime) {
        this.otpGeneratedTime = otpGeneratedTime;
    }
    public String getUserAvatar() {
        return userAvatar;
    }
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    public String getUserAvatarpublicId() {
        return userAvatarpublicId;
    }
    public void setUserAvatarpublicId(String userAvatarpublicId) {
        this.userAvatarpublicId = userAvatarpublicId;
    }
    public String getUserBanner() {
        return userBanner;
    }
    public void setUserBanner(String userBanner) {
        this.userBanner = userBanner;
    }
    public String getUserBannerpublicId() {
        return userBannerpublicId;
    }
    public void setUserBannerpublicId(String userBannerpublicId) {
        this.userBannerpublicId = userBannerpublicId;
    }
    
    
}
