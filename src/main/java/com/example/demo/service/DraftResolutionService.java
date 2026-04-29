package com.example.demo.service;

import com.example.demo.domain.*;
import com.example.demo.dto.DraftOverrideRequest;
import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.repository.AppUserRepository; // <-- ADDED IMPORT
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DraftOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DraftResolutionService {

    private final DraftOrderRepo draftOrderRepo;
    private final ContactRepo contactRepo;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final SseService sseService;

    // <-- ADDED INJECTION so we can securely authenticate the admin
    private final AppUserRepository userRepository;

    public List<DraftOrder> getPendingDraftsForContact(String contactId, Long tenantId) {
        return draftOrderRepo.findByContactIdAndTenantIdAndStatus(contactId, tenantId, DraftOrder.DraftStatus.PENDING);
    }

    // ==========================================
    // NEW: DISCARD DRAFT LOGIC
    // ==========================================
    @Transactional
    public void discardDraft(Long draftId, String currentUsername) {
        // 1. Authenticate the admin and get their shop (Tenant)
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long tenantId = currentUser.getTenant().getId();

        // 2. Find the draft securely (ensuring it belongs to this shop)
        DraftOrder draft = draftOrderRepo.findByIdAndTenantId(draftId, tenantId)
                .orElseThrow(() -> new RuntimeException("Draft not found or unauthorized"));

        Contact contact = draft.getContact();

        // 3. Delete the draft from the database (This will cascade and delete items too!)
        draftOrderRepo.delete(draft);

        // 4. IMPORTANT: Clear the UI flag since there is no order waiting anymore
        checkAndClearOrderReadyFlag(contact, tenantId);
    }

    @Transactional
    public void cancelDraft(Long draftId, Long tenantId, String currentUsername) {
        DraftOrder draft = draftOrderRepo.findByIdAndTenantId(draftId, tenantId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        draft.setStatus(DraftOrder.DraftStatus.CANCELLED);
        draftOrderRepo.save(draft);

        checkAndClearOrderReadyFlag(draft.getContact(), tenantId);
    }

    @Transactional
    public void confirmDraft(Long draftId, Long tenantId, String currentUsername, DraftOverrideRequest overrides) {
        DraftOrder draft = draftOrderRepo.findByIdAndTenantId(draftId, tenantId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        if (overrides.getOverridePhone() != null) draft.setProvidedPhone(overrides.getOverridePhone());
        if (overrides.getOverrideAddress() != null) draft.setProvidedAddress(overrides.getOverrideAddress());
        if (overrides.getOverrideDelivery() != null) draft.setDeliveryMethod(overrides.getOverrideDelivery());
        if (overrides.getOverridePayment() != null) draft.setPaymentMethod(overrides.getOverridePayment());

        Contact contact = draft.getContact();
        Tenant tenant = draft.getTenant();

        // --- UPDATED CUSTOMER RESOLUTION LOGIC ---
        Customer customer = contact.getCustomer();
        if (customer == null) {
            String phoneToCheck = draft.getProvidedPhone();

            // Check if the phone number already exists for this tenant
            Optional<Customer> existingCustomer = customerRepository.findByPhoneNumberAndTenantId(phoneToCheck, tenantId);

            if (existingCustomer.isPresent()) {
                // Link the existing customer to this new contact
                customer = existingCustomer.get();
            } else {
                // Safely create a brand new customer
                customer = Customer.builder()
                        .fullName(draft.getProvidedName())
                        .phoneNumber(phoneToCheck)
                        .email(draft.getProvidedEmail())
                        .address(draft.getProvidedAddress())
                        .tenant(tenant)
                        .build();
                customer = customerRepository.save(customer);
            }

            // Link contact to the resolved customer
            contact.setCustomer(customer);
            contactRepo.save(contact);
        }

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setCustomerId(customer.getId());
        orderRequest.setDeliveryMethod(draft.getDeliveryMethod());
        orderRequest.setPaymentMethod(draft.getPaymentMethod());

        // Items are pulled directly from the override payload
        orderRequest.setItems(overrides.getItems());

        orderService.createOrder(currentUsername, orderRequest);

        draft.setStatus(DraftOrder.DraftStatus.CONFIRMED);
        draftOrderRepo.save(draft);

        checkAndClearOrderReadyFlag(contact, tenantId);
    }

    private void checkAndClearOrderReadyFlag(Contact contact, Long tenantId) {
        List<DraftOrder> remainingDrafts = draftOrderRepo.findByContactIdAndTenantIdAndStatus(
                contact.getId(), tenantId, DraftOrder.DraftStatus.PENDING);

        if (remainingDrafts.isEmpty()) {
            contact.setOrderReady(false);
            contactRepo.save(contact);
            sseService.pushContactUpdateToTenant(tenantId, contact);
        }
    }
}