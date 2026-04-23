package com.example.antivirus.signatures.binary;

import com.example.antivirus.signature.TicketSigningService;
import com.example.antivirus.signatures.MalwareSignature;
import com.example.antivirus.signatures.MalwareSignatureRepository;
import com.example.antivirus.signatures.SignatureStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class BinarySignatureExportService {

    private static final int FORMAT_VERSION = 1;

    private final MalwareSignatureRepository signatures;
    private final TicketSigningService signingService;
    private final String magicOwner;

    public BinarySignatureExportService(MalwareSignatureRepository signatures,
                                        TicketSigningService signingService,
                                        @Value("${binary.magic.owner:ALENZ}") String magicOwner) {
        this.signatures = signatures;
        this.signingService = signingService;
        this.magicOwner = magicOwner;
    }

    @Transactional(readOnly = true)
    public BinarySignatureBundle exportFull() {
        List<MalwareSignature> records = signatures.findByStatusOrderByUpdatedAtAsc(SignatureStatus.ACTUAL);
        return buildBundle(records, BinaryExportType.FULL, -1L);
    }

    @Transactional(readOnly = true)
    public BinarySignatureBundle exportIncrement(Instant since) {
        if (since == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Parameter since is required");
        }
        List<MalwareSignature> records = signatures.findForIncrement(since);
        return buildBundle(records, BinaryExportType.INCREMENT, since.toEpochMilli());
    }

    @Transactional(readOnly = true)
    public BinarySignatureBundle exportByIds(List<UUID> ids) {
        List<MalwareSignature> records;
        if (ids == null || ids.isEmpty()) {
            records = List.of();
        } else {
            records = signatures.findByIdIn(ids).stream()
                    .sorted(Comparator.comparing(MalwareSignature::getUpdatedAt)
                            .thenComparing(MalwareSignature::getId))
                    .toList();
        }
        return buildBundle(records, BinaryExportType.BY_IDS, -1L);
    }

    private BinarySignatureBundle buildBundle(List<MalwareSignature> records, BinaryExportType exportType, long sinceEpochMillis) {
        DataBuildResult data = buildDataBin(records);
        byte[] manifest = buildManifestBin(records, data.entries, data.dataBytes, exportType, sinceEpochMillis);
        return new BinarySignatureBundle(manifest, data.dataBytes);
    }

    private DataBuildResult buildDataBin(List<MalwareSignature> records) {
        List<EntryOffset> offsets = new ArrayList<>(records.size());
        try {
            ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(payloadBos);

            long offset = 0L;
            for (MalwareSignature m : records) {
                byte[] recordBytes = encodeDataRecord(m);
                payloadOut.write(recordBytes);
                offsets.add(new EntryOffset(offset, recordBytes.length));
                offset += recordBytes.length;
            }

            ByteArrayOutputStream dataBos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(dataBos);
            BinaryEncoding.writeStringU32(out, "DB-" + magicOwner);
            BinaryEncoding.writeU16(out, FORMAT_VERSION);
            BinaryEncoding.writeU32(out, records.size());
            out.write(payloadBos.toByteArray());

            return new DataBuildResult(dataBos.toByteArray(), offsets);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build data.bin", e);
        }
    }

    private byte[] buildManifestBin(List<MalwareSignature> records,
                                    List<EntryOffset> offsets,
                                    byte[] dataBytes,
                                    BinaryExportType exportType,
                                    long sinceEpochMillis) {
        try {
            ByteArrayOutputStream unsignedBos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(unsignedBos);

            BinaryEncoding.writeStringU32(out, "MF-" + magicOwner);
            BinaryEncoding.writeU16(out, FORMAT_VERSION);
            BinaryEncoding.writeU8(out, exportType.code());
            BinaryEncoding.writeI64(out, Instant.now().toEpochMilli());
            BinaryEncoding.writeI64(out, sinceEpochMillis);
            BinaryEncoding.writeU32(out, records.size());
            out.write(sha256(dataBytes));

            for (int i = 0; i < records.size(); i++) {
                MalwareSignature m = records.get(i);
                EntryOffset off = offsets.get(i);
                byte[] recordSig = decodeBase64Signature(m.getDigitalSignatureBase64(), m.getId());

                BinaryEncoding.writeUuid(out, m.getId());
                BinaryEncoding.writeU8(out, statusCode(m.getStatus()));
                BinaryEncoding.writeI64(out, m.getUpdatedAt().toEpochMilli());
                BinaryEncoding.writeI64(out, off.offset());
                BinaryEncoding.writeU32(out, off.length());
                BinaryEncoding.writeU32(out, recordSig.length);
                out.write(recordSig);
            }

            byte[] unsignedManifest = unsignedBos.toByteArray();
            byte[] manifestSignature = signingService.signBytes(unsignedManifest);

            ByteArrayOutputStream signedBos = new ByteArrayOutputStream();
            DataOutputStream signedOut = new DataOutputStream(signedBos);
            signedOut.write(unsignedManifest);
            BinaryEncoding.writeU32(signedOut, manifestSignature.length);
            signedOut.write(manifestSignature);
            return signedBos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build manifest.bin", e);
        }
    }

    private byte[] encodeDataRecord(MalwareSignature m) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            BinaryEncoding.writeStringU32(out, m.getThreatName());
            BinaryEncoding.writeBytesU32(out, decodeHex(m.getFirstBytesHex(), "firstBytesHex"));
            BinaryEncoding.writeBytesU32(out, decodeHex(m.getRemainderHashHex(), "remainderHashHex"));
            BinaryEncoding.writeI64(out, m.getRemainderLength());
            BinaryEncoding.writeStringU32(out, m.getFileType());
            BinaryEncoding.writeI64(out, m.getOffsetStart());
            BinaryEncoding.writeI64(out, m.getOffsetEnd());
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot encode data record for signature " + m.getId(), e);
        }
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute SHA-256", e);
        }
    }

    private static int statusCode(SignatureStatus status) {
        return status == SignatureStatus.ACTUAL ? 1 : 2;
    }

    private static byte[] decodeBase64Signature(String base64, UUID id) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Missing record signature for id " + id);
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid base64 signature for id " + id, e);
        }
    }

    private static byte[] decodeHex(String hex, String fieldName) {
        if (hex == null) {
            throw new IllegalStateException(fieldName + " is null");
        }
        String value = hex.trim();
        if ((value.length() & 1) == 1) {
            throw new IllegalStateException(fieldName + " must have even hex length");
        }
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(value.charAt(i * 2), 16);
            int lo = Character.digit(value.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalStateException(fieldName + " contains non-hex chars");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private record EntryOffset(long offset, int length) {
    }

    private record DataBuildResult(byte[] dataBytes, List<EntryOffset> entries) {
    }
}
