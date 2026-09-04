package com.campusmarket.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final Algorithm algorithm;
    private final long expirationMinutes;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMinutes * 60_000);

        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public LoginUser parseToken(String token) {
        DecodedJWT decodedJwt = JWT.require(algorithm).build().verify(token);
        Long userId = Long.valueOf(decodedJwt.getSubject());
        String username = decodedJwt.getClaim("username").asString();
        return new LoginUser(userId, username);
    }

    public long getRemainingSeconds(String token) {
        DecodedJWT decodedJwt = JWT.require(algorithm).build().verify(token);
        long remainingMillis = decodedJwt.getExpiresAt().getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000);
    }
}
