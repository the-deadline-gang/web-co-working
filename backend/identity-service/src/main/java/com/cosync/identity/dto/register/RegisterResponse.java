package com.cosync.identity.dto.register;

import java.time.Instant;

public record RegisterResponse(Long userId, Instant otpExpiresAt) {
}