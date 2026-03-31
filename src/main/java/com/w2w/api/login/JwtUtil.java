package com.w2w.api.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;

@Component
public class JwtUtil {

    private final ResourceLoader resourceLoader;
    private final String keystorePath;
    private final String keystorePassword;
    private final String keyAlias;
    private final String keyPassword;
    private final long expirationMs;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public JwtUtil(
            ResourceLoader resourceLoader,
            @Value("${jwt.keystore.path}") String keystorePath,
            @Value("${jwt.keystore.password}") String keystorePassword,
            @Value("${jwt.keystore.alias}") String keyAlias,
            @Value("${jwt.key.password}") String keyPassword,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.resourceLoader = resourceLoader;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    public void init() throws Exception {
        Resource resource = resourceLoader.getResource(keystorePath);
        try (InputStream is = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(is, keystorePassword.toCharArray());

            this.privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
            Certificate cert = keyStore.getCertificate(keyAlias);
            this.publicKey = cert.getPublicKey();
        }
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(privateKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
