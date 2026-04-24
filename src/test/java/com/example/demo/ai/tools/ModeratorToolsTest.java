// ==========================================
// Naylah Testing using Mockito

package com.example.demo.ai.tools;

import com.example.demo.domain.Contact;
import com.example.demo.domain.Complaint;
import com.example.demo.domain.Order;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.domain.Task;
import com.example.demo.domain.Message;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ComplaintRepo;
import com.example.demo.repository.TaskRepo;
import com.example.demo.repository.MessageRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ModeratorToolsTest {

    @Mock
    private ContactRepo contactRepo; // "Fake" database
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ComplaintRepo complaintRepo;
    @Mock
    private TaskRepo taskRepo;
    @Mock
    private MessageRepo messageRepo;

    @InjectMocks
    private ModeratorTools moderatorTools; // The tool we are testing

    @Test
    public void testGetCustomerDossier_Success() {
        // 1. SETUP: Fake Contact linked to a Fake Customer
        String psid = "12345";
        Long tenantId = 1L;
        Long internalCustomerId = 99L;

        com.example.demo.domain.Customer mockCustomer = new com.example.demo.domain.Customer();
        mockCustomer.setId(internalCustomerId);

        Contact mockContact = new Contact();
        mockContact.setId(psid);
        mockContact.setName("Jasmine");
        mockContact.setTags("VIP");
        mockContact.setCustomer(mockCustomer);

        // Fake Orders
        com.example.demo.domain.Order o1 = new com.example.demo.domain.Order();
        o1.setTotalAmount(BigDecimal.valueOf(100.0));
        com.example.demo.domain.Order o2 = new com.example.demo.domain.Order();
        o2.setTotalAmount(BigDecimal.valueOf(50.5));

        // Tell Mockito what to return
        when(contactRepo.findByIdAndTenantId(psid, tenantId)).thenReturn(Optional.of(mockContact));
        when(orderRepository.findAllByCustomerIdAndTenantId(internalCustomerId, tenantId))
                .thenReturn(java.util.List.of(o1, o2));

        // 2. ACT
        String result = moderatorTools.getCustomerDossier(psid, tenantId);

        // 3. ASSERT
        assert(result.contains("Name: Jasmine"));
        assert(result.contains("Tags: VIP"));
        assert(result.contains("Total Lifetime Spending: $150.50"));

        // Testing output in Terminal
        System.out.println("\n[TOOL 1: DOSSIER OUTPUT]\n" + result);
        assertTrue(result.contains("Jasmine"));
    }

    @Test
    public void testUpdateCustomerMeta_Success() {
        // 1. SETUP: Create a fake contact
        String psid = "12345";
        Long tenantId = 1L;
        Contact mockContact = new Contact();
        mockContact.setId(psid);
        mockContact.setName("Jasmine");

        // Tell Mockito: "When the tool asks the DB for this contact, return our fake Jasmine"
        when(contactRepo.findByIdAndTenantId(psid, tenantId)).thenReturn(Optional.of(mockContact));

        // 2. ACT: Run the tool
        String result = moderatorTools.updateCustomerMeta(psid, "VIP", "TAG", tenantId);

        // 3. ASSERT: Check if it worked
        assertEquals("Successfully updated TAG for Jasmine", result);
        assertEquals("VIP", mockContact.getTags());

        // Verify the database 'save' was actually called
        verify(contactRepo, times(1)).save(mockContact);

        // Testing output in Terminal
        System.out.println("\n[TOOL 2: TAGS & NOTES OUTPUT]\n" + result);
        assertTrue(result.contains("updated"));
    }

    @Test
    public void testManageOrders_UpdateStatus() {
        Long tenantId = 1L;
        Long orderId = 101L;
        Order mockOrder = new Order();
        mockOrder.setId(orderId);
        mockOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(mockOrder));

        // ACT
        String result = moderatorTools.manageOrders("UPDATE", null, orderId, "SHIPPED", tenantId);

        // ASSERT
        assertEquals("Successfully updated Order #101 to SHIPPED", result);
        assertEquals(OrderStatus.SHIPPED, mockOrder.getStatus());
        verify(orderRepository, times(1)).save(mockOrder);

        // Testing output in Terminal
        System.out.println("\n[TOOL 3: ORDER MANAGER OUTPUT]\n" + result);
        assertTrue(result.contains("SHIPPED"));
    }

    @Test
    public void testHandleComplaints_LogSuccess() {
        Long tenantId = 1L;
        String psid = "12345";

        // Test LOG action
        String result = moderatorTools.handleComplaints("LOG", psid, "Damaged item", "HIGH", tenantId);

        verify(complaintRepo, times(1)).save(any(Complaint.class));
        assertEquals("Logged new HIGH priority complaint for PSID: 12345", result);

        // Testing output in Terminal
        System.out.println("\n[TOOL 4: COMPLAINT OUTPUT]\n" + result);
        assertTrue(result.toLowerCase().contains("logged"));
    }

    @Test
    public void testManageTasks_Create() {
        Long tenantId = 1L;

        // ACT
        String result = moderatorTools.manageTasks("CREATE", "Call back about refund", "12345", 2, tenantId);

        // ASSERT
        verify(taskRepo, times(1)).save(any(Task.class));
        assert(result.contains("Task created"));

        // Testing output in Terminal
        System.out.println("\n[TOOL 5: TASK MANAGER OUTPUT]\n" + result);
        assertTrue(result.contains("Task created"));
    }

    @Test
    public void testGetShopPerformance_Today() {
        Long tenantId = 1L;

        // Create two orders for today
        Order o1 = Order.builder()
                .totalAmount(new BigDecimal("100.50"))
                .createdAt(LocalDateTime.now())
                .build();
        Order o2 = Order.builder()
                .totalAmount(new BigDecimal("50.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findAllByTenantId(tenantId)).thenReturn(List.of(o1, o2));

        // ACT
        String result = moderatorTools.getShopPerformance("TODAY", tenantId);

        // ASSERT
        assert(result.contains("Total Orders: 2"));
        assert(result.contains("Total Revenue: $150.50"));

        // Testing output in Terminal
        System.out.println("\n[TOOL 6: SHOP SNAPSHOT OUTPUT]\n" + result);
        assertTrue(result.contains("Total Revenue"));
    }

    @Test
    public void testGetDetailedChatLogs_Success() {
        String psid = "12345";
        Long tenantId = 1L;

        Contact mockContact = new Contact();
        mockContact.setName("Jasmine");

        Message m1 = new Message();
        m1.setContent("I need help with my order");
        m1.setContact(mockContact);

        // Use the long method name from your repository
        when(messageRepo.findTop10ByContact_IdAndContact_Tenant_IdOrderByCreatedAtDesc(psid, tenantId))
                .thenReturn(java.util.List.of(m1));

        String result = moderatorTools.getDetailedChatLogs(psid, tenantId);

        // Testing output in Terminal
        System.out.println("\n[TOOL 7: GetDetailedChatLogs_Success]\n" + result);
        assertTrue(result.contains("I need help with my order"));
    }
}
// ==========================================
