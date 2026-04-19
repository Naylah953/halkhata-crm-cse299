package com.example.demo.service;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.Contact;
import com.example.demo.domain.Customer;
import com.example.demo.domain.Tenant;
import com.example.demo.dto.CustomerCreateRequest;
import com.example.demo.dto.CustomerDto;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ContactRepo contactRepo;
    private final AppUserRepository userRepository;

    private Tenant getTenantFromUsername(String username) {
        return userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getTenant();
    }

    @Transactional
    public CustomerDto createCustomer(String username, CustomerCreateRequest request) {
        Tenant tenant = getTenantFromUsername(username);

        // 1. Check for duplicates
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            if (customerRepository.existsByPhoneNumberAndTenantId(request.getPhoneNumber(), tenant.getId())) {
                throw new RuntimeException("A customer with this phone number already exists in your shop.");
            }
        }

        // 2. Fetch the Contact early if a contactId was provided
        Contact contact = null;
        if (request.getContactId() != null && !request.getContactId().isEmpty()) {
            contact = contactRepo.findByIdAndTenantId(request.getContactId(), tenant.getId())
                    .orElseThrow(() -> new RuntimeException("Messenger Contact not found or doesn't belong to your shop."));
        }

        // 3. SMART NAME RESOLUTION: Use the provided name, OR fallback to the Facebook Contact name
        String finalName = request.getFullName();
        if (finalName == null || finalName.trim().isEmpty()) {
            if (contact != null) {
                finalName = contact.getName(); // Auto-fill from database!
            } else {
                throw new IllegalArgumentException("Full name is required if you are not linking an existing contact.");
            }
        }

        // 4. Build the new customer
        Customer customer = Customer.builder()
                .fullName(finalName)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .tenant(tenant)
                .build();

        customer = customerRepository.save(customer);

        // 5. Link existing Messenger Contact to this Customer
        if (contact != null) {
            contact.setCustomer(customer);
            contactRepo.save(contact); // Updates the foreign key
        }

        return mapToDto(customer);
    }

    public List<CustomerDto> getCustomers(String username) {
        Tenant tenant = getTenantFromUsername(username);

        return customerRepository.findAllByTenantId(tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CustomerDto mapToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setEmail(customer.getEmail());
        dto.setAddress(customer.getAddress());
        dto.setTotalSpent(customer.getTotalSpent());
        dto.setOrderCount(customer.getOrderCount());
        return dto;
    }
}