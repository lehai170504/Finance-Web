package com.homie.finance.repository;

import com.homie.finance.entity.GroupSpace;
import com.homie.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupSpaceRepository extends JpaRepository<GroupSpace, String> {
    // Tìm các nhóm mà User này là thành viên
    List<GroupSpace> findByMembersContaining(User user);

    // Tìm nhóm bằng mã mời
    Optional<GroupSpace> findByInviteCode(String inviteCode);
}