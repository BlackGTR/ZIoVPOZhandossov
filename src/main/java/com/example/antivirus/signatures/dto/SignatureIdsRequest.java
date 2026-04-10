package com.example.antivirus.signatures.dto;

import java.util.List;
import java.util.UUID;

public class SignatureIdsRequest {

    private List<UUID> ids;

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }
}
