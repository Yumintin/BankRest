package com.example.bankcards.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CipherUtil {
    @Value("${cipher.key}")
    private String keyHex;
    private SecretKeySpec keySpec;
    @PostConstruct
    public void init() {
        byte[] keyBytes = keyHex.getBytes(StandardCharsets.UTF_8);
        byte[] k = new byte[32];
        System.arraycopy(keyBytes, 0, k, 0, Math.min(keyBytes.length, k.length));
        keySpec = new SecretKeySpec(k, "AES");
    }
    public String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv,0,out,0,iv.length);
            System.arraycopy(encrypted,0,out,iv.length,encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    public String decrypt(String cipherText) {
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[12];
            System.arraycopy(all,0,iv,0,12);
            byte[] enc = new byte[all.length - 12];
            System.arraycopy(all,12,enc,0,enc.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            byte[] dec = cipher.doFinal(enc);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    public String mask(String plainCardNumber) {
        if (plainCardNumber == null) return null;
        String digits = plainCardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "****";
        String last4 = digits.substring(digits.length()-4);
        return "**** **** **** " + last4;
    }
}
