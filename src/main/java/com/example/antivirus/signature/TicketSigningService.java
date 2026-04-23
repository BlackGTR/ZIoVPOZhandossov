package com.example.antivirus.signature;

import org.springframework.stereotype.Service;

import java.security.Signature;
import java.util.Base64;


@Service
public class TicketSigningService {

    private final SignatureKeyStoreService keyStoreService;
    private final JsonCanonicalizer canonicalizer;
    private final SignatureProperties properties;

    public TicketSigningService(SignatureKeyStoreService keyStoreService,
                                JsonCanonicalizer canonicalizer,
                                SignatureProperties properties) {
        this.keyStoreService = keyStoreService;
        this.canonicalizer = canonicalizer;
        this.properties = properties;
    }

    public String sign(Object payload) {
        try {
            byte[] canonical = canonicalizer.canonicalBytes(payload);
            return Base64.getEncoder().encodeToString(signBytes(canonical));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign payload", e);
        }
    }

    /**
     * Подписывает уже готовые байты (без канонизации). Нужен для подписи бинарного манифеста.
     */
    public byte[] signBytes(byte[] payloadBytes) {
        try {
            Signature signature = Signature.getInstance(properties.getAlgorithm());
            signature.initSign(keyStoreService.getPrivateKey());
            signature.update(payloadBytes);
            return signature.sign();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign raw bytes", e);
        }
    }

    /**
     * Проверка целостности: подпись должна соответствовать каноническому JSON полезной нагрузки.
     */
    public boolean verify(Object payload, String signatureBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        try {
            byte[] canonical = canonicalizer.canonicalBytes(payload);
            Signature signature = Signature.getInstance(properties.getAlgorithm());
            signature.initVerify(keyStoreService.getCertificate().getPublicKey());
            signature.update(canonical);
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    public String getPublicKeyBase64() {
        return keyStoreService.getPublicKeyBase64();
    }
}
