package com.sohamshinde.billsplit.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupDto {

    @NotEmpty(message = "Group name mising")
    private String groupName;

    @NotEmpty(message = "Specify member ID's in the form [UserID1, UserID2, ...]")
    private List<Long> memberIds;
}
