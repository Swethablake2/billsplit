package com.sohamshinde.billsplit.service;

import com.sohamshinde.billsplit.dto.GroupDto;
import com.sohamshinde.billsplit.entity.Group;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.exceptions.GroupException;
import com.sohamshinde.billsplit.exceptions.UserNotFoundException;
import com.sohamshinde.billsplit.repository.GroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GroupService {
    private GroupRepository groupRepository;

    private UserService userService;

    public GroupService(GroupRepository groupRepository, @Lazy UserService userService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Group fetchGroupById(Long id) throws GroupException {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new GroupException("Group not found"));

        if (group.getMembers() == null) {
            group.setMembers(Collections.emptyList());
        }

        // Ensure the authenticated user is part of this group
        User user = getAuthenticatedUser();
        if (!group.getMembers().contains(user)) {
            throw new GroupException("You do not have permission to access this group.");
        }

        return group;
    }

    // Create a group (Creator = Logged-in User)
    public void createGroup(GroupDto groupDto) {
        User creator = getAuthenticatedUser();

        // Fetch members from provided IDs
        List<User> members = new ArrayList<>(userService.fetchUsersByIds(groupDto.getMemberIds())); // ✅ Make it mutable

        if (members.isEmpty()) {
            throw new GroupException("Invalid member IDs");
        }

        // Ensure the creator is added to the group
        if (!members.contains(creator)) {
            members.add(creator);
        }

        Group group = new Group();
        group.setGroupName(groupDto.getGroupName());
        group.setMembers(members);
        group.setCreatedBy(creator); // Set logged-in user as creator

        groupRepository.save(group);
    }

    // Get group by ID (Only if the logged-in user is a member)
    public Group getGroupById(Long groupId) throws GroupException {
        User authenticatedUser = getAuthenticatedUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if the logged-in user is a member of the group
        if (!group.getMembers().contains(authenticatedUser)) {
            throw new GroupException("Access denied: You are not a member of this group");
        }

        return group;
    }

    // Get all groups for the authenticated user
    public List<Group> getAllGroupsForAuthenticatedUser() {
        User user = getAuthenticatedUser();
        return groupRepository.findGroupsByMemberId(user.getId());
    }

    // Delete a group (Only the creator can perform this action)
    public void deleteGroup(Long groupId) throws GroupException {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));


        if (!group.getCreatedBy().equals(getAuthenticatedUser())) {
            throw new GroupException("You do not have permission to delete this group.");
        }

        groupRepository.delete(group);
    }

    // Update a group (Only the creator can perform this action)
    public Group updateGroup(Long groupId, GroupDto groupDto) throws GroupException {
        Group group = fetchGroupById(groupId);

        System.out.println(group.getGroupName());

        if (!group.getCreatedBy().equals(getAuthenticatedUser())) {
            throw new GroupException("You do not have permission to update this group.");
        }

        if (groupDto.getGroupName() != null) {
            group.setGroupName(groupDto.getGroupName());
        }

        if (groupDto.getMemberIds() != null && !groupDto.getMemberIds().isEmpty()) {
            try {
                List<User> members = userService.fetchUsersByIds(groupDto.getMemberIds());
                if (members.isEmpty()) {
                    throw new GroupException("Invalid member IDs");
                }
                group.setMembers(members);
            } catch (UserNotFoundException e) {
                throw new GroupException("Invalid member IDs");
            }
        }

        return groupRepository.save(group);
    }
}
