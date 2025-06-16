package com.twofactorauth.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.twofactorauth.model.UserTotp;

import java.util.Optional;

public interface UserTotpRepository extends MongoRepository<UserTotp, String> {
    Optional<UserTotp> findByUsername(String username);
}
