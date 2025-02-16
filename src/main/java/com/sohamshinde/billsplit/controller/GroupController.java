package com.sohamshinde.billsplit.controller;

import com.sohamshinde.billsplit.dto.GroupDto;
import com.sohamshinde.billsplit.entity.Group;
import com.sohamshinde.billsplit.exceptions.GroupException;
import com.sohamshinde.billsplit.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/group")
@Validated
public class GroupController {

    // Create a new group
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Create a group (Only authenticated users)
    @PostMapping
    public ResponseEntity<String> createGroup(@Valid @RequestBody GroupDto groupDto) {
        groupService.createGroup(groupDto);
        return ResponseEntity.ok("Group created successfully.");
    }

    // Get all groups for the logged-in user
    @GetMapping
    public ResponseEntity<List<Group>> getAllGroupsForAuthenticatedUser() {
        List<Group> groups = groupService.getAllGroupsForAuthenticatedUser();
        return ResponseEntity.ok(groups);
    }

    // Get group details (Only members can access)
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        try {
            Group group = groupService.fetchGroupById(groupId);
            return ResponseEntity.ok(group);
        } catch (GroupException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    // Update a group (Only the creator can perform this action)
    @PutMapping("/{groupId}")
    public ResponseEntity<String> updateGroup(@PathVariable Long groupId, @RequestBody GroupDto groupDto) {
        try {
            groupService.updateGroup(groupId, groupDto);
            return ResponseEntity.ok("Group updated successfully.");
        } catch (GroupException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }

    // Delete a group (Only the creator can perform this action)
    @DeleteMapping("/{groupId}")
    public ResponseEntity<String> deleteGroup(@PathVariable Long groupId) {
        try {
            groupService.deleteGroup(groupId);
            return ResponseEntity.ok("Group deleted successfully.");
        } catch (GroupException ex) {
            return ResponseEntity.status(403).body(ex.getMessage());
        }
    }
}
