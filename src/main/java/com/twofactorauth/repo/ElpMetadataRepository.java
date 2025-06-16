package com.twofactorauth.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.twofactorauth.model.ElpMetadata;

@Repository
public interface ElpMetadataRepository extends MongoRepository<ElpMetadata, String> {
    ElpMetadata findByElpId(String elpId);
}