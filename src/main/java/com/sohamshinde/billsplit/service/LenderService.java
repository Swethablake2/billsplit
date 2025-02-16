//package com.sohamshinde.billsplit.service;
//
//import com.sohamshinde.billsplit.entity.User;
//import com.sohamshinde.billsplit.entity.VerificationToken;
//import com.sohamshinde.billsplit.dto.UserRegistrationDto;
//import com.sohamshinde.billsplit.enums.Role;
//import com.sohamshinde.billsplit.exceptions.UserAlreadyExistsException;
//import com.sohamshinde.billsplit.exceptions.UserNotFoundException;
//import com.sohamshinde.billsplit.repository.UserRepository;
//import com.sohamshinde.billsplit.repository.VerificationTokenRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Service
//@Slf4j
//public class LenderService {
//
//    private final UserRepository userRepository;
//    private final VerificationTokenRepository verificationTokenRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    public LenderService(UserRepository userRepository,
//                         VerificationTokenRepository verificationTokenRepository,
//                         PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.verificationTokenRepository = verificationTokenRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    // ✅ Register a lender (Only ROLE_LENDER)
//    public void registerLender(UserRegistrationDto userRegistrationDto) {
//        if (userRepository.findByEmail(userRegistrationDto.getEmail()).isPresent()) {
//            throw new UserAlreadyExistsException("Lender with this email already exists.");
//        }
//
//        // Creating a new lender with ROLE_LENDER
//        User lender = new User();
//        lender.setName(userRegistrationDto.getName());
//        lender.setEmail(userRegistrationDto.getEmail());
//        lender.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
//        lender.setGender(userRegistrationDto.getGender());
//        lender.setRole(Role.LENDER);
//        lender.setEnabled(false);
//        lender.setActive(true);
//
//        userRepository.save(lender);
//
//        // ✅ Generate verification token
//        String token = UUID.randomUUID().toString();
//        VerificationToken verificationToken = new VerificationToken();
//        verificationToken.setToken(token);
//        verificationToken.setUser(lender);
//        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
//
//        verificationTokenRepository.save(verificationToken);
//
//        log.info("Lender verification token generated: {}", token);
//    }
//
//    // ✅ Verify email for lender
//    public void verifyEmailForLender(String token) {
//        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);
//        if (verificationToken == null) {
//            throw new IllegalArgumentException("Invalid verification token");
//        }
//
//        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Verification token has expired");
//        }
//
//        User lender = verificationToken.getUser();
//        lender.setEnabled(true);
//        userRepository.save(lender);
//
//        verificationTokenRepository.delete(verificationToken);
//    }
//
//    public User fetchLenderById(Long id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new UserNotFoundException("Lender with ID " + id + " not found"));
//    }
//}
