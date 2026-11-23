package com.example.notification_service.repository;

import com.example.notification_service.model.NotifLogsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifLogsRepository extends JpaRepository<NotifLogsModel, Long> {
}
