package com.tekion.interview.stockTradingPlatform.repository;

import com.tekion.interview.stockTradingPlatform.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
}
