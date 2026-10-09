package com.example.rental_management.document;

import com.example.rental_management.billing.exception.BillingAccessDeniedException;
import com.example.rental_management.billing.exception.ResourceNotFoundException;
import com.example.rental_management.document.service.DocumentService;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.support.TestFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Phase 10 — Document (PDF) generation.
 *
 * Tests run against the real PostgreSQL database.
 * All tests are @Transactional so no data is committed.
 *
 * Uses pre-existing lease 1 as the canonical fixture
 * (same convention as BillingIntegrationTest and ScheduledJobIntegrationTest).
 *
 * Scenarios:
 *  1.  Tenant can download their own lease PDF
 *  2.  Tenant cannot download another tenant's lease
 *  3.  Owner can download lease for owned property
 *  4.  Owner cannot download an unrelated lease
 *  5.  Manager cannot download unrelated property lease
 *  6.  Maintenance staff is denied
 *  7.  Nonexistent lease returns ResourceNotFoundException
 *  8.  Response is non-empty
 *  9.  Response starts with %PDF (valid PDF header)
 * 10.  Response Content-Type is application/pdf (verified via controller response)
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentIntegrationTest {

    @Autowired DocumentService documentService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired TestFixtures fixtures;

    // ── Security helpers ──────────────────────────────────────────────────────

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    private Lease activeLease() {
        return fixtures.createActiveLease();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — Tenant can download their own lease PDF
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void tenant_canDownloadOwnLeasePdf() {
        Lease lease = activeLease();
        Long tenantId = lease.getTenant().getId();

        authenticate(tenantId, "TENANT");
        byte[] pdf = documentService.generateLeasePdf(lease.getId());

        assertThat(pdf).isNotEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — Tenant cannot download another tenant's lease
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    void tenant_cannotDownloadAnotherTenantsLease() {
        Lease lease = activeLease();
        Long impostorId = lease.getTenant().getId() + 9999L;

        authenticate(impostorId, "TENANT");
        assertThatThrownBy(() -> documentService.generateLeasePdf(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3 — Owner can download lease for owned property
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    void owner_canDownloadLeaseForOwnedProperty() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        authenticate(ownerId, "OWNER");
        byte[] pdf = documentService.generateLeasePdf(lease.getId());

        assertThat(pdf).isNotEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 4 — Owner cannot download an unrelated lease
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    void owner_cannotDownloadUnrelatedLease() {
        Lease lease = activeLease();
        Long unrelatedOwnerId = lease.getUnit().getProperty().getOwner().getId() + 9999L;

        authenticate(unrelatedOwnerId, "OWNER");
        assertThatThrownBy(() -> documentService.generateLeasePdf(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 5 — Manager cannot download unrelated property lease
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    void manager_cannotDownloadUnrelatedPropertyLease() {
        Lease lease = activeLease();
        Long unrelatedManagerId = 99999L; // no assignment in test DB

        authenticate(unrelatedManagerId, "MANAGER");
        assertThatThrownBy(() -> documentService.generateLeasePdf(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 6 — Maintenance staff is denied
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void maintenanceStaff_isDenied() {
        Lease lease = activeLease();

        authenticate(42L, "MAINTENANCE_STAFF");
        assertThatThrownBy(() -> documentService.generateLeasePdf(lease.getId()))
                .isInstanceOf(BillingAccessDeniedException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 7 — Nonexistent lease returns ResourceNotFoundException
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    void nonexistentLease_throwsResourceNotFoundException() {
        authenticate(1L, "OWNER");
        assertThatThrownBy(() -> documentService.generateLeasePdf(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("lease not found");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 8 — Response is non-empty
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    void pdf_isNonEmpty() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        authenticate(ownerId, "OWNER");
        byte[] pdf = documentService.generateLeasePdf(lease.getId());

        assertThat(pdf.length).isGreaterThan(100);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 9 — Response starts with %PDF (valid PDF header)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void pdf_hasValidPdfHeader() {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        authenticate(ownerId, "OWNER");
        byte[] pdf = documentService.generateLeasePdf(lease.getId());

        // PDF files always start with "%PDF-"
        String header = new String(pdf, 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 10 — PDF contains lease data (spot-check tenant name)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    void pdf_containsLeaseData() throws Exception {
        Lease lease = activeLease();
        Long ownerId = lease.getUnit().getProperty().getOwner().getId();

        authenticate(ownerId, "OWNER");
        byte[] pdf = documentService.generateLeasePdf(lease.getId());

        // Extract readable text from the PDF using PDFBox — raw bytes are binary
        // and must not be searched with new String(bytes).
        String pdfText;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            pdfText = new PDFTextStripper().getText(doc);
        }

        // The lease ID and tenant name must appear in the extracted text.
        assertThat(pdfText).contains(String.valueOf(lease.getId()));
        assertThat(pdfText).contains(lease.getTenant().getName());
    }
}
