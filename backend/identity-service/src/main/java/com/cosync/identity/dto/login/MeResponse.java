package com.cosync.identity.dto.login;

import com.cosync.identity.entity.User;

public record MeResponse(
        Long userId,
        String displayId,
        String firstName,
        String lastName,
        String email,
        String phone
) {

    /** แปลง User (Entity) เป็น MeResponse (DTO) */
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                "U-%06d".formatted(user.getId()),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber());
    }
}