package com.example.rental_management.payment.controller;

import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.payment.dto.RentPaymentResponse;
import com.example.rental_management.payment.entity.RentPayment;
import com.example.rental_management.payment.service.BillingService;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final BillingService billingService;

    public PaymentController(BillingService billingService) {
        this.billingService = billingService;

    }

    @PostMapping("/leases/{leaseId}")
    public ResponseEntity<RentPaymentResponse> createRentObligation(@PathVariable Long leaseId, @RequestParam LocalDate dueDate){
        RentPayment payment=billingService.createRentObligation(leaseId,dueDate);

        return ResponseEntity.ok(billingService.toResponse(payment));
    }
    @GetMapping("/leases/{leaseId}")
    public ResponseEntity<List<RentPaymentResponse>> getLeaseLedger(@PathVariable Long leaseId){
        return ResponseEntity.ok(billingService.getLeaseLedger(leaseId).stream().map(billingService::toResponse).toList());

    }
    @GetMapping("/my")
    public ResponseEntity<List<RentPaymentResponse>> getMyLedger() {

        Long tenantId = SecurityUtil.getCurrentUserId();

        return ResponseEntity.ok(
                billingService.getTenantLedger(tenantId).stream().map(billingService::toResponse).toList()
        );
    }
    @PutMapping("/{paymentId}/pay")
    public ResponseEntity<RentPaymentResponse> markAsPaid(
            @PathVariable Long paymentId,@RequestParam BigDecimal amountPaid){
        RentPayment payment=billingService.markAsPaid(paymentId,amountPaid);

        return ResponseEntity.ok(billingService.toResponse(payment));
    }
    @GetMapping("/{paymentId}/late-fee")
    public ResponseEntity<BigDecimal> getLateFee(@PathVariable Long paymentId){
        return ResponseEntity.ok(billingService.calculateLateFee(paymentId));
    }



    }

