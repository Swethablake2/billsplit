package com.sohamshinde.billsplit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDetailDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Boolean gender;
}
