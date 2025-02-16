//package com.sohamshinde.billsplit.mapper.toDTO;
//
//public class toExpenseDTO {
//
//    private ExpenseDto convertToDto(Expense expense) {
//        return ExpenseDto.builder()
//                .amount(expense.getAmount())
//                .currency(expense.getCurrency())
//                .description(expense.getDescription())
//                .payer_id(expense.getPayer().getId())
//                .participant_ids(expense.getParticipants().stream().map(User::getId).collect(Collectors.toList()))
//                .group_id(expense.getGroup() != null ? expense.getGroup().getId() : null)
//                .build();
//    }
//
//}
