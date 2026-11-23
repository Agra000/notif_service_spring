package com.example.notification_service.listener;

import com.example.notification_service.event.UserRegistrationEvent;
import com.example.notification_service.model.NotifLogsModel;
import com.example.notification_service.repository.NotifLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistListener {

    private final NotifLogsRepository notifLogsRepository;

    @RabbitListener(queues = "${app.rabbit.queue}")
    public void OnUserRegistered(UserRegistrationEvent event) {
        try {

            if (!"user_registration".equals(event.eventType())) return;
    //        if (notifLogsRepository.findById(event.eventId())) return;

            if (event.data() == null || event.data().email() == null) {
                return;
            }

    //        String to = event.data().email();
            String body = buildBody(event.data());
            Instant registeredAt = Instant.parse(event.createdAt());

            NotifLogsModel notifLog = new NotifLogsModel();
            notifLog.setNotif_type(event.eventType());
            notifLog.setNotif_body(body);
            notifLog.setCreated_at(LocalDateTime.ofInstant(registeredAt, ZoneOffset.UTC));

    //        try {
    //            emailService.send(to, subject, body);
    //            notifLog.setStatus("SENT");
    //        } catch (Exception e) {
    //            notifLog.setStatus("FAILED");
    //            notifLog.setErrorMessage(e.getMessage());
    //        }

            notifLogsRepository.save(notifLog);
        } catch (Exception e){
            throw new RuntimeException();
        }
    }

    private String buildBody(UserRegistrationEvent.Data data) {
        String name = data.fullName() != null ? data.fullName() : "Pengguna";
        return """
                Halo %s,

                Your account already registed by admin.

                Email login: %s

                Please login with your email and password.

                Regards,
                Team
                """.formatted(name, data.email());
    }
}
