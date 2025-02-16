package com.sohamshinde.billsplit.service;

import com.sohamshinde.billsplit.dto.ExpenseDto;
import com.sohamshinde.billsplit.entity.Expense;
import com.sohamshinde.billsplit.entity.Group;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.exceptions.ExpenseException;
import com.sohamshinde.billsplit.repository.ExpenseRepository;
import com.sohamshinde.billsplit.enums.SplitType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.sohamshinde.billsplit.utils.AuthUtil.getAuthenticatedUser;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseService {

    private UserService userService;

    private GroupService groupService;

    private ExpenseRepository expenseRepository;

    @Value("${cache.expense.enabled:false}") // Default is false if not set
    private boolean isCacheEnabled;

    private RedisTemplate<String, ExpenseDto> redisTemplate;

    public ExpenseService(UserService userService, GroupService groupService, ExpenseRepository expenseRepository, RedisTemplate<String, ExpenseDto> redisTemplate) {
        this.userService = userService;
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
        this.redisTemplate = redisTemplate;
    }

    public void addExpense(ExpenseDto expenseDto) throws ExpenseException {
        if (expenseDto.getAmount() == null) {
            throw new ExpenseException("Amount cannot be null");
        }

        if (expenseDto.getSplitType() == null) {
            throw new ExpenseException("Split type must be provided");
        }

        User payer = userService.fetchUserById(expenseDto.getPayerId());
        if (payer == null) {
            throw new ExpenseException("Payer not found");
        }

        final List<User> participants;
        if (expenseDto.getGroupId() != null) {
            Group group = groupService.fetchGroupById(expenseDto.getGroupId());
            if (group == null) {
                throw new ExpenseException("Invalid Group ID");
            }
            participants = group.getMembers();
        } else if (expenseDto.getParticipantIds() != null) {
            participants = expenseDto.getParticipantIds().stream()
                    .map(userService::fetchUserById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (participants.isEmpty()) {
                throw new ExpenseException("No valid participants found");
            }
        } else {
            throw new ExpenseException("Either group ID or participant IDs must be provided");
        }

        // ✅ Use the common method to calculate shares
        Map<User, BigDecimal> participantShares = calculateShares(
                expenseDto.getSplitType(),
                expenseDto.getAmount(),
                participants,
                expenseDto.getParticipantShares()
        );

        Expense expense = Expense.builder()
                .payer(payer)
                .group(expenseDto.getGroupId() != null ? groupService.fetchGroupById(expenseDto.getGroupId()) : null)
                .participants(participants)
                .amount(expenseDto.getAmount())
                .currency(expenseDto.getCurrency())
                .description(expenseDto.getDescription())
                .splitType(expenseDto.getSplitType())
                .participantShares(participantShares)
                .status(expenseDto.getStatus())
                .category(expenseDto.getCategory())
                .build();

        expenseRepository.save(expense);
    }

    private static Map<User, BigDecimal> calculateShares(
            SplitType splitType,
            BigDecimal totalAmount,
            List<User> participants,
            Map<Long, BigDecimal> participantShares) throws ExpenseException {

        Map<User, BigDecimal> calculatedShares = new HashMap<>();

        switch (splitType) {
            case EQUAL:
                if (participants.isEmpty()) {
                    throw new ExpenseException("No participants found for equal split.");
                }
                BigDecimal equalShare = totalAmount.divide(BigDecimal.valueOf(participants.size()), 2, BigDecimal.ROUND_HALF_UP);
                for (User participant : participants) {
                    calculatedShares.put(participant, equalShare);
                }
                break;

            case PERCENTAGE:
                if (participantShares == null || participantShares.isEmpty()) {
                    throw new ExpenseException("Participant shares must be provided for PERCENTAGE split type");
                }
                BigDecimal totalPercentage = participantShares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
                    throw new ExpenseException("Total percentage must equal 100%");
                }
                for (Map.Entry<Long, BigDecimal> entry : participantShares.entrySet()) {
                    User participant = participants.stream()
                            .filter(p -> p.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow(() -> new ExpenseException("Invalid participant ID: " + entry.getKey()));

                    BigDecimal individualShare = totalAmount.multiply(entry.getValue()).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                    calculatedShares.put(participant, individualShare);
                }
                break;

            case EXACT:
                if (participantShares == null || participantShares.isEmpty()) {
                    throw new ExpenseException("Participant shares must be provided for EXACT split type");
                }
                BigDecimal totalExactAmount = participantShares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalExactAmount.compareTo(totalAmount) != 0) {
                    throw new ExpenseException("Total exact shares must equal the total amount");
                }
                for (Map.Entry<Long, BigDecimal> entry : participantShares.entrySet()) {
                    User participant = participants.stream()
                            .filter(p -> p.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow(() -> new ExpenseException("Invalid participant ID: " + entry.getKey()));

                    calculatedShares.put(participant, entry.getValue());
                }
                break;

            default:
                throw new ExpenseException("Invalid split type");
        }

        return calculatedShares;
    }

    public ExpenseDto getExpenseById(Long expenseId) throws ExpenseException {
        User authenticatedUser = getAuthenticatedUser();
        String cacheKey = "expense:" + expenseId; // ✅ Cache key for Redis

        // ✅ If caching is enabled, check Redis before querying the database
        if (isCacheEnabled) {
            ValueOperations<String, ExpenseDto> ops = redisTemplate.opsForValue();
            ExpenseDto cachedExpense = ops.get(cacheKey);

            if (cachedExpense != null) {
                System.out.println("✅ Retrieved from Redis Cache: " + cacheKey);
                return cachedExpense;
            }
        }

        // ⚠️ If cache is disabled or expense is not in Redis, fetch from DB
        System.out.println("⚠️ Fetching from Database...");
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseException("Expense not found with ID: " + expenseId));

        // ✅ Access control check
        if (!expense.getPayer().equals(authenticatedUser) && !expense.getParticipants().contains(authenticatedUser)) {
            throw new ExpenseException("Access denied: You are not a part of this expense");
        }

        ExpenseDto expenseDto = convertToDto(expense);

        // ✅ Store in Redis only if caching is enabled
        if (isCacheEnabled) {
            redisTemplate.opsForValue().set(cacheKey, expenseDto, 10, TimeUnit.MINUTES); // Cache for 10 mins
            System.out.println("✅ Stored in Redis Cache: " + cacheKey);
        }

        return expenseDto;
    }

    public List<ExpenseDto> getExpensesByUser() {
        User authenticatedUser = getAuthenticatedUser();
        List<Expense> expenses = expenseRepository.findAllByPayerOrParticipantsContaining(authenticatedUser, authenticatedUser);
        return expenses.stream().map(this::convertToDto).collect(Collectors.toList());
    }


    public List<ExpenseDto> getExpensesByGroup(Long groupId) throws ExpenseException {
        User authenticatedUser = getAuthenticatedUser();
        Group group = groupService.fetchGroupById(groupId);

        if (group == null) {
            throw new ExpenseException("Group not found");
        }

        if (!group.getMembers().contains(authenticatedUser)) {
            throw new ExpenseException("Access denied: You are not a member of this group");
        }

        List<Expense> expenses = expenseRepository.findAllByGroup(group);
        return expenses.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public void updateExpense(Long expenseId, ExpenseDto expenseDto) throws ExpenseException {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseException("Expense not found with ID: " + expenseId));

        User authenticatedUser = userService.getAuthenticatedUser(); // Get logged-in user

        // ✅ Prevent NullPointerException when checking if the user has permission
        boolean isPayer = expense.getPayer() != null && expense.getPayer().equals(authenticatedUser);
        boolean isParticipant = expense.getParticipants() != null && expense.getParticipants().contains(authenticatedUser);
        boolean isGroupMember = expense.getGroup() != null && expense.getGroup().getMembers() != null
                && expense.getGroup().getMembers().contains(authenticatedUser);

        System.out.println("DEBUG: isPayer=" + isPayer + ", isParticipant=" + isParticipant + ", isGroupMember=" + isGroupMember);

        if (!isPayer && !isParticipant && !isGroupMember) {
            throw new ExpenseException("You do not have permission to update this expense.");
        }

        // ✅ Update basic fields if provided
        if (expenseDto.getAmount() != null) {
            expense.setAmount(expenseDto.getAmount());
        }
        if (expenseDto.getCurrency() != null) {
            expense.setCurrency(expenseDto.getCurrency());
        }
        if (expenseDto.getDescription() != null) {
            expense.setDescription(expenseDto.getDescription());
        }
        if (expenseDto.getStatus() != null) {
            expense.setStatus(expenseDto.getStatus());
        }
        if (expenseDto.getCategory() != null) {
            expense.setCategory(expenseDto.getCategory());
        }

        // ✅ Fetch updated participants (if changed)
        List<User> updatedParticipants = new ArrayList<>(
                expense.getParticipants() != null ? expense.getParticipants() : Collections.emptyList()
        );

        boolean participantsUpdated = false;

        if (expenseDto.getGroupId() != null) {
            Group group = groupService.fetchGroupById(expenseDto.getGroupId());
            if (group == null) {
                throw new ExpenseException("Invalid Group ID");
            }
            updatedParticipants = group.getMembers();
            participantsUpdated = true;
        } else if (expenseDto.getParticipantIds() != null) {
            updatedParticipants = expenseDto.getParticipantIds().stream()
                    .map(userService::fetchUserById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (updatedParticipants.isEmpty()) {
                throw new ExpenseException("No valid participants found");
            }
            participantsUpdated = true;
        }

        expense.setParticipants(updatedParticipants);

        // ✅ **Recalculate shares only if required**
        boolean shouldRecalculateShares = expenseDto.getSplitType() != null || participantsUpdated;

        if (shouldRecalculateShares) {
            SplitType newSplitType = expenseDto.getSplitType() != null ? expenseDto.getSplitType() : expense.getSplitType();

            // Validate participantShares if updating to PERCENTAGE or EXACT
            if (newSplitType == SplitType.PERCENTAGE || newSplitType == SplitType.EXACT) {
                if (expenseDto.getParticipantShares() == null || expenseDto.getParticipantShares().isEmpty()) {
                    throw new ExpenseException("Participant shares must be provided for " + newSplitType + " split type.");
                }
            }

            expense.setSplitType(newSplitType);
            expense.setParticipantShares(
                    calculateShares(newSplitType, expense.getAmount(), updatedParticipants, expenseDto.getParticipantShares())
            );
        }

        // ✅ Save updated expense
        expenseRepository.save(expense);
    }


    public void deleteExpense(Long expenseId) throws ExpenseException {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseException("Expense not found with ID: " + expenseId));
        expenseRepository.delete(expense);
    }

    private ExpenseDto convertToDto(Expense expense) {
        return ExpenseDto.builder()
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .description(expense.getDescription())
                .groupId(expense.getGroup() != null ? expense.getGroup().getId() : null)
                .payerId(expense.getPayer() != null ? expense.getPayer().getId() : null) // ✅ Handle null payer
                .participantIds(expense.getParticipants() != null ?
                        expense.getParticipants().stream().map(User::getId).collect(Collectors.toList()) : new ArrayList<>()) // ✅ Handle null participants
                .splitType(expense.getSplitType())
                .participantShares(expense.getParticipantShares() != null ?
                        expense.getParticipantShares().entrySet().stream()
                                .collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue))
                        : new HashMap<>()) // ✅ Handle null shares
                .status(expense.getStatus())
                .category(expense.getCategory())
                .build();
    }

}
