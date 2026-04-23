package com.example.antivirus.signatures.binary;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class BinaryEncoding {

    private BinaryEncoding() {
    }

    static void writeU8(DataOutputStream out, int value) throws IOException {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("uint8 out of range: " + value);
        }
        out.writeByte(value);
    }

    static void writeU16(DataOutputStream out, int value) throws IOException {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("uint16 out of range: " + value);
        }
        out.writeShort(value);
    }

    static void writeU32(DataOutputStream out, long value) throws IOException {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("uint32 out of range: " + value);
        }
        out.writeInt((int) value);
    }

    static void writeI64(DataOutputStream out, long value) throws IOException {
        out.writeLong(value);
    }

    static void writeUuid(DataOutputStream out, UUID id) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    static void writeStringU32(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        writeU32(out, bytes.length);
        out.write(bytes);
    }

    static void writeBytesU32(DataOutputStream out, byte[] value) throws IOException {
        byte[] bytes = value == null ? new byte[0] : value;
        writeU32(out, bytes.length);
        out.write(bytes);
    }
}
