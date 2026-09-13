package com.swu.smartgrade.user.dto;

public record LoginResponse(String token, UserView user) {
}
