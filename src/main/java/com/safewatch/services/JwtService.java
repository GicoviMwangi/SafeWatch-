package com.safewatch.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final String secretKey;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secretKey,@Value("${jwt.expiration-ms}") long expiration) {
        this.secretKey = secretKey;
        this.expiration = expiration;
    }

    private SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        List<Map<String, String>> authorities = userDetails.getAuthorities().stream()
                .map(auth -> {
                    Map<String, String> authMap = new HashMap<>();
                    authMap.put("authority", auth.getAuthority());
                    return authMap;
                }).toList();

        return Jwts.builder()
                .claims()
                .add("authorities", authorities)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .and()
                .signWith(getKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsTFunction) {
        final Claims claim = extractAllClaims(token);
        return claimsTFunction.apply(claim);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpired(String token) {
        return expiration(token).before(new Date());
    }

    private Date expiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean validateToken(UserDetails userDetails, String token) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Collection<? extends GrantedAuthority> extractToken(String token) {
        Claims claims = extractAllClaims(token);
        List<Map<String, String>> authorities = claims.get("authorities", List.class);

        if (authorities == null) return Collections.emptyList();

        return authorities.stream()
                .map(auth -> new SimpleGrantedAuthority(auth.get("authority")))
                .collect(Collectors.toList());
    }
}
