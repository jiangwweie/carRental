package com.carrental.admin;

import com.carrental.application.pricing.HolidayAdminService;
import com.carrental.controller.AdminOrderController;
import com.carrental.controller.AdminVehicleController;
import com.carrental.controller.AgreementController;
import com.carrental.controller.DashboardController;
import com.carrental.domain.holiday.Holiday;
import com.carrental.domain.order.Order;
import com.carrental.domain.order.OrderRepository;
import com.carrental.domain.order.OrderStatus;
import com.carrental.domain.order.PriceBreakdown;
import com.carrental.domain.user.User;
import com.carrental.domain.user.UserRepository;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import com.carrental.infrastructure.persistence.dataobject.UserAgreementDO;
import com.carrental.infrastructure.persistence.mapper.OrderMapper;
import com.carrental.infrastructure.persistence.mapper.UserAgreementMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin Integration Tests
 *
 * Tests all admin-side user stories (US-08 ~ US-14, US-23, US-24) against
 * the admin backend API endpoints using MockMvc standalone setup.
 *
 * Coverage by user story:
 * - US-08/US-24: Dashboard overview
 * - US-09: Vehicle CRUD management
 * - US-10/US-12/US-13: Order management (list, detail, confirm, reject, start, complete)
 * - US-11: Holiday pricing management
 * - US-06: Agreement management (admin update)
 */
class AdminIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Repositories / Services / Mappers (mocked)
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private HolidayAdminService holidayAdminService;
    private UserAgreementMapper userAgreementMapper;
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        // Create mocks
        orderRepository = mock(OrderRepository.class);
        userRepository = mock(UserRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        holidayAdminService = mock(HolidayAdminService.class);
        userAgreementMapper = mock(UserAgreementMapper.class);
        orderMapper = mock(OrderMapper.class);

        // Create ObjectMapper with SNAKE_CASE (matching application.yml config)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);

        // Build MockMvc with admin controllers
        AdminVehicleController adminVehicleController = new AdminVehicleController(vehicleRepository);
        AdminOrderController adminOrderController = new AdminOrderController(orderRepository, userRepository, vehicleRepository);
        DashboardController dashboardController = new DashboardController(orderMapper, vehicleRepository);
        com.carrental.controller.AdminPricingController adminPricingController =
                new com.carrental.controller.AdminPricingController(holidayAdminService);
        AgreementController agreementController = new AgreementController(userAgreementMapper);

        // Configure Jackson with SNAKE_CASE
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(
                adminVehicleController,
                adminOrderController,
                dashboardController,
                adminPricingController,
                agreementController
        ).setMessageConverters(converter).build();
    }

    // ==================== US-08 / US-24: Dashboard ====================

    @Nested
    @DisplayName("US-08/US-24: Dashboard Overview")
    class DashboardTests {

        @Test
        @DisplayName("GET /api/v1/admin/dashboard/overview - returns all dashboard metrics")
        void dashboard_overview_returnsAllMetrics() throws Exception {
            // Setup mocks on the class-level orderMapper
            when(orderMapper.selectCount(any())).thenReturn(5L);
            when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(vehicleRepository.countActiveVehicles()).thenReturn(12L);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");

            // Verify all 7 dashboard metrics exist
            assertThat(data.has("today_orders")).isTrue();
            assertThat(data.has("today_revenue")).isTrue();
            assertThat(data.has("month_orders")).isTrue();
            assertThat(data.has("month_revenue")).isTrue();
            assertThat(data.has("active_orders")).isTrue();
            assertThat(data.has("pending_orders")).isTrue();
            assertThat(data.has("available_vehicles")).isTrue();

            // Verify camelCase variants do NOT exist
            assertThat(data.has("todayOrders")).isFalse();
            assertThat(data.has("todayRevenue")).isFalse();
            assertThat(data.has("monthOrders")).isFalse();
            assertThat(data.has("monthRevenue")).isFalse();
            assertThat(data.has("activeOrders")).isFalse();
            assertThat(data.has("pendingOrders")).isFalse();
            assertThat(data.has("availableVehicles")).isFalse();

            // Verify data types
            assertThat(data.get("today_orders").asInt()).isEqualTo(5);
            assertThat(data.get("today_revenue").asDouble()).isEqualTo(0.0);
            assertThat(data.get("available_vehicles").asInt()).isEqualTo(12);
        }
    }

    // ==================== US-09: Vehicle Management ====================

    @Nested
    @DisplayName("US-09: Vehicle Management (CRUD)")
    class VehicleManagementTests {

        @Test
        @DisplayName("POST /api/v1/admin/vehicles - creates a new vehicle")
        void createVehicle_success() throws Exception {
            Vehicle saved = new Vehicle();
            saved.setId(1L);
            saved.setName("Toyota Camry");
            saved.setBrand("Toyota");
            saved.setSeats(5);
            saved.setTransmission("auto");
            saved.setDescription("Mid-size sedan");
            saved.setImages(List.of("img1", "img2"));
            saved.setWeekdayPrice(new BigDecimal("200"));
            saved.setWeekendPrice(new BigDecimal("280"));
            saved.setStatus("active");

            when(vehicleRepository.save(any())).thenReturn(saved);

            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "Toyota Camry",
                    "brand", "Toyota",
                    "seats", 5,
                    "transmission", "auto",
                    "description", "Mid-size sedan",
                    "images", List.of("img1", "img2"),
                    "weekday_price", 200,
                    "weekend_price", 280
            ));

            MvcResult result = mockMvc.perform(post("/api/v1/admin/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("Toyota Camry"))
                    .andExpect(jsonPath("$.data.brand").value("Toyota"))
                    .andExpect(jsonPath("$.data.seats").value(5))
                    .andExpect(jsonPath("$.data.transmission").value("auto"))
                    .andExpect(jsonPath("$.data.weekday_price").value(200))
                    .andExpect(jsonPath("$.data.weekend_price").value(280))
                    .andExpect(jsonPath("$.data.status").value("active"))
                    .andReturn();

            // Verify snake_case fields in response
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode data = root.get("data");
            assertThat(data.has("weekday_price")).isTrue();
            assertThat(data.has("weekdayPrice")).isFalse();
            assertThat(data.has("weekend_price")).isTrue();
            assertThat(data.has("weekendPrice")).isFalse();
        }

        @Test
        @DisplayName("PUT /api/v1/admin/vehicles/{id} - updates vehicle fields partially")
        void updateVehicle_partialUpdate() throws Exception {
            Vehicle existing = new Vehicle();
            existing.setId(1L);
            existing.setName("Old Name");
            existing.setBrand("Old Brand");
            existing.setSeats(5);
            existing.setTransmission("auto");
            existing.setWeekdayPrice(new BigDecimal("200"));
            existing.setWeekendPrice(new BigDecimal("280"));

            Vehicle updated = new Vehicle();
            updated.setId(1L);
            updated.setName("New Name");
            updated.setBrand("Old Brand"); // unchanged
            updated.setSeats(5);
            updated.setTransmission("auto");
            updated.setWeekdayPrice(new BigDecimal("250"));
            updated.setWeekendPrice(new BigDecimal("280"));

            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(vehicleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "New Name",
                    "weekday_price", 250
            ));

            mockMvc.perform(put("/api/v1/admin/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("New Name"));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/vehicles/{id} - returns 4004 for non-existent vehicle")
        void updateVehicle_notFound() throws Exception {
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/admin/vehicles/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(4004));
        }

        @Test
        @DisplayName("POST /api/v1/admin/vehicles/{id}/toggle-status - toggles active to inactive")
        void toggleStatus_activeToInactive() throws Exception {
            Vehicle vehicle = new Vehicle();
            vehicle.setId(1L);
            vehicle.setName("Test Car");
            vehicle.setBrand("Honda");
            vehicle.setSeats(5);
            vehicle.setTransmission("auto");
            vehicle.setWeekdayPrice(new BigDecimal("200"));
            vehicle.setWeekendPrice(new BigDecimal("280"));
            vehicle.setStatus("active");

            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/vehicles/1/toggle-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("inactive"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/vehicles/{id}/toggle-status - toggles inactive to active")
        void toggleStatus_inactiveToActive() throws Exception {
            Vehicle vehicle = new Vehicle();
            vehicle.setId(2L);
            vehicle.setName("Test Car");
            vehicle.setBrand("Honda");
            vehicle.setSeats(5);
            vehicle.setTransmission("auto");
            vehicle.setWeekdayPrice(new BigDecimal("200"));
            vehicle.setWeekendPrice(new BigDecimal("280"));
            vehicle.setStatus("inactive");

            when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/vehicles/2/toggle-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("active"));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/vehicles/{id} - soft deletes vehicle")
        void deleteVehicle_success() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/vehicles/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(vehicleRepository).softDelete(1L);
        }

        @Test
        @DisplayName("PUT /api/v1/admin/vehicles/prices - batch updates vehicle prices")
        void batchUpdatePrices_success() throws Exception {
            Vehicle v1 = new Vehicle();
            v1.setId(1L);
            v1.setName("Car 1");
            v1.setBrand("Honda");
            v1.setSeats(5);
            v1.setTransmission("auto");
            v1.setWeekdayPrice(new BigDecimal("200"));
            v1.setWeekendPrice(new BigDecimal("280"));
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(v1));
            when(vehicleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Vehicle v2 = new Vehicle();
            v2.setId(2L);
            v2.setName("Car 2");
            v2.setBrand("Toyota");
            v2.setSeats(7);
            v2.setTransmission("auto");
            v2.setWeekdayPrice(new BigDecimal("300"));
            v2.setWeekendPrice(new BigDecimal("400"));
            when(vehicleRepository.findById(2L)).thenReturn(Optional.of(v2));

            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "items", List.of(
                            java.util.Map.of("id", 1, "weekday_price", 220, "weekend_price", 300),
                            java.util.Map.of("id", 2, "weekday_price", 350)
                    )
            ));

            mockMvc.perform(put("/api/v1/admin/vehicles/prices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // Verify prices were updated
            verify(vehicleRepository, times(2)).save(any());
        }
    }

    // ==================== US-10 / US-12 / US-13: Order Management ====================

    @Nested
    @DisplayName("US-10/US-12/US-13: Order Management")
    class OrderManagementTests {

        @Test
        @DisplayName("GET /api/v1/admin/orders - returns order list with user/vehicle info")
        void adminOrders_listWithUserInfo() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            when(orderRepository.findAdminOrders(any(), any(), anyInt(), anyInt())).thenReturn(List.of(order));
            when(orderRepository.countAdminOrders(any(), any())).thenReturn(1L);

            User user = new User("13800138000", "openid123", "张三");
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            Vehicle vehicle = new Vehicle();
            vehicle.setId(1L);
            vehicle.setName("Toyota Camry");
            vehicle.setBrand("Toyota");
            vehicle.setSeats(5);
            vehicle.setTransmission("auto");
            vehicle.setWeekdayPrice(new BigDecimal("200"));
            vehicle.setWeekendPrice(new BigDecimal("280"));
            vehicle.setImages(List.of("https://example.com/img.jpg"));
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

            MvcResult result = mockMvc.perform(get("/api/v1/admin/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode items = root.get("data").get("items");
            assertThat(items.size()).isEqualTo(1);

            JsonNode firstOrder = items.get(0);
            // Verify admin DTO has vehicle and user info
            assertThat(firstOrder.has("vehicle_name")).isTrue();
            assertThat(firstOrder.has("user_phone")).isTrue();
            assertThat(firstOrder.get("vehicle_name").asText()).isEqualTo("Toyota Camry");
            assertThat(firstOrder.get("user_phone").asText()).contains("****"); // phone masking
            assertThat(firstOrder.get("user_phone").asText()).isEqualTo("138****8000");
        }

        @Test
        @DisplayName("GET /api/v1/admin/orders?status=pending - filters by status")
        void adminOrders_filterByStatus() throws Exception {
            when(orderRepository.findAdminOrders(eq("pending"), any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
            when(orderRepository.countAdminOrders(eq("pending"), any())).thenReturn(0L);

            mockMvc.perform(get("/api/v1/admin/orders")
                            .param("status", "pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("GET /api/v1/admin/orders?vehicleId=1 - filters by vehicle")
        void adminOrders_filterByVehicle() throws Exception {
            when(orderRepository.findAdminOrders(any(), eq(1L), anyInt(), anyInt())).thenReturn(Collections.emptyList());
            when(orderRepository.countAdminOrders(any(), eq(1L))).thenReturn(0L);

            mockMvc.perform(get("/api/v1/admin/orders")
                            .param("vehicleId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/admin/orders/{id} - returns order detail with vehicle info")
        void adminOrderDetail_success() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            order.setPriceBreakdown(List.of(
                    new PriceBreakdown(LocalDate.of(2026, 4, 12), "weekday", new BigDecimal("200")),
                    new PriceBreakdown(LocalDate.of(2026, 4, 13), "weekday", new BigDecimal("200"))
            ));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Vehicle vehicle = new Vehicle();
            vehicle.setId(1L);
            vehicle.setName("Toyota Camry");
            vehicle.setImages(List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg"));
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

            User user = new User("13800138000", "openid123", "张三");
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            MvcResult result = mockMvc.perform(get("/api/v1/admin/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.vehicle.name").value("Toyota Camry"))
                    .andExpect(jsonPath("$.data.user_phone").value("13800138000"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode data = root.get("data");

            // Verify order detail VO has all required fields
            assertThat(data.has("id")).isTrue();
            assertThat(data.has("vehicle")).isTrue();
            assertThat(data.has("start_date")).isTrue();
            assertThat(data.has("end_date")).isTrue();
            assertThat(data.has("days")).isTrue();
            assertThat(data.has("total_price")).isTrue();
            assertThat(data.has("status")).isTrue();
            assertThat(data.has("status_label")).isTrue();
            assertThat(data.has("status_steps")).isTrue();
            assertThat(data.has("price_breakdown")).isTrue();
            assertThat(data.has("pickup_address")).isTrue();
            assertThat(data.has("can_cancel")).isTrue();
            assertThat(data.has("reject_reason")).isTrue();
            assertThat(data.has("user_phone")).isTrue();

            // Verify vehicle info in detail
            assertThat(data.get("vehicle").has("id")).isTrue();
            assertThat(data.get("vehicle").has("name")).isTrue();
            assertThat(data.get("vehicle").has("images")).isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/confirm - confirms pending order")
        void confirmOrder_success() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/orders/1/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/confirm - rejects non-pending order")
        void confirmOrder_wrongStatus() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            order.setStatus(OrderStatus.CONFIRMED); // already confirmed
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            mockMvc.perform(post("/api/v1/admin/orders/1/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(5300));
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/reject - rejects order with reason")
        void rejectOrder_withReason() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            String requestBody = objectMapper.writeValueAsString(java.util.Map.of("reason", "车辆故障"));

            mockMvc.perform(post("/api/v1/admin/orders/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/start - marks confirmed order as in progress")
        void startOrder_success() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/orders/1/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/complete - marks in-progress order as completed")
        void completeOrder_success() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            order.setStatus(OrderStatus.IN_PROGRESS);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/orders/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/complete - rejects non-in-progress order")
        void completeOrder_wrongStatus() throws Exception {
            Order order = buildPendingOrder(1L, 1L, 1L);
            order.setStatus(OrderStatus.PENDING); // not in_progress yet
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            mockMvc.perform(post("/api/v1/admin/orders/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(5300));
        }

        @Test
        @DisplayName("POST /api/v1/admin/orders/{id}/* - returns 4004 for non-existent order")
        void orderActions_notFound() throws Exception {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/admin/orders/99/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(4004));
        }
    }

    // ==================== US-11: Pricing / Holiday Management ====================

    @Nested
    @DisplayName("US-11: Holiday Pricing Management")
    class HolidayPricingTests {

        @Test
        @DisplayName("GET /api/v1/admin/pricing/holidays - returns holiday list")
        void listHolidays_success() throws Exception {
            Holiday holiday = new Holiday();
            holiday.setId(1L);
            holiday.setName("国庆节");
            holiday.setStartDate(LocalDate.of(2026, 10, 1));
            holiday.setEndDate(LocalDate.of(2026, 10, 7));
            holiday.setPriceMultiplier(new BigDecimal("2.0"));
            holiday.setYear(2026);

            when(holidayAdminService.listHolidays(2026)).thenReturn(List.of(holiday));

            mockMvc.perform(get("/api/v1/admin/pricing/holidays")
                            .param("year", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data[0].name").value("国庆节"))
                    .andExpect(jsonPath("$.data[0].start_date").value("2026-10-01"))
                    .andExpect(jsonPath("$.data[0].end_date").value("2026-10-07"))
                    .andExpect(jsonPath("$.data[0].price_multiplier").value(2.0));
        }

        @Test
        @DisplayName("POST /api/v1/admin/pricing/holidays - creates a holiday")
        void createHoliday_success() throws Exception {
            Holiday holiday = new Holiday();
            holiday.setId(1L);
            holiday.setName("春节");
            holiday.setStartDate(LocalDate.of(2026, 2, 17));
            holiday.setEndDate(LocalDate.of(2026, 2, 23));
            holiday.setPriceMultiplier(new BigDecimal("1.5"));
            holiday.setYear(2026);

            when(holidayAdminService.createHoliday(any())).thenReturn(holiday);

            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "春节",
                    "start_date", "2026-02-17",
                    "end_date", "2026-02-23",
                    "price_multiplier", 1.5,
                    "year", 2026
            ));

            mockMvc.perform(post("/api/v1/admin/pricing/holidays")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("春节"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/pricing/holidays/batch - batch creates holidays")
        void batchCreateHolidays_success() throws Exception {
            when(holidayAdminService.batchCreateHolidays(anyList())).thenReturn(2);

            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "holidays", List.of(
                            java.util.Map.of("name", "五一", "start_date", "2026-05-01", "end_date", "2026-05-05", "price_multiplier", 1.5, "year", 2026),
                            java.util.Map.of("name", "端午", "start_date", "2026-06-19", "end_date", "2026-06-21", "price_multiplier", 1.3, "year", 2026)
                    )
            ));

            mockMvc.perform(post("/api/v1/admin/pricing/holidays/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.created").value(2));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/pricing/holidays/{id} - deletes holiday")
        void deleteHoliday_success() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/pricing/holidays/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    // ==================== US-06: Agreement Management ====================

    @Nested
    @DisplayName("US-06: Agreement Management (Admin)")
    class AgreementManagementTests {

        @Test
        @DisplayName("PUT /api/v1/admin/agreement - updates agreement with new version")
        void updateAgreement_newVersion() throws Exception {
            // No existing agreement
            when(userAgreementMapper.selectOne(any())).thenReturn(null);
            when(userAgreementMapper.selectList(null)).thenReturn(Collections.emptyList());
            when(userAgreementMapper.insert(any(UserAgreementDO.class))).thenAnswer(invocation -> {
                UserAgreementDO agreement = invocation.getArgument(0);
                agreement.setId(1L);
                agreement.setUpdatedAt(LocalDateTime.now());
                return 1;
            });

            mockMvc.perform(put("/api/v1/admin/agreement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"新协议内容\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.version").value("1.0"))
                    .andExpect(jsonPath("$.data.content").value("新协议内容"));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/agreement - increments version number")
        void updateAgreement_incrementVersion() throws Exception {
            // Existing agreement
            UserAgreementDO existing = new UserAgreementDO();
            existing.setId(1L);
            existing.setContent("旧协议");
            existing.setVersion("1.2");
            existing.setIsActive(true);

            when(userAgreementMapper.selectOne(any())).thenReturn(existing);
            when(userAgreementMapper.selectList(null)).thenReturn(List.of(existing));
            when(userAgreementMapper.insert(any(UserAgreementDO.class))).thenAnswer(invocation -> {
                UserAgreementDO agreement = invocation.getArgument(0);
                agreement.setId(2L);
                agreement.setUpdatedAt(LocalDateTime.now());
                return 1;
            });

            mockMvc.perform(put("/api/v1/admin/agreement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"新协议内容 v2\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.version").value("1.3"));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/agreement - deactivates old agreement")
        void updateAgreement_deactivatesOld() throws Exception {
            UserAgreementDO existing = new UserAgreementDO();
            existing.setId(1L);
            existing.setContent("旧协议");
            existing.setVersion("1.0");
            existing.setIsActive(true);

            when(userAgreementMapper.selectOne(any())).thenReturn(existing);
            when(userAgreementMapper.selectList(null)).thenReturn(List.of(existing));
            when(userAgreementMapper.insert(any(UserAgreementDO.class))).thenAnswer(invocation -> {
                UserAgreementDO agreement = invocation.getArgument(0);
                agreement.setId(2L);
                agreement.setUpdatedAt(LocalDateTime.now());
                return 1;
            });

            mockMvc.perform(put("/api/v1/admin/agreement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"新协议\"}"))
                    .andExpect(status().isOk());

            // Verify old agreement was deactivated
            verify(userAgreementMapper).updateById(argThat((UserAgreementDO a) -> !a.getIsActive()));
        }
    }

    // ==================== Helper Methods ====================

    private Order buildPendingOrder(Long id, Long userId, Long vehicleId) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setVehicleId(vehicleId);
        order.setStartDate(LocalDate.of(2026, 4, 12));
        order.setEndDate(LocalDate.of(2026, 4, 14));
        order.setDays(2);
        order.setTotalPrice(new BigDecimal("400"));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus("unpaid");
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
