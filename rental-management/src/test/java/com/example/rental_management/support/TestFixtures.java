package com.example.rental_management.support;

import com.example.rental_management.application.entity.Application;
import com.example.rental_management.application.repository.ApplicationRepository;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.lease.repository.LeaseRepository;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.entity.Unit;
import com.example.rental_management.property.repository.PropertyRepository;
import com.example.rental_management.property.repository.UnitRepository;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared fixture builder for integration tests.
 *
 * <p>Creates a minimal, fully-linked entity graph in the database:
 * <pre>
 *   owner (OWNER) → property → unit (OCCUPIED) → application (APPROVED) → lease (ACTIVE)
 *                                                     ↑
 *                                                 tenant (TENANT)
 * </pre>
 *
 * <p>Every created entity uses a UUID-based unique email / name so concurrent
 * test-class executions and multiple invocations within a single run never
 * collide on UNIQUE constraints.
 *
 * <p>When the calling test is {@code @Transactional} the entire fixture is
 * rolled back automatically after the test.  For non-transactional tests
 * (e.g. {@code ScheduledJobIntegrationTest}) the caller is responsible for
 * cleanup via {@link #deleteAll(Lease)}.
 */
@Component
public class TestFixtures {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final ApplicationRepository applicationRepository;
    private final LeaseRepository leaseRepository;
    private final PasswordEncoder passwordEncoder;

    public TestFixtures(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            UnitRepository unitRepository,
            ApplicationRepository applicationRepository,
            LeaseRepository leaseRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.applicationRepository = applicationRepository;
        this.leaseRepository = leaseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates and persists a complete ACTIVE lease fixture.
     * The lease starts one year ago and ends one year from now so that:
     * - It is currently ACTIVE (not expired).
     * - The RentChargeGenerationJob has an ACTIVE lease to work with.
     */
    public Lease createActiveLease() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // ── Owner ──────────────────────────────────────────────────────────────
        User owner = new User();
        owner.setName("Owner-" + uid);
        owner.setEmail("owner-" + uid + "@test.example");
        owner.setPasswordHash(passwordEncoder.encode("TestPass1!"));
        owner.setRole(Role.OWNER);
        owner.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        owner.setCreatedAt(now);
        owner.setUpdatedAt(now);
        owner = userRepository.save(owner);

        // ── Tenant ─────────────────────────────────────────────────────────────
        User tenant = new User();
        tenant.setName("Tenant-" + uid);
        tenant.setEmail("tenant-" + uid + "@test.example");
        tenant.setPasswordHash(passwordEncoder.encode("TestPass1!"));
        tenant.setRole(Role.TENANT);
        tenant.setStatus("ACTIVE");
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenant = userRepository.save(tenant);

        // ── Property ───────────────────────────────────────────────────────────
        Property property = new Property();
        property.setOwner(owner);
        property.setName("Property-" + uid);
        property.setAddress("123 Test St, City " + uid);
        property.setPropertyType("RESIDENTIAL");
        property.setDescription("Test property " + uid);
        property = propertyRepository.save(property);

        // ── Unit ───────────────────────────────────────────────────────────────
        Unit unit = new Unit();
        unit.setProperty(property);
        unit.setUnitNumber("U-" + uid);
        unit.setUnitType("APARTMENT");
        unit.setMonthlyRent(new BigDecimal("1500.00"));
        unit.setStatus("OCCUPIED");
        unit = unitRepository.save(unit);

        // ── Application (APPROVED) ─────────────────────────────────────────────
        Application application = new Application();
        application.setUnit(unit);
        application.setTenant(tenant);
        application.setStatus("APPROVED");
        application.setAppliedAt(now);
        application.setReviewedAt(now);
        application = applicationRepository.save(application);

        // ── Lease (ACTIVE) ─────────────────────────────────────────────────────
        Lease lease = new Lease();
        lease.setApplication(application);
        lease.setTenant(tenant);
        lease.setUnit(unit);
        lease.setStartDate(LocalDate.now().minusYears(1));
        lease.setEndDate(LocalDate.now().plusYears(1));
        lease.setMonthlyRent(unit.getMonthlyRent());
        lease.setStatus("ACTIVE");
        lease = leaseRepository.save(lease);

        return lease;
    }

    /**
     * Deletes the entire fixture graph created by {@link #createActiveLease()}.
     * Only needed for non-transactional tests that do not benefit from automatic rollback.
     * Deletes in reverse FK order.
     */
    public void deleteAll(Lease lease) {
        Long unitId   = lease.getUnit().getId();
        Long tenantId = lease.getTenant().getId();
        Long ownerId  = lease.getUnit().getProperty().getOwner().getId();
        Long propId   = lease.getUnit().getProperty().getId();

        leaseRepository.deleteById(lease.getId());
        applicationRepository.deleteById(lease.getApplication().getId());
        unitRepository.deleteById(unitId);
        propertyRepository.deleteById(propId);
        userRepository.deleteById(tenantId);
        userRepository.deleteById(ownerId);
    }
}
