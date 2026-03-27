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
            Signature signature = Signature.getInstance(properties.getAlgorithm());
            signature.initSign(keyStoreService.getPrivateKey());
            signature.update(canonical);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign payload", e);
        }
    }

    public String getPublicKeyBase64() {
        return keyStoreService.getPublicKeyBase64();
    }
}
