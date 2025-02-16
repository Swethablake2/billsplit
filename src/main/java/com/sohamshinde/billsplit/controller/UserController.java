package com.sohamshinde.billsplit.controller;

import com.sohamshinde.billsplit.dto.UserDetailDto;
import com.sohamshinde.billsplit.dto.UserRegistrationDto;
import com.sohamshinde.billsplit.dto.UserUpdateDto;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.exceptions.ResourceNotFoundException;
import com.sohamshinde.billsplit.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Registration endpoint
    @PostMapping
    public ResponseEntity<String> registerUser(@RequestBody @Valid UserRegistrationDto userRegistrationDto) {
        System.out.println(userRegistrationDto);
        userService.registerUser(userRegistrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully. Please check your email for verification.");
    }

    // Email verification endpoint
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully.");
    }

    @GetMapping
    public ResponseEntity<UserDetailDto> getUser() {
        User authenticatedUser = userService.getAuthenticatedUser();

        UserDetailDto userDetailDto = new UserDetailDto(
                authenticatedUser.getId(),
                authenticatedUser.getName(),
                authenticatedUser.getEmail(),
                authenticatedUser.getPhone(),
                authenticatedUser.getGender()
        );

        return ResponseEntity.ok(userDetailDto);
    }

    // ✅ Update User Info (Only for Authenticated Users)
    @PutMapping
    public ResponseEntity<String> updateUser(@RequestBody @Valid UserUpdateDto userUpdateDto) {
        User authenticatedUser = userService.getAuthenticatedUser();
        userService.updateUser(authenticatedUser.getId(), userUpdateDto);
        return ResponseEntity.ok("User updated successfully.");
    }

    // ✅ Deactivate Account (Soft Delete)
    @DeleteMapping
    public ResponseEntity<String> deactivateAccount() {
        User authenticatedUser = userService.getAuthenticatedUser();
        userService.deactivateUser(authenticatedUser.getId());

        // Logout the user (invalidate session)
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("User account deactivated successfully.");
    }


}
