package com.sohamshinde.billsplit.service;

import com.sohamshinde.billsplit.dto.GroupDto;
import com.sohamshinde.billsplit.entity.Group;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.exceptions.GroupException;
import com.sohamshinde.billsplit.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private User member1, member2;

    private Group testGroup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a mock authenticated user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");

        // Set up a security context with the authenticated user
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(testUser, null));
        SecurityContextHolder.setContext(securityContext);

        // Create a mock group
        testGroup = new Group();
        testGroup.setId(100L);
        testGroup.setGroupName("Test Group");
        testGroup.setMembers(Collections.singletonList(testUser)); // User is a member

        // Create mock members
        member1 = new User();
        member1.setId(2L);
        member1.setName("Alice");

        member2 = new User();
        member2.setId(3L);
        member2.setName("Bob");
    }

    /**
     * ✅ Test: Fetch an existing group where the authenticated user is a member.
     */
    @Test
    void testFetchGroupById_Success() {
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

        Group fetchedGroup = groupService.fetchGroupById(100L);

        assertNotNull(fetchedGroup);
        assertEquals(100L, fetchedGroup.getId());
        assertEquals("Test Group", fetchedGroup.getGroupName());
    }

    /**
     * ❌ Test: Fetch a non-existent group (Expect GroupException).
     */
    @Test
    void testFetchGroupById_GroupNotFound() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(GroupException.class, () -> groupService.fetchGroupById(999L));
        assertEquals("Group not found", exception.getMessage());
    }

    /**
     * ❌ Test: Fetch a group where the authenticated user is NOT a member.
     */
    @Test
    void testFetchGroupById_UserNotInGroup() {
        // Create a new group that does NOT contain the authenticated user
        Group anotherGroup = new Group();
        anotherGroup.setId(200L);
        anotherGroup.setGroupName("Another Group");
        anotherGroup.setMembers(Collections.emptyList()); // User is NOT a member

        when(groupRepository.findById(200L)).thenReturn(Optional.of(anotherGroup));

        Exception exception = assertThrows(GroupException.class, () -> groupService.fetchGroupById(200L));
        assertEquals("You do not have permission to access this group.", exception.getMessage());
    }

    /**
     * ✅ Test: Successfully create a new group with valid members.
     */
    @Test
    void testCreateGroup_Success() {
        // Given: A valid GroupDto
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("Friends Group");
        groupDto.setMemberIds(Arrays.asList(2L, 3L)); // IDs of member1 and member2

        // Mock fetching users by IDs
        when(userService.fetchUsersByIds(Arrays.asList(2L, 3L))).thenReturn(Arrays.asList(member1, member2));

        // Call createGroup method
        groupService.createGroup(groupDto);

        // Capture the saved group
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());

        // Assertions: Ensure the group has correct attributes
        Group savedGroup = groupCaptor.getValue();
        assertEquals("Friends Group", savedGroup.getGroupName());
        assertTrue(savedGroup.getMembers().containsAll(Arrays.asList(member1, member2, testUser))); // Members include creator
        assertEquals(testUser, savedGroup.getCreatedBy()); // Authenticated user is set as the creator
    }

    /**
     * ❌ Test: Creating a group with invalid member IDs should throw an exception.
     */
    @Test
    void testCreateGroup_InvalidMembers() {
        // Given: A GroupDto with invalid member IDs
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("Invalid Group");
        groupDto.setMemberIds(Arrays.asList(99L, 100L)); // Non-existing user IDs

        // Mock fetching users by IDs returning an empty list
        when(userService.fetchUsersByIds(Arrays.asList(99L, 100L))).thenReturn(Collections.emptyList());

        // Expect GroupException when calling createGroup
        Exception exception = assertThrows(GroupException.class, () -> groupService.createGroup(groupDto));
        assertEquals("Invalid member IDs", exception.getMessage());

        // Ensure group is NOT saved
        verify(groupRepository, never()).save(any(Group.class));
    }

    /**
     * ✅ Test: Ensure the authenticated user is always included in the group.
     */
    @Test
    void testCreateGroup_EnsureCreatorAdded() {
        // Given: A GroupDto where the creator is not explicitly listed
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("My Group");
        groupDto.setMemberIds(Collections.singletonList(2L)); // Only Alice, testUser missing

        // Mock fetching users by IDs
        when(userService.fetchUsersByIds(Collections.singletonList(2L))).thenReturn(Collections.singletonList(member1));

        // Call createGroup method
        groupService.createGroup(groupDto);

        // Capture the saved group
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());

        // Assertions: Ensure the authenticated user is added
        Group savedGroup = groupCaptor.getValue();
        assertTrue(savedGroup.getMembers().contains(testUser)); // Creator should be in members list
    }

    /**
     * ✅ Test: Verify the creator is set correctly.
     */
    @Test
    void testCreateGroup_CreatorIsSet() {
        // Given: A valid GroupDto
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("Office Group");
        groupDto.setMemberIds(Arrays.asList(2L, 3L)); // IDs of member1 and member2

        // Mock fetching users by IDs
        when(userService.fetchUsersByIds(Arrays.asList(2L, 3L))).thenReturn(Arrays.asList(member1, member2));

        // Call createGroup method
        groupService.createGroup(groupDto);

        // Capture the saved group
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());

        // Assertions: Ensure the creator is correctly set
        Group savedGroup = groupCaptor.getValue();
        assertEquals(testUser, savedGroup.getCreatedBy()); // Creator should be authenticated user
    }

    @Test
    void testGetGroupById_Success() {
        Group group = new Group();
        group.setId(1L);
        group.setMembers(Collections.singletonList(testUser));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);

        Group result = groupService.getGroupById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetGroupById_UserNotInGroup() {
        Group group = new Group();
        group.setId(1L);
        group.setMembers(Collections.emptyList()); // No members

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);

        Exception exception = assertThrows(GroupException.class, () -> groupService.getGroupById(1L));
        assertEquals("Access denied: You are not a member of this group", exception.getMessage());
    }

    @Test
    void testGetGroupById_GroupNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(GroupException.class, () -> groupService.getGroupById(1L));
        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void testGetAllGroupsForAuthenticatedUser_WithGroups() {
        Group group1 = new Group();
        group1.setId(1L);
        group1.setMembers(Collections.singletonList(testUser));

        Group group2 = new Group();
        group2.setId(2L);
        group2.setMembers(Collections.singletonList(testUser));

        List<Group> groups = Arrays.asList(group1, group2);

        when(userService.getAuthenticatedUser()).thenReturn(testUser);
        when(groupRepository.findGroupsByMemberId(testUser.getId())).thenReturn(groups);

        List<Group> result = groupService.getAllGroupsForAuthenticatedUser();

        assertEquals(2, result.size());
    }

    @Test
    void testGetAllGroupsForAuthenticatedUser_NoGroups() {
        when(userService.getAuthenticatedUser()).thenReturn(testUser);
        when(groupRepository.findGroupsByMemberId(testUser.getId())).thenReturn(Collections.emptyList());

        List<Group> result = groupService.getAllGroupsForAuthenticatedUser();

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteGroup_Success() {
        Group group = new Group();
        group.setId(1L);
        group.setCreatedBy(testUser);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);

        groupService.deleteGroup(1L);

        verify(groupRepository, times(1)).delete(group);
    }

    @Test
    void testDeleteGroup_NotCreator() {
        Group group = new Group();
        group.setId(1L);
        group.setCreatedBy(new User()); // Different user as creator

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);

        Exception exception = assertThrows(GroupException.class, () -> groupService.deleteGroup(1L));
        assertEquals("You do not have permission to delete this group.", exception.getMessage());

        verify(groupRepository, never()).delete(group);
    }

    @Test
    void testDeleteGroup_GroupNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(GroupException.class, () -> groupService.deleteGroup(1L));
        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void testUpdateGroup_Success() {
        // ✅ Initialize group and ensure members list is not null
        Group group = new Group();
        group.setId(1L);
        group.setGroupName("Old Group");
        group.setCreatedBy(testUser);
        group.setMembers(new ArrayList<>(Arrays.asList(testUser)));

        // ✅ Define the update request
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("Updated Group");
        groupDto.setMemberIds(Arrays.asList(2L, 3L));

        // ✅ Mock new members
        User member1 = new User();
        member1.setId(2L);
        User member2 = new User();
        member2.setId(3L);
        List<User> newMembers = Arrays.asList(member1, member2);

        // ✅ Mock repository and service calls
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);
        when(userService.fetchUsersByIds(groupDto.getMemberIds())).thenReturn(newMembers);
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0)); // ✅ Ensure save() returns the updated group

        // ✅ Call update function
        Group updatedGroup = groupService.updateGroup(1L, groupDto);

        // ✅ Assertions
        assertNotNull(updatedGroup); // ✅ Prevent NullPointerException
        assertEquals("Updated Group", updatedGroup.getGroupName());
        assertEquals(2, updatedGroup.getMembers().size());
    }

    @Test
    void testUpdateGroup_NotCreator() {
        Group group = new Group();
        group.setId(1L);
        group.setCreatedBy(new User()); // Different creator
        group.setMembers(new ArrayList<>(Arrays.asList(testUser)));

        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("New Name");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);

        Exception exception = assertThrows(GroupException.class, () -> groupService.updateGroup(1L, groupDto));
        assertEquals("You do not have permission to update this group.", exception.getMessage());

        verify(groupRepository, never()).save(any(Group.class));
    }

    @Test
    void testUpdateGroup_GroupNotFound() {
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupName("New Name");

        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(GroupException.class, () -> groupService.updateGroup(1L, groupDto));
        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void testUpdateGroup_InvalidMembers() {
        Group group = new Group();
        group.setId(1L);
        group.setCreatedBy(testUser);
        group.setMembers(Arrays.asList(testUser));

        GroupDto groupDto = new GroupDto();
        groupDto.setMemberIds(Arrays.asList(99L, 100L)); // Invalid members

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);
//        when(userService.fetchUsersByIds(groupDto.getMemberIds())).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(GroupException.class, () -> groupService.updateGroup(1L, groupDto));
        assertEquals("Invalid member IDs", exception.getMessage());
    }

    @Test
    void testUpdateGroup_NoChanges() {
        // ✅ Ensure members list is initialized
        Group group = new Group();
        group.setId(1L);
        group.setGroupName("Original Name");
        group.setCreatedBy(testUser);
        group.setMembers(Collections.singletonList(testUser));

        GroupDto groupDto = new GroupDto(); // No changes

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userService.getAuthenticatedUser()).thenReturn(testUser);
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        Group updatedGroup = groupService.updateGroup(1L, groupDto);

        assertNotNull(updatedGroup); // ✅ Prevent NullPointerException
        assertEquals("Original Name", updatedGroup.getGroupName());
        assertEquals(group.getMembers(), updatedGroup.getMembers());
    }


}