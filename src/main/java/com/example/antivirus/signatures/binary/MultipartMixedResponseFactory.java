package com.example.antivirus.signatures.binary;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

@Component
public class MultipartMixedResponseFactory {

    public ResponseEntity<MultiValueMap<String, Object>> create(BinarySignatureBundle bundle) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("manifest", part(bundle.manifest(), "manifest.bin"));
        body.add("data", part(bundle.data(), "data.bin"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("multipart/mixed"))
                .body(body);
    }

    private HttpEntity<byte[]> part(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(bytes.length);
        return new HttpEntity<>(bytes, headers);
    }
}
