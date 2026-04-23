package com.example.antivirus.signature;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;

/**
 * Key provider для модуля ЭЦП: загрузка PKCS12/JKS, кэш записи с приватным ключом и сертификатом.
 * <p>
 * <b>Когда загружается ключ:</b> не при старте приложения, а при <b>первом</b> вызове
 * {@link #getPrivateKey()}, {@link #getCertificate()} или {@link #getPublicKeyBase64()} — внутри
 * {@link #getEntry()} ленивая инициализация. После успешной загрузки {@link KeyStore.PrivateKeyEntry}
 * хранится в {@link #cachedEntry} и повторно с диска не читается.
 * <p>
 * <b>Пароль ключа:</b> если в {@link SignatureProperties} пустой {@code keyPassword}, используется
 * {@code keyStorePassword} (как в keytool: при отсутствии {@code -keypass} пробуют пароль хранилища).
 * <p>
 * Потокобезопасность: double-checked locking на {@code cachedEntry}; пара ключ/сертификат из одной записи keystore.
 */
@Service
public class SignatureKeyStoreService {

    private final SignatureProperties properties;
    private final ResourceLoader resourceLoader;

    private volatile KeyStore.PrivateKeyEntry cachedEntry;

    public SignatureKeyStoreService(SignatureProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public PrivateKey getPrivateKey() {
        return getEntry().getPrivateKey();
    }

    public Certificate getCertificate() {
        return getEntry().getCertificate();
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(getCertificate().getPublicKey().getEncoded());
    }

    private KeyStore.PrivateKeyEntry getEntry() {
        KeyStore.PrivateKeyEntry entry = cachedEntry;
        if (entry != null) {
            return entry;
        }
        synchronized (this) {
            if (cachedEntry == null) {
                cachedEntry = loadEntry();
            }
            return cachedEntry;
        }
    }

    private KeyStore.PrivateKeyEntry loadEntry() {
        try {
            KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType());
            Resource resource = resourceLoader.getResource(properties.getKeyStorePath());
            try (InputStream is = resource.getInputStream()) {
                keyStore.load(is, properties.getKeyStorePassword().toCharArray());
            }

            String keyPassword = properties.getKeyPassword();
            if (keyPassword == null || keyPassword.isBlank()) {
                keyPassword = properties.getKeyStorePassword();
            }

            KeyStore.ProtectionParameter protection =
                    new KeyStore.PasswordProtection(keyPassword.toCharArray());
            return (KeyStore.PrivateKeyEntry) keyStore.getEntry(properties.getKeyAlias(), protection);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load signing keys from keystore", e);
        }
    }
}
