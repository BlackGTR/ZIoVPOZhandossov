package com.example.antivirus.signatures.binary;

import com.example.antivirus.signatures.dto.SignatureIdsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureController {

    private final BinarySignatureExportService exportService;
    private final MultipartMixedResponseFactory responseFactory;

    public BinarySignatureController(BinarySignatureExportService exportService,
                                     MultipartMixedResponseFactory responseFactory) {
        this.exportService = exportService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/full")
    public ResponseEntity<MultiValueMap<String, Object>> full() {
        return responseFactory.create(exportService.exportFull());
    }

    @GetMapping("/increment")
    public ResponseEntity<MultiValueMap<String, Object>> increment(@RequestParam("since") String sinceParam) {
        try {
            Instant since = Instant.parse(sinceParam);
            return responseFactory.create(exportService.exportIncrement(since));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter since must be a valid ISO-8601 instant");
        }
    }

    @PostMapping("/by-ids")
    public ResponseEntity<MultiValueMap<String, Object>> byIds(@RequestBody(required = false) SignatureIdsRequest request) {
        List<java.util.UUID> ids = (request == null || request.getIds() == null) ? List.of() : request.getIds();
        return responseFactory.create(exportService.exportByIds(ids));
    }
}
