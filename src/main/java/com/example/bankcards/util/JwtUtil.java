package com.example.bankcards.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    private final Key key;
    @Value("${jwt.expirationMs}")
    private long expirationMs;
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] b = Arrays.copyOf(secret.getBytes(), 32);
        this.key = Keys.hmacShaKeyFor(b);
    }
    public String generateToken(String username, Collection<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key)
                .compact();
    }
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
    public String usernameFromToken(String token) {
        return parse(token).getBody().getSubject();
    }
    @SuppressWarnings("unchecked")
    public List<String> rolesFromToken(String token) {
        Object v = parse(token).getBody().get("roles");
        if (v instanceof Collection) {
            return ((Collection<?>)v).stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
