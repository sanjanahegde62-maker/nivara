package com.example.rental_management.property;

import com.example.rental_management.auth.service.JwtService;
import com.example.rental_management.lease.entity.Lease;
import com.example.rental_management.property.entity.Property;
import com.example.rental_management.property.entity.PropertyManager;
import com.example.rental_management.property.entity.PropertyManagerId;
import com.example.rental_management.property.repository.PropertyManagerRepository;
import com.example.rental_management.property.repository.PropertyRepository;
import com.example.rental_management.support.TestFixtures;
import com.example.rental_management.user.entity.Role;
import com.example.rental_management.user.entity.User;
import com.example.rental_management.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Property detail, manager listing, unit listing, and user directory tests.
 *
 * Covers:
 * - GET /properties/{id} returns PropertyDetailResponse with units + managers + stats
 * - GET /properties/{id}/managers lists assigned managers
 * - GET /users?role=MANAGER returns manager directory (OWNER only)
 * - Manager cannot access user directory
 * - Tenant cannot access property detail
 * - GET /properties/{propertyId}/units works for both OWNER and MANAGER
 * - Manager cannot list units for a property they are not assigned to
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PropertyDetailTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired TestFixtures fixtures;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired PropertyManagerRepository propertyManagerRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String jwt(Long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return jwtService.generateToken(u);
    }

    private User saveUser(Role role, String suffix) {
        User u = new User();
        u.setName(role + "-" + suffix);
        u.setEmail(role.name().toLowerCase() + "-" + suffix + "@test.example");
        u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
        u.setRole(role);
        u.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return userRepository.save(u);
    }

    private PropertyManager assignManager(Property property, User manager) {
        PropertyManagerId pmId = new PropertyManagerId(property.getId(), manager.getId());
        PropertyManager pm = new PropertyManager();
        pm.setId(pmId);
        pm.setProperty(property);
        pm.setManager(manager);
        pm.setAssignedAt(LocalDateTime.now());
        return propertyManagerRepository.save(pm);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD1 — GET /properties/{id} returns full detail with units and stat counts
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void propertyDetail_returnsUnitsAndStats() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long ownerId  = lease.getUnit().getProperty().getOwner().getId();
        Long propId   = lease.getUnit().getProperty().getId();
        String token  = jwt(ownerId, Role.OWNER);

        mockMvc.perform(get("/properties/" + propId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(propId))
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.totalUnits").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.occupiedUnits").isNumber())
                .andExpect(jsonPath("$.vacantUnits").isNumber())
                .andExpect(jsonPath("$.units").isArray())
                .andExpect(jsonPath("$.managers").isArray());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD2 — GET /properties/{id} includes managers list
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void propertyDetail_includesAssignedManager() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long ownerId  = lease.getUnit().getProperty().getOwner().getId();
        Long propId   = lease.getUnit().getProperty().getId();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager  = saveUser(Role.MANAGER, suffix);
        Property property = propertyRepository.findById(propId).orElseThrow();
        assignManager(property, manager);

        String token = jwt(ownerId, Role.OWNER);
        mockMvc.perform(get("/properties/" + propId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managers", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.managers[0].managerId").isNumber())
                .andExpect(jsonPath("$.managers[0].managerName").isNotEmpty())
                .andExpect(jsonPath("$.managers[0].managerEmail").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD3 — Manager can view detail for their assigned property
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void manager_canViewDetailForAssignedProperty() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long propId   = lease.getUnit().getProperty().getId();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager  = saveUser(Role.MANAGER, suffix);
        Property property = propertyRepository.findById(propId).orElseThrow();
        assignManager(property, manager);

        String token = jwt(manager.getId(), Role.MANAGER);
        mockMvc.perform(get("/properties/" + propId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(propId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD4 — Tenant cannot access property detail (403)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void tenant_cannotAccessPropertyDetail() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long propId    = lease.getUnit().getProperty().getId();
        Long tenantId  = lease.getTenant().getId();

        String token = jwt(tenantId, Role.TENANT);
        mockMvc.perform(get("/properties/" + propId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD5 — GET /properties/{id}/managers returns list
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void listManagers_returnsAssignedManagers() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long ownerId  = lease.getUnit().getProperty().getOwner().getId();
        Long propId   = lease.getUnit().getProperty().getId();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager  = saveUser(Role.MANAGER, suffix);
        Property property = propertyRepository.findById(propId).orElseThrow();
        assignManager(property, manager);

        String token = jwt(ownerId, Role.OWNER);
        mockMvc.perform(get("/properties/" + propId + "/managers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].managerId").value(manager.getId()))
                .andExpect(jsonPath("$[0].managerName").value(manager.getName()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD6 — GET /users?role=MANAGER available to OWNER
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void userDirectory_ownerCanListManagers() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User owner   = saveUser(Role.OWNER,   "owner-" + suffix);
        User manager = saveUser(Role.MANAGER, "mgr-"   + suffix);

        String token = jwt(owner.getId(), Role.OWNER);
        mockMvc.perform(get("/users?role=MANAGER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // password hash must NOT appear
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[*].token").doesNotExist());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD7 — Manager cannot access the user directory (403)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void userDirectory_managerIsDenied() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager = saveUser(Role.MANAGER, suffix);

        String token = jwt(manager.getId(), Role.MANAGER);
        mockMvc.perform(get("/users?role=MANAGER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD8 — GET /properties/{id}/units works for MANAGER (assigned property)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void manager_canListUnitsForAssignedProperty() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long propId   = lease.getUnit().getProperty().getId();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User manager  = saveUser(Role.MANAGER, suffix);
        Property property = propertyRepository.findById(propId).orElseThrow();
        assignManager(property, manager);

        String token = jwt(manager.getId(), Role.MANAGER);
        mockMvc.perform(get("/properties/" + propId + "/units")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PD9 — Manager cannot list units for a property they are not assigned to
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void manager_cannotListUnitsForUnassignedProperty() throws Exception {
        Lease lease = fixtures.createActiveLease();
        Long propId   = lease.getUnit().getProperty().getId();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User unrelated = saveUser(Role.MANAGER, suffix);

        String token = jwt(unrelated.getId(), Role.MANAGER);
        mockMvc.perform(get("/properties/" + propId + "/units")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(not(equalTo(200))));
    }
}
