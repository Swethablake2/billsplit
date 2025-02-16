package com.sohamshinde.billsplit.service;

import com.sohamshinde.billsplit.dto.ExpenseDto;
import com.sohamshinde.billsplit.entity.Expense;
import com.sohamshinde.billsplit.entity.Group;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.enums.Currency;
import com.sohamshinde.billsplit.enums.SplitType;
import com.sohamshinde.billsplit.exceptions.ExpenseException;
import com.sohamshinde.billsplit.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ExpenseServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User payer, participant1, participant2;
    private Group group;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock users
        payer = new User();
        payer.setId(1L);

        participant1 = new User();
        participant1.setId(2L);

        participant2 = new User();
        participant2.setId(3L);

        // Setup mock group
        group = new Group();
        group.setId(10L);
        group.setMembers(Arrays.asList(payer, participant1, participant2));

        when(userService.fetchUserById(1L)).thenReturn(payer);
        when(userService.fetchUserById(2L)).thenReturn(participant1);
        when(userService.fetchUserById(3L)).thenReturn(participant2);
        when(groupService.fetchGroupById(10L)).thenReturn(group);

        // ✅ Mock Security Context for Authentication
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(payer, null));
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * ✅ Test Equal Split
     */
    @Test
    void testAddExpenseEqualSplit() throws ExpenseException {
        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("120.00"))
                .currency(Currency.USD)
                .description("Lunch Expense")
                .payerId(1L)
                .participantIds(Arrays.asList(2L, 3L))
                .splitType(SplitType.EQUAL)
                .build();

        expenseService.addExpense(expenseDto);
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    /**
     * ✅ Test Percentage Split
     */
    @Test
    void testAddExpensePercentageSplit() throws ExpenseException {
        Map<Long, BigDecimal> participantShares = new HashMap<>();
        participantShares.put(2L, new BigDecimal("40"));
        participantShares.put(3L, new BigDecimal("60"));

        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("200.00"))
                .currency(Currency.USD)
                .description("Grocery Expense")
                .payerId(1L)
                .participantIds(Arrays.asList(2L, 3L))
                .splitType(SplitType.PERCENTAGE)
                .participantShares(participantShares)
                .build();

        expenseService.addExpense(expenseDto);
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    /**
     * ✅ Test Exact Split
     */
    @Test
    void testAddExpenseExactSplit() throws ExpenseException {
        Map<Long, BigDecimal> participantShares = new HashMap<>();
        participantShares.put(2L, new BigDecimal("50.00"));
        participantShares.put(3L, new BigDecimal("70.00"));

        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("120.00"))
                .currency(Currency.USD)
                .description("Movie Tickets")
                .payerId(1L)
                .participantIds(Arrays.asList(2L, 3L))
                .splitType(SplitType.EXACT)
                .participantShares(participantShares)
                .build();

        expenseService.addExpense(expenseDto);
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    /**
     * ✅ Test Expense for a Group
     */
    @Test
    void testAddExpenseForGroup() throws ExpenseException {
        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("300.00"))
                .currency(Currency.USD)
                .description("Dinner with Group")
                .payerId(1L)
                .groupId(10L)
                .splitType(SplitType.EQUAL)
                .build();

        expenseService.addExpense(expenseDto);
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    /**
     * ✅ Test Invalid Payer
     */
    @Test
    void testAddExpenseInvalidPayer() {
        when(userService.fetchUserById(1L)).thenReturn(null);

        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("100.00"))
                .currency(Currency.USD)
                .description("Dinner Expense")
                .payerId(1L)
                .participantIds(Arrays.asList(2L, 3L))
                .splitType(SplitType.EQUAL)
                .build();

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.addExpense(expenseDto));
        assertEquals("Payer not found", exception.getMessage());
    }

    /**
     * ✅ Test Invalid Group ID
     */
    @Test
    void testAddExpenseInvalidGroup() {
        when(groupService.fetchGroupById(99L)).thenReturn(null);

        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("250.00"))
                .currency(Currency.USD)
                .description("Office Party")
                .payerId(1L)
                .groupId(99L)
                .splitType(SplitType.EQUAL)
                .build();

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.addExpense(expenseDto));
        assertEquals("Invalid Group ID", exception.getMessage());
    }

    /**
     * ✅ Test Invalid Percentage Split (Total != 100%)
     */
    @Test
    void testAddExpenseInvalidPercentageSplit() {
        Map<Long, BigDecimal> participantShares = new HashMap<>();
        participantShares.put(2L, new BigDecimal("30"));
        participantShares.put(3L, new BigDecimal("50"));  // Total = 80% (Invalid)

        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("200.00"))
                .currency(Currency.USD)
                .description("Grocery")
                .payerId(1L)
                .participantIds(Arrays.asList(2L, 3L))
                .splitType(SplitType.PERCENTAGE)
                .participantShares(participantShares)
                .build();

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.addExpense(expenseDto));
        assertEquals("Total percentage must equal 100%", exception.getMessage());
    }

    /**
     * ✅ Test Invalid Exact Split (Sum Mismatch)
     */
    @Test
    void testAddExpenseInvalidExactSplit() {
        Map<Long, BigDecimal> participantShares = new HashMap<>();
        participantShares.put(2L, new BigDecimal("40.00"));
        participantShares.put(3L, new BigDecimal("60.00"));  // Total = 100.00 (Valid) but Expense Amount is 120

        ExpenseDto expenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("120.00"))
                .currency(Currency.USD)
                .description("Movie Tickets")
                .payerId(1L)
                .participantIds(Arrays.asList(2L, 3L))
                .splitType(SplitType.EXACT)
                .participantShares(participantShares)
                .build();

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.addExpense(expenseDto));
        assertEquals("Total exact shares must equal the total amount", exception.getMessage());
    }

    /**
     * ✅ Test Get Expense by Valid ID
     */
    @Test
    void testGetExpenseById() throws ExpenseException {
        Expense expense = Expense.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .currency(Currency.USD)
                .description("Trip Expense")
                .payer(payer)
                .group(group)
                .participants(Arrays.asList(participant1, participant2))
                .splitType(SplitType.EQUAL)
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        ExpenseDto result = expenseService.getExpenseById(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals("Trip Expense", result.getDescription());
        assertEquals(1L, result.getPayerId());
        assertEquals(10L, result.getGroupId());
    }

    /**
     * ❌ Test Get Expense by Invalid ID
     */
    @Test
    void testGetExpenseByInvalidId() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.getExpenseById(99L));
        assertEquals("Expense not found with ID: 99", exception.getMessage());
    }

    /**
     * ✅ Test Get Expenses for Authenticated User
     */
    @Test
    void testGetExpensesForAuthenticatedUser() {
        // Mock authenticated user
        when(userService.getAuthenticatedUser()).thenReturn(payer);

        // Mock expenses for the authenticated user
        Expense expense1 = Expense.builder().id(1L).payer(payer).amount(new BigDecimal("100.00")).currency(Currency.USD).build();
        Expense expense2 = Expense.builder().id(2L).payer(payer).amount(new BigDecimal("200.00")).currency(Currency.USD).build();

        when(expenseRepository.findAllByPayerOrParticipantsContaining(payer, payer))
                .thenReturn(Arrays.asList(expense1, expense2));

        List<ExpenseDto> result = expenseService.getExpensesByUser();

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("200.00"), result.get(1).getAmount());
    }

    /**
     * ✅ Test Get Expenses by Group
     */
    @Test
    void testGetExpensesByGroup() throws ExpenseException {
        Expense expense1 = Expense.builder().id(1L).group(group).amount(new BigDecimal("150.00")).currency(Currency.USD).build();
        Expense expense2 = Expense.builder().id(2L).group(group).amount(new BigDecimal("250.00")).currency(Currency.USD).build();

        when(expenseRepository.findAllByGroup(group)).thenReturn(Arrays.asList(expense1, expense2));

        List<ExpenseDto> result = expenseService.getExpensesByGroup(10L);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("150.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("250.00"), result.get(1).getAmount());
    }

    /**
     * ❌ Test Get Expenses by Invalid Group
     */
    @Test
    void testGetExpensesByInvalidGroup() {
        when(groupService.fetchGroupById(99L)).thenReturn(null);

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.getExpensesByGroup(99L));
        assertEquals("Group not found", exception.getMessage());
    }

    /**
     * ✅ Test Update Expense
     */
    @Test
    void testUpdateExpense() throws ExpenseException {
        Expense expense = Expense.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .currency(Currency.USD)
                .description("Old Description")
                .splitType(SplitType.EQUAL)
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        ExpenseDto updatedExpenseDto = ExpenseDto.builder()
                .amount(new BigDecimal("200.00"))
                .currency(Currency.USD)
                .description("Updated Description")
                .splitType(SplitType.EXACT)
                .build();

        expenseService.updateExpense(1L, updatedExpenseDto);

        assertEquals(new BigDecimal("200.00"), expense.getAmount());
        assertEquals("Updated Description", expense.getDescription());
        assertEquals(SplitType.EXACT, expense.getSplitType());

        verify(expenseRepository, times(1)).save(expense);
    }

    @Test
    void testUpdateExpense_Success() throws ExpenseException {
        Expense expense = Expense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .currency(Currency.USD)
                .description("Old Description")
                .payer(payer)
                .participants(Arrays.asList(participant1, participant2))
                .splitType(SplitType.EQUAL)
                .build();

        ExpenseDto updateDto = ExpenseDto.builder()
                .amount(new BigDecimal("150.00"))
                .currency(Currency.EUR)
                .description("Updated Description")
                .splitType(SplitType.PERCENTAGE)
                .participantShares(Map.of(2L, new BigDecimal("40"), 3L, new BigDecimal("60")))
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(userService.getAuthenticatedUser()).thenReturn(payer);
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        expenseService.updateExpense(1L, updateDto);

        assertEquals(new BigDecimal("150.00"), expense.getAmount());
        assertEquals(Currency.EUR, expense.getCurrency());
        assertEquals("Updated Description", expense.getDescription());
        assertEquals(SplitType.PERCENTAGE, expense.getSplitType());
        assertEquals(2, expense.getParticipantShares().size());

        verify(expenseRepository, times(1)).save(expense);
    }

    @Test
    void testUpdateExpense_OnlyDescription() throws ExpenseException {
        Expense expense = Expense.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .currency(Currency.USD)
                .description("Old Desc")
                .payer(payer)
                .participants(Arrays.asList(participant1, participant2))
                .splitType(SplitType.EQUAL)
                .build();

        ExpenseDto updateDto = ExpenseDto.builder()
                .description("New Description")
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(userService.getAuthenticatedUser()).thenReturn(payer);

        expenseService.updateExpense(1L, updateDto);

        assertEquals("New Description", expense.getDescription());
        assertEquals(new BigDecimal("100.00"), expense.getAmount()); // Amount unchanged
        assertEquals(SplitType.EQUAL, expense.getSplitType()); // Split Type unchanged

        verify(expenseRepository, times(1)).save(expense);
    }

    @Test
    void testUpdateExpense_WithoutPermission() {
        Expense expense = Expense.builder()
                .id(1L)
                .payer(participant1) // A different user is the payer
                .participants(Arrays.asList(participant2))
                .splitType(SplitType.EQUAL)
                .build();

        ExpenseDto updateDto = ExpenseDto.builder().amount(new BigDecimal("200.00")).build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(userService.getAuthenticatedUser()).thenReturn(payer); // Authenticated user is **not the payer**

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.updateExpense(1L, updateDto));
        assertEquals("You do not have permission to update this expense.", exception.getMessage());
    }

    @Test
    void testUpdateExpense_InvalidGroup() {
        Expense expense = Expense.builder()
                .id(1L)
                .payer(payer)
                .participants(Arrays.asList(participant1, participant2))
                .splitType(SplitType.EQUAL)
                .build();

        ExpenseDto updateDto = ExpenseDto.builder()
                .groupId(99L) // Non-existent group
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(userService.getAuthenticatedUser()).thenReturn(payer);
        when(groupService.fetchGroupById(99L)).thenReturn(null);

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.updateExpense(1L, updateDto));
        assertEquals("Invalid Group ID", exception.getMessage());
    }



    /**
     * ❌ Test Update Non-Existent Expense
     */
    @Test
    void testUpdateNonExistentExpense() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        ExpenseDto updateDto = ExpenseDto.builder().amount(new BigDecimal("200.00")).build();

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.updateExpense(99L, updateDto));
        assertEquals("Expense not found with ID: 99", exception.getMessage());
    }

    /**
     * ✅ Test Delete Expense
     */
    @Test
    void testDeleteExpense() throws ExpenseException {
        Expense expense = Expense.builder().id(1L).build();
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(1L);
        verify(expenseRepository, times(1)).delete(expense);
    }

    /**
     * ❌ Test Delete Non-Existent Expense
     */
    @Test
    void testDeleteNonExistentExpense() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        ExpenseException exception = assertThrows(ExpenseException.class, () -> expenseService.deleteExpense(99L));
        assertEquals("Expense not found with ID: 99", exception.getMessage());
    }


}