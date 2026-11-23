package com.example.notification_service.event;

public record UserRegistrationEvent(
        String eventId,
        String eventType,
        String createdAt,
        String createdBy,
        Data data
) {
    public record Data(
            String id,
            String fullName,
            String email,
            Integer roleId
    ) {}
}