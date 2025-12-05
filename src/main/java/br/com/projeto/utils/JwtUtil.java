package br.com.projeto.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtUtil {

    @ConfigProperty(name = "app.jwt.secret", defaultValue = "dev-secret")
    String secret;
    
    // Log temporário para debug
    @jakarta.annotation.PostConstruct
    void init() {
        System.out.println("🔑 JWT Secret configurado: " + (secret != null ? secret.substring(0, Math.min(3, secret.length())) + "***" : "NULL"));
    }

    public String createToken(UUID id, String username, String role, long expiresInSeconds){
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        long now = Instant.now().getEpochSecond();
        long exp = now + expiresInSeconds;
        String payloadJson = String.format("{\"id\":\"%s\",\"username\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
            id, escape(username), role, now, exp);
        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha256(header + "." + payload, secret);
        return header + "." + payload + "." + signature;
    }

    public TokenPayload validateToken(String token){
        try{
            if(token == null || token.isBlank()) return null;
            
            String[] parts = token.split("\\.");
            if(parts.length != 3) return null;
            
            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];
            
            // Verificar assinatura
            String expectedSignature = hmacSha256(header + "." + payload, secret);
            if(!signature.equals(expectedSignature)) {
                System.err.println("❌ Token assinatura inválida! Expected: " + expectedSignature.substring(0, 10) + "... Got: " + signature.substring(0, 10) + "...");
                System.err.println("   Secret usado: " + secret.substring(0, 3) + "***");
                return null;
            }
            
            // Decodificar payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            System.out.println("✅ Token válido (assinatura OK), payload: " + payloadJson);
            ObjectMapper mapper = new ObjectMapper();
            TokenPayload tokenPayload = mapper.readValue(payloadJson, TokenPayload.class);
            
            // Verificar expiração
            long now = Instant.now().getEpochSecond();
            if(tokenPayload.exp <= now) {
                System.err.println("❌ Token expirado! exp=" + tokenPayload.exp + " now=" + now + " diff=" + (now - tokenPayload.exp) + "s");
                return null;
            }
            
            System.out.println("✅ Token válido e não expirado");
            return tokenPayload;
        } catch(Exception e){
            System.err.println("❌ Erro ao validar token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String escape(String s){
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String base64UrlEncode(byte[] bytes){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hmacSha256(String data, String key){
        try{
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(sig);
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @RegisterForReflection
    public static class TokenPayload {
        public String id;
        public String username;
        public String role;
        public long iat;
        public long exp;
        
        // Construtor padrão necessário para Jackson + GraalVM Native Image
        public TokenPayload() {}
        
        public TokenPayload(String id, String username, String role, long iat, long exp) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.iat = iat;
            this.exp = exp;
        }
    }
}
