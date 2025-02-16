package com.sohamshinde.billsplit.repository;


import com.sohamshinde.billsplit.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.id = :memberId")
    List<Group> findGroupsByMemberId(@Param("memberId") Long memberId);

    @Override
    Optional<Group> findById(Long aLong);
}
