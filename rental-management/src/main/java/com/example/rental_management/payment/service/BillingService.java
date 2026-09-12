package com.example.rental_management.payment.service;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.auth.security.SecurityUtil;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.payment.dto.RentPaymentResponse;
import com.example.rental_management.payment.entity.RentPayment;
import com.example.rental_management.payment.repository.RentPaymentRepository;
import com.example.rental_management.property.entity.PropertyManager;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BillingService {

    private final RentPaymentRepository rentPaymentRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyManagerRepository propertyManagerRepository;

    public BillingService(
            RentPaymentRepository rentPaymentRepository,
            LeaseRepository leaseRepository,
            PropertyManagerRepository propertyManagerRepository) {

        this.rentPaymentRepository = rentPaymentRepository;
        this.leaseRepository = leaseRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    public RentPayment createRentObligation(
            Long leaseId,
            LocalDate dueDate) {

        Lease lease = getLease(leaseId);

        authorizeLeaseAccess(lease);

        if (!"ACTIVE".equals(lease.getStatus())) {
            throw new IllegalStateException(
                    "rent can only be created for an active lease");
        }
        if(dueDate.isBefore(lease.getStartDate()) || dueDate.isAfter(lease.getEndDate())){
    throw new IllegalStateException("due date must fall within the lease period");
        }

        RentPayment payment = new RentPayment();
if(rentPaymentRepository.existsByLeaseIdAndDueDate(leaseId,dueDate)){
    throw new IllegalStateException("rent obligation already exists for this due date");
}
        payment.setLease(lease);
        payment.setDueDate(dueDate);
        payment.setAmountDue(lease.getMonthlyRent());
        payment.setStatus("DUE");

        return rentPaymentRepository.save(payment);
    }

    public List<RentPayment> getLeaseLedger(Long leaseId) {

        Lease lease = getLease(leaseId);

        authorizeLeaseAccess(lease);

        return rentPaymentRepository.findByLeaseId(leaseId);
    }

    public List<RentPayment> getTenantLedger(Long tenantId) {

        Long currentUserId = SecurityUtil.getCurrentUserId();

        if (!currentUserId.equals(tenantId)) {
            throw new RuntimeException(
                    "user is not authorized to view this tenant ledger");
        }

        return rentPaymentRepository.findByLeaseTenantId(tenantId);
    }

    public RentPayment markAsPaid(
            Long paymentId,
            BigDecimal amountPaid) {

        RentPayment payment = rentPaymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("rent payment not found"));

        Lease lease = payment.getLease();

        Long currentUserId = SecurityUtil.getCurrentUserId();

        if (!currentUserId.equals(lease.getTenant().getId())) {
            throw new RuntimeException(
                    "user is not authorized to make this payment");
        }

        if ("PAID".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "rent payment is already marked as paid");
        }if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount paid must be greater than zero");
        }

        BigDecimal previousAmountPaid =
                payment.getAmountPaid() == null
                        ? BigDecimal.ZERO
                        : payment.getAmountPaid();

        BigDecimal totalAmountPaid =
                previousAmountPaid.add(amountPaid);

        if (totalAmountPaid.compareTo(payment.getAmountDue()) > 0) {
            throw new IllegalStateException("total amount paid cannot exceed amount due");
        }

        payment.setAmountPaid(totalAmountPaid);

        payment.setPaidDate(LocalDate.now());

        if (totalAmountPaid.compareTo(payment.getAmountDue()) == 0) {
            payment.setStatus("PAID");
        } else {
            payment.setStatus("PARTIALLY_PAID");
        }

        return rentPaymentRepository.save(payment);
    }

    private Lease getLease(Long leaseId) {

        return leaseRepository.findById(leaseId)
                .orElseThrow(() ->
                        new RuntimeException("lease not found"));
    }

    private void authorizeLeaseAccess(Lease lease) {

        Long currentUserId = SecurityUtil.getCurrentUserId();
        String role = SecurityUtil.getCurrentUserRole();

        if ("TENANT".equals(role)) {

            if (!currentUserId.equals(lease.getTenant().getId())) {
                throw new RuntimeException(
                        "user is not authorized to access this lease");
            }

            return;
        }

        if ("OWNER".equals(role)) {

            if (!currentUserId.equals(
                    lease.getUnit().getProperty().getOwner().getId())) {

                throw new RuntimeException(
                        "owner is not authorized to access this lease");
            }

            return;
        }

        if ("MANAGER".equals(role)) {

            Long propertyId =
                    lease.getUnit().getProperty().getId();

            PropertyManager assignment =
                    propertyManagerRepository
                            .findByPropertyIdAndManagerId(
                                    propertyId,
                                    currentUserId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "manager is not assigned to this property"));

            if (!assignment.getManager().getId().equals(currentUserId)) {
                throw new RuntimeException(
                        "manager is not authorized to access this lease");
            }

            return;
        }

        throw new RuntimeException(
                "user role is not authorized for billing operations");
    }
    public List<RentPayment> getOverduePayments(){
        return rentPaymentRepository.findByStatusAndDueDateBefore("DUE",LocalDate.now());
    }
    public void markOverduePayments() {

        List<RentPayment> overduePayments =
                rentPaymentRepository.findPaymentsToMarkOverdue(
                        LocalDate.now()
                );

        for (RentPayment payment : overduePayments) {

            payment.setStatus("OVERDUE");

            long overdueDays =
                    java.time.temporal.ChronoUnit.DAYS.between(
                            payment.getDueDate(),
                            LocalDate.now()
                    );

            BigDecimal amountPaid =
                    payment.getAmountPaid() == null
                            ? BigDecimal.ZERO
                            : payment.getAmountPaid();

            BigDecimal outstandingAmount =
                    payment.getAmountDue().subtract(amountPaid);

            BigDecimal lateFee =
                    outstandingAmount
                            .multiply(new BigDecimal("0.01"))
                            .multiply(BigDecimal.valueOf(overdueDays));

            payment.setLateFee(lateFee);
        }

        rentPaymentRepository.saveAll(overduePayments);
    }

    public BigDecimal calculateLateFee(Long paymentId){
        RentPayment payment=rentPaymentRepository.findById(paymentId)
                .orElseThrow(()->new RuntimeException("rent payment not found"));
        authorizeLeaseAccess(payment.getLease());
        if("PAID".equals(payment.getStatus())){
            return BigDecimal.ZERO;
        }

        LocalDate today=LocalDate.now();
        if(!today.isAfter(payment.getDueDate())){
            return BigDecimal.ZERO;
        }
        long overdueDays =
                ChronoUnit.DAYS.between(
                        payment.getDueDate(),
                        today
                );

        BigDecimal amountPaid =
                payment.getAmountPaid() == null
                        ? BigDecimal.ZERO
                        : payment.getAmountPaid();

        BigDecimal outstandingAmount =
                payment.getAmountDue().subtract(amountPaid);

        if (outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailyRate =
                new BigDecimal("0.01");

        return outstandingAmount
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(overdueDays));
    }

        public RentPaymentResponse toResponse(RentPayment payment) {
            BigDecimal amountPaid =
                    payment.getAmountPaid() == null
                            ? BigDecimal.ZERO
                            : payment.getAmountPaid();

            BigDecimal remainingAmount =
                    payment.getAmountDue().subtract(amountPaid);
        return new RentPaymentResponse(
                payment.getId(),
                payment.getLease().getId(),
                payment.getDueDate(),
                payment.getPaidDate(),
                payment.getAmountDue(),
                payment.getAmountPaid(),
                remainingAmount,
                payment.getLateFee(),
                payment.getStatus()
        );
    }
}