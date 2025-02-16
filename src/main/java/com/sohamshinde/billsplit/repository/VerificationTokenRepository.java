package com.sohamshinde.billsplit.repository;

import com.sohamshinde.billsplit.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findByToken(String token); // Method to find token
}
