package com.sohamshinde.billsplit.dto;

import com.sohamshinde.billsplit.enums.Currency;
import com.sohamshinde.billsplit.enums.ExpenseCategory;
import com.sohamshinde.billsplit.enums.ExpenseStatus;
import com.sohamshinde.billsplit.enums.SplitType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ExpenseDto {

    @NotNull(message = "Amount cannot be null")
    @PositiveOrZero(message = "Amount cannot be negative")
    BigDecimal amount;

    @NotNull(message = "Currency must be provided")
    @Enumerated(EnumType.STRING)
    Currency currency;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description;

    @NotNull(message = "Payer ID cannot be null")
    Long payerId;

    List<Long> participantIds; // Optional if using group-based sharing

    Long groupId; // Optional for group-based expenses

    @NotNull(message = "Split type must be provided")
    @Enumerated(EnumType.STRING)
    SplitType splitType; // EQUAL, EXACT, or PERCENTAGE

    Map<Long, BigDecimal> participantShares; // Map of participant IDs to exact amounts or percentages

    @NotNull(message = "Expense status must be provided")
    @Enumerated(EnumType.STRING)
    ExpenseStatus status;

    @NotNull(message = "Expense category must be provided")
    @Enumerated(EnumType.STRING)
    ExpenseCategory category;
}
