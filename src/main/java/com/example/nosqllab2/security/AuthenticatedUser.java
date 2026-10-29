package com.example.nosqllab2.security;

import java.util.List;

// аналог JwtPayload, что было в NestJS
public record AuthenticatedUser(
        Long id,
        String username,
        List<String> roles
) {
}
