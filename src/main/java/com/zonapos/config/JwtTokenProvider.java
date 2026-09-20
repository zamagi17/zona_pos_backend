package com.zonapos.config;

import com.zonapos.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long ownerManagerExpiration;
    private final long cashierExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.owner-manager-expiration-ms:3600000}") long ownerManagerExpiration,
            @Value("${app.jwt.cashier-expiration-ms:86400000}") long cashierExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.ownerManagerExpiration = ownerManagerExpiration;
        this.cashierExpiration = cashierExpiration;
    }

    public String generateToken(User user) {
        Date now = new Date();
        boolean isCashier = user.getRole() != null && "ROLE_CASHIER".equals(user.getRole().getName());
        long expirationTime = isCashier ? cashierExpiration : ownerManagerExpiration;
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("tenantId", user.getTenantId())
                .claim("outletId", user.getOutletId())
                .claim("role", user.getRole() != null ? user.getRole().getName() : "")
                .claim("name", user.getName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public long getExpirationDuration(User user) {
        boolean isCashier = user.getRole() != null && "ROLE_CASHIER".equals(user.getRole().getName());
        return isCashier ? cashierExpiration : ownerManagerExpiration;
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Number id = (Number) parseClaims(token).get("userId");
        return id != null ? id.longValue() : null;
    }

    public Long getTenantIdFromToken(String token) {
        Number id = (Number) parseClaims(token).get("tenantId");
        return id != null ? id.longValue() : null;
    }

    public Long getOutletIdFromToken(String token) {
        Number id = (Number) parseClaims(token).get("outletId");
        return id != null ? id.longValue() : null;
    }

    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).get("role");
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
