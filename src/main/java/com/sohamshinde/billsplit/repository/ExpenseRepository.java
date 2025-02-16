package com.sohamshinde.billsplit.repository;

import com.sohamshinde.billsplit.entity.Expense;
import com.sohamshinde.billsplit.entity.Group;
import com.sohamshinde.billsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByPayerOrParticipantsContaining(User payer, User participant);

    List<Expense> findAllByGroup(Group group);
}
