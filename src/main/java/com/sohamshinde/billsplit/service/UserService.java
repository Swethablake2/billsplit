package com.sohamshinde.billsplit.service;

import com.sohamshinde.billsplit.dto.UserUpdateDto;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.entity.VerificationToken;
import com.sohamshinde.billsplit.dto.UserRegistrationDto;
import com.sohamshinde.billsplit.enums.Role;
import com.sohamshinde.billsplit.exceptions.InvalidTokenException;
import com.sohamshinde.billsplit.exceptions.ResourceNotFoundException;
import com.sohamshinde.billsplit.exceptions.UserAlreadyExistsException;
import com.sohamshinde.billsplit.exceptions.UserNotFoundException;
import com.sohamshinde.billsplit.repository.UserRepository;
import com.sohamshinde.billsplit.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.exceptions.MailerSendException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${mailerSend.apiKey}")
    private String mailersendAPIKey;

    public UserService(UserRepository userRepository, VerificationTokenRepository verificationTokenRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Method to register a user
    public void registerUser(UserRegistrationDto userRegistrationDto) {
        // Check if user already exists
        if (userRepository.findByEmail(userRegistrationDto.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        // Create new user and set properties
        User user = new User();
        user.setName(userRegistrationDto.getName());
        user.setEmail(userRegistrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword())); // Hash password
        user.setGender(userRegistrationDto.getGender());
        user.setPhone(userRegistrationDto.getPhone());
        user.setRole(Role.USER);
        user.setEnabled(false);
        user.setActive(true);

        System.out.println(1);
        userRepository.save(user); // Save user to the database
        System.out.println(2);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));

        sendEmail(userRegistrationDto.getEmail(), userRegistrationDto.getName(), "localhost:8080", token);
        System.out.println(3);

        verificationTokenRepository.save(verificationToken); // Save token to the database
        System.out.println(4);

        System.out.println(verificationToken.getToken());
        System.out.println(5);

    }


    // Method to verify the email with token
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);

        // Handle invalid token
        if (verificationToken == null) {
            throw new InvalidTokenException("Invalid verification token");
        }

        // Check if token is expired
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token has expired");
        }

        // Ensure the token is linked to a valid user
        User user = verificationToken.getUser();
        if (user == null) {
            throw new InvalidTokenException("Associated user not found for the token.");
        }

        // Enable user and save
        user.setEnabled(true);
        userRepository.save(user);

        // Delete the verification token after successful verification
        verificationTokenRepository.delete(verificationToken);
    }

    public User fetchUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + id + " not found"));
    }

    public List<User> fetchUsersByIds(List<Long> userIds) {
        return userIds.stream().map(userId -> userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found"))).collect(Collectors.toList());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public User getAuthenticatedUser() throws UserNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new ResourceNotFoundException("User not found");
        }

        return (User) authentication.getPrincipal();
    }

    // Update User Information
    public void updateUser(Long userId, UserUpdateDto userUpdateDto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update only allowed fields
        user.setName(userUpdateDto.getName());
        user.setPhone(userUpdateDto.getPhone());
        user.setGender(userUpdateDto.getGender());

        userRepository.save(user);
    }

    // Soft Delete User (Deactivate Account)
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Soft delete (instead of actual deletion)
        user.setActive(false);
        userRepository.save(user);
    }

    private void sendEmail(String emailId, String name, String host, String token) {

        String url = host + "/api/user/verify?token=" + token;

        Email email = new Email();

        email.setFrom("Bill Split", "verify@sohamshinde.info");
        email.addRecipient(name, emailId);

        email.setSubject("Verify your Bill Split account");

        String html = "<div class=\"container\">\n" +
                "        <div class=\"header\">Verify Your Email</div>\n" +
                "        <p class=\"message\">Hello,</p>\n" +
                "        <p class=\"message\">\n" +
                "            Thank you for registering. To complete your sign-up, please verify your email address by clicking the button below:\n" +
                "        </p>\n" +
                "        <a class=\"verify-button\" href=\"" + url + "\" target=\"_blank\">Verify Email</a>\n" +
                "        <p class=\"message\">\n" +
                "           If the above button doens't work for you copy paste this link in browser \n" + url +
                "        </p>\n" +
                "        <p class=\"message\">\n" +
                "            If you didn't request this email, please ignore it.\n" +
                "        </p>\n" +
                "        <div class=\"footer\">\n" +
                "            &copy; 2025 Bill Split | All rights reserved.\n" +
                "        </div>\n" +
                "    </div>";

//        System.out.println(html);

        email.setHtml(html);

        MailerSend ms = new MailerSend();

        ms.setToken(mailersendAPIKey);

//        System.out.println("mailersendAPIKey" + mailersendAPIKey);
        try {
            MailerSendResponse response = ms.emails().send(email);
//            System.out.println(response.messageId);
        } catch (MailerSendException e) {
            e.printStackTrace();
        }
    }

}
