package com.homie.finance.service;

import com.homie.finance.entity.GroupSpace;
import com.homie.finance.entity.User;
import com.homie.finance.repository.GroupSpaceRepository;
import com.homie.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupSpaceService {

    @Autowired
    private GroupSpaceRepository groupSpaceRepository;

    @Autowired
    private UserRepository userRepository;

    // Lấy User hiện tại từ Token
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
    }

    // 1. Tạo nhóm mới
    @Transactional
    public GroupSpace createGroup(String groupName) {
        User me = getCurrentUser();

        if (groupSpaceRepository.existsByNameAndOwner(groupName, me)) {
            throw new IllegalArgumentException("Homie đã có một nhóm tên '" + groupName + "' rồi!");
        }

        GroupSpace group = new GroupSpace();
        group.setName(groupName);
        group.setOwner(me);
        group.setInviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        Set<User> members = new HashSet<>();
        members.add(me);
        group.setMembers(members);

        return groupSpaceRepository.save(group);
    }

    // 2. Tham gia nhóm bằng Mã Code (Dùng Repository check member để né lỗi Lazy)
    @Transactional
    public GroupSpace joinGroup(String inviteCode) {
        User me = getCurrentUser();

        GroupSpace group = groupSpaceRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Mã mời không hợp lệ!"));

        if (groupSpaceRepository.existsByIdAndMembersContaining(group.getId(), me)) {
            throw new IllegalArgumentException("Homie đã ở trong nhóm này rồi!");
        }

        group.getMembers().add(me);
        return groupSpaceRepository.save(group);
    }

    // 3. Lấy danh sách nhóm của tôi
    @Transactional(readOnly = true)
    public List<GroupSpace> getMyGroups() {
        User me = getCurrentUser();
        List<GroupSpace> groups = groupSpaceRepository.findByMembersContaining(me);

        // Nạp size để Jackson build JSON thành viên không bị lỗi
        groups.forEach(g -> {
            if (g.getMembers() != null) g.getMembers().size();
        });

        return groups;
    }

    // 4. Chỉnh sửa tên nhóm (Chỉ Owner)
    @Transactional
    public GroupSpace updateGroup(String groupId, String newName) {
        User me = getCurrentUser();
        GroupSpace group = groupSpaceRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm!"));

        if (!group.getOwner().getId().equals(me.getId())) {
            throw new RuntimeException("Chỉ chủ nhóm mới có quyền đổi tên!");
        }

        if (groupSpaceRepository.existsByNameAndOwner(newName, me)) {
            throw new IllegalArgumentException("Tên nhóm này homie đã sử dụng rồi!");
        }

        group.setName(newName);
        GroupSpace updated = groupSpaceRepository.save(group);

        // Nạp members để trả về JSON đầy đủ
        if (updated.getMembers() != null) updated.getMembers().size();

        return updated;
    }

    // 5. Giải tán nhóm (Chỉ Owner)
    @Transactional
    public void deleteGroup(String groupId) {
        User me = getCurrentUser();
        GroupSpace group = groupSpaceRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm!"));

        if (!group.getOwner().getId().equals(me.getId())) {
            throw new RuntimeException("Chỉ chủ nhóm mới được giải tán nhóm!");
        }

        groupSpaceRepository.delete(group);
    }

    // 6. Rời khỏi nhóm (Thành viên thường)
    @Transactional
    public void leaveGroup(String groupId) {
        User me = getCurrentUser();
        GroupSpace group = groupSpaceRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Nhóm không tồn tại!"));

        if (group.getOwner().getId().equals(me.getId())) {
            throw new RuntimeException("Chủ nhóm không được rời, chỉ có thể giải tán nhóm!");
        }

        if (!groupSpaceRepository.existsByIdAndMembersContaining(groupId, me)) {
            throw new IllegalArgumentException("Homie không phải thành viên nhóm này!");
        }

        group.getMembers().remove(me);
        groupSpaceRepository.save(group);
    }


}