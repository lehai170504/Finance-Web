package com.homie.finance.service;

import com.homie.finance.entity.GroupSpace;
import com.homie.finance.entity.Notification;
import com.homie.finance.entity.User;
import com.homie.finance.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    @Autowired private NotificationRepository notificationRepository;

    public void sendToGroup(GroupSpace group, String message, User actor) {
        List<Notification> notis = group.getMembers().stream()
                .filter(m -> !m.getId().equals(actor.getId())) // Không gửi thông báo cho chính người làm
                .map(m -> new Notification(m, message))
                .collect(Collectors.toList());
        notificationRepository.saveAll(notis);
    }

    public void sendToUser(User user, String message) {
        Notification noti = new Notification(user, message);
        notificationRepository.save(noti);
    }
}