package com.sohamshinde.billsplit.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {

    @NotEmpty(message = "Name is required.")
    private String name;

    @NotEmpty(message = "Phone number is required.")
    @Length(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    @Pattern(regexp = "\\d+", message = "Phone number must contain only digits")
    private String phone;

    private Boolean gender;
}
