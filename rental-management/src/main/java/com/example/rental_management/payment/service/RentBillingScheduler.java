package com.example.rental_management.payment.service;

import com.example.rental_management.payment.entity.RentPayment;
import com.example.rental_management.payment.repository.RentPaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class RentBillingScheduler {
    private final BillingService billingService;

    public RentBillingScheduler(BillingService billingService) {
        this.billingService = billingService;
    }


    @Scheduled(cron = "0 0 0 * * *")
    public void markOverduePayments() {
        billingService.markOverduePayments();
    }
}
