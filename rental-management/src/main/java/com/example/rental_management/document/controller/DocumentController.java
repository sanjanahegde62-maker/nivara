package com.example.rental_management.document.controller;

import com.example.rental_management.document.service.DocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Document generation endpoint.
 *
 * GET /leases/{leaseId}/document
 *   Returns the lease agreement as a PDF file.
 *   JWT authentication required (enforced by SecurityConfig).
 *   Row-level authorization enforced in DocumentService.
 */
@RestController
@RequestMapping("/leases")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Generate and download a PDF lease document.
     *
     * @param leaseId the lease to generate a document for
     * @return the PDF bytes with Content-Type: application/pdf
     */
    @GetMapping("/{leaseId}/document")
    public ResponseEntity<byte[]> getLeasePdf(@PathVariable Long leaseId) {
        byte[] pdf = documentService.generateLeasePdf(leaseId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"lease-" + leaseId + ".pdf\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }
}
