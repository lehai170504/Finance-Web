package com.homie.finance.service;

import com.homie.finance.entity.GroupSpace;
import com.homie.finance.entity.User;
import com.homie.finance.repository.GroupSpaceRepository;
import com.homie.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GroupSpaceService {

    @Autowired private GroupSpaceRepository groupSpaceRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    // 1. Tạo nhóm mới
    public GroupSpace createGroup(String groupName) {
        User me = getCurrentUser();

        GroupSpace group = new GroupSpace();
        group.setName(groupName);
        group.setOwner(me);

        // Tạo mã code ngẫu nhiên 6 chữ số (Cắt từ UUID cho nhanh và đỡ trùng)
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        group.setInviteCode(code);

        // Thêm chính mình vào danh sách thành viên đầu tiên
        group.getMembers().add(me);

        return groupSpaceRepository.save(group);
    }

    // 2. Tham gia nhóm bằng Mã Code
    public GroupSpace joinGroup(String inviteCode) {
        User me = getCurrentUser();

        GroupSpace group = groupSpaceRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Mã mời không hợp lệ hoặc nhóm không tồn tại!"));

        // Kiểm tra xem đã trong nhóm chưa
        if (group.getMembers().contains(me)) {
            throw new IllegalArgumentException("Homie đã ở trong nhóm này rồi!");
        }

        group.getMembers().add(me);
        return groupSpaceRepository.save(group);
    }

    // 3. Lấy danh sách nhóm của tôi
    public List<GroupSpace> getMyGroups() {
        return groupSpaceRepository.findByMembersContaining(getCurrentUser());
    }
}