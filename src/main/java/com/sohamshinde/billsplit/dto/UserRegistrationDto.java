package com.sohamshinde.billsplit.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {

    @NotEmpty(message = "Name is required.")
    private String name;

    @NotEmpty(message = "Email is required.")
    @Email(message = "Email should be valid.")
    private String email;

    @NotEmpty(message = "Phone number is required.")
    @Length(min = 10, max = 10)
    private String phone;

    @NotEmpty(message = "Password is required")
    private String password;

    private Boolean gender;
}
