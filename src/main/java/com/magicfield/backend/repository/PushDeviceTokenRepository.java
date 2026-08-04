package com.magicfield.backend.repository;

import com.magicfield.backend.entity.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, UUID> {

    Optional<PushDeviceToken> findByToken(String token);
}
