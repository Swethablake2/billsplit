package com.sohamshinde.billsplit.controller;

import com.sohamshinde.billsplit.dto.ExpenseDto;
import com.sohamshinde.billsplit.exceptions.ExpenseException;
import com.sohamshinde.billsplit.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseController {

    ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<String> addExpense(@Valid @RequestBody ExpenseDto expenseDto) throws ExpenseException {
        expenseService.addExpense(expenseDto);
        return ResponseEntity.ok("Expense added successfully");
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseDto> getExpenseById(@PathVariable Long expenseId) throws ExpenseException {
        ExpenseDto expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expense);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ExpenseDto>> getExpensesByUser(@PathVariable Long userId) throws ExpenseException {
        List<ExpenseDto> expenses = expenseService.getExpensesByUser();
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ExpenseDto>> getExpensesByGroup(@PathVariable Long groupId) throws ExpenseException {
        List<ExpenseDto> expenses = expenseService.getExpensesByGroup(groupId);
        return ResponseEntity.ok(expenses);
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<String> updateExpense(@PathVariable Long expenseId, @RequestBody ExpenseDto expenseDto) throws ExpenseException {
        expenseService.updateExpense(expenseId, expenseDto);
        return ResponseEntity.ok("Expense updated successfully");
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long expenseId) throws ExpenseException {
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.ok("Expense deleted successfully");
    }
}
