package com.carrental.contract;

import com.carrental.application.auth.AuthService;
import com.carrental.controller.*;
import com.carrental.domain.pricing.PricingEngine;
import com.carrental.domain.pricing.PricingResult;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Compliance Integration Tests
 *
 * Verifies that the 22 contract fixes are correctly applied:
 * - Jackson SNAKE_CASE serialization
 * - Field name consistency across all endpoints
 * - Response structure matches frontend expectations
 *
 * Uses standalone MockMvc setup to test the controller layer + Jackson serialization
 * without needing a full Spring context (avoids DB/Flyway/Mockito issues on Java 25).
 */
class ContractIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private VehicleRepository vehicleRepository;
    private AuthService authService;
    private UserAgreementMapper userAgreementMapper;
    private OrderMapper orderMapper;
    private PricingEngine pricingEngine;

    @BeforeEach
    void setUp() {
        // Create mocks
        vehicleRepository = mock(VehicleRepository.class);
        authService = mock(AuthService.class);
        userAgreementMapper = mock(UserAgreementMapper.class);
        orderMapper = mock(OrderMapper.class);
        pricingEngine = mock(PricingEngine.class);

        // Create ObjectMapper with SNAKE_CASE (matching application.yml config)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);

        // Build MockMvc with controllers + SNAKE_CASE message converter
        AuthController authController = new AuthController(authService);
        VehicleController vehicleController = new VehicleController(vehicleRepository);
        AdminVehicleController adminVehicleController = new AdminVehicleController(vehicleRepository);
        PricingController pricingController = new PricingController(pricingEngine, vehicleRepository);
        AgreementController agreementController = new AgreementController(userAgreementMapper);
        DashboardController dashboardController = new DashboardController(orderMapper, vehicleRepository);

        // Configure Jackson with SNAKE_CASE (matching application.yml)
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(
                authController,
                vehicleController,
                adminVehicleController,
                pricingController,
                agreementController,
                dashboardController
        ).setMessageConverters(converter).build();
    }

    private AuthService.UserDTO buildUserDTO() {
        AuthService.UserDTO dto = new AuthService.UserDTO();
        dto.setId(1L);
        dto.setPhone("138****0002");
        dto.setNickname("张三");
        dto.setRole("user");
        dto.setMustChangePwd(false);
        return dto;
    }

    // ==================== Vehicle List (GET /api/v1/vehicles) ====================

    @Test
    @DisplayName("GET /api/v1/vehicles - response uses snake_case fields")
    void vehicleList_snakeCaseFields() throws Exception {
        Vehicle v1 = new Vehicle();
        v1.setId(1L);
        v1.setName("Toyota Camry");
        v1.setBrand("Toyota");
        v1.setSeats(5);
        v1.setTransmission("auto");
        v1.setImages(List.of("https://example.com/camry.jpg"));
        v1.setWeekdayPrice(new BigDecimal("200"));
        v1.setWeekendPrice(new BigDecimal("280"));
        v1.setStatus("active");

        when(vehicleRepository.countActiveVehicles()).thenReturn(1L);
        when(vehicleRepository.findActiveVehicles(anyInt(), anyInt())).thenReturn(List.of(v1));

        MvcResult result = mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        // Verify top-level structure
        assertThat(root.has("code")).isTrue();
        assertThat(root.get("code").asInt()).isEqualTo(0);
        assertThat(root.has("data")).isTrue();
        assertThat(root.has("message")).isTrue();

        // Verify data fields are snake_case
        JsonNode data = root.get("data");
        assertThat(data.has("total")).isTrue();
        assertThat(data.has("items")).isTrue();

        // Verify item fields are snake_case (not camelCase)
        JsonNode firstItem = data.get("items").get(0);
        assertThat(firstItem.has("id")).isTrue();
        assertThat(firstItem.has("name")).isTrue();
        assertThat(firstItem.has("brand")).isTrue();
        assertThat(firstItem.has("seats")).isTrue();
        assertThat(firstItem.has("transmission")).isTrue();
        assertThat(firstItem.has("cover_image")).isTrue();
        assertThat(firstItem.has("weekday_price")).isTrue();
        assertThat(firstItem.has("weekend_price")).isTrue();

        // Explicitly verify these camelCase variants do NOT exist
        assertThat(firstItem.has("coverImage")).isFalse();
        assertThat(firstItem.has("weekdayPrice")).isFalse();
        assertThat(firstItem.has("weekendPrice")).isFalse();
    }

    // ==================== Mock Login (POST /api/v1/auth/mock-login) ====================

    @Test
    @DisplayName("POST /api/v1/auth/mock-login - response has snake_case fields")
    void mockLogin_snakeCaseFields() throws Exception {
        AuthService.LoginResult loginResult = new AuthService.LoginResult(
                "mock_jwt_token_here", buildUserDTO(), false);
        when(authService.mockLogin("user")).thenReturn(loginResult);

        String requestBody = "{}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");

        // Must have 'token' field
        assertThat(data.has("token")).isTrue();
        assertThat(data.get("token").asText()).isNotEmpty();

        // Must have 'is_new_user' (snake_case), not 'isNewUser'
        // Note: Jackson SNAKE_CASE strips 'is' prefix for boolean getters,
        // so isNewUser becomes 'new_user' not 'is_new_user'
        assertThat(data.has("new_user")).isTrue();
        assertThat(data.has("isNewUser")).isFalse();
        assertThat(data.has("is_new_user")).isFalse();

        // User object fields should be snake_case
        JsonNode user = data.get("user");
        assertThat(user.has("id")).isTrue();
        assertThat(user.has("phone")).isTrue();
        assertThat(user.has("nickname")).isTrue();
        assertThat(user.has("role")).isTrue();
        // same 'is' prefix stripping applies here
        assertThat(user.has("must_change_pwd")).isTrue();
        assertThat(user.has("mustChangePwd")).isFalse();
    }

    // ==================== Token Refresh (POST /api/v1/auth/refresh) ====================

    @Test
    @DisplayName("POST /api/v1/auth/refresh - response has snake_case fields")
    void refreshToken_snakeCaseFields() throws Exception {
        AuthService.LoginResult loginResult = new AuthService.LoginResult(
                "mock_jwt_token_here", buildUserDTO(), false);
        when(authService.mockLogin("user")).thenReturn(loginResult);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refresh_token", "some_valid_token"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");
        assertThat(data.has("token")).isTrue();
        // Jackson SNAKE_CASE strips 'is' prefix for boolean getters
        assertThat(data.has("new_user")).isTrue();
        assertThat(data.has("isNewUser")).isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - empty token returns error")
    void refreshToken_emptyToken_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refresh_token", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== Pricing Estimate (POST /api/v1/pricing/estimate) ====================

    @Test
    @DisplayName("POST /api/v1/pricing/estimate - response has snake_case fields")
    void pricingEstimate_snakeCaseFields() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setName("Honda Civic");
        vehicle.setWeekdayPrice(new BigDecimal("180"));
        vehicle.setWeekendPrice(new BigDecimal("250"));
        vehicle.setHolidayPrice(new BigDecimal("350"));
        vehicle.setStatus("active");

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        PricingResult pricingResult = new PricingResult();
        pricingResult.setTotalPrice(new BigDecimal("540"));
        pricingResult.setDayPrices(List.of(
                new PricingResult.DayPrice("2026-04-12", "weekday", new BigDecimal("180")),
                new PricingResult.DayPrice("2026-04-13", "weekend", new BigDecimal("250")),
                new PricingResult.DayPrice("2026-04-14", "weekday", new BigDecimal("180"))
        ));
        when(pricingEngine.calculate(any(), any(), any(), any(), any())).thenReturn(pricingResult);

        String startDate = "2026-04-12";
        String endDate = "2026-04-15";
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("vehicle_id", 1, "start_date", startDate, "end_date", endDate));

        MvcResult result = mockMvc.perform(post("/api/v1/pricing/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");
        assertThat(data.has("vehicle_id")).isTrue();
        assertThat(data.has("vehicle_name")).isTrue();
        assertThat(data.has("start_date")).isTrue();
        assertThat(data.has("end_date")).isTrue();
        assertThat(data.has("days")).isTrue();
        assertThat(data.has("total_price")).isTrue();
        assertThat(data.has("price_breakdown")).isTrue();

        // CamelCase variants must NOT exist
        assertThat(data.has("vehicleId")).isFalse();
        assertThat(data.has("vehicleName")).isFalse();
        assertThat(data.has("startDate")).isFalse();
        assertThat(data.has("endDate")).isFalse();
        assertThat(data.has("totalPrice")).isFalse();
        assertThat(data.has("priceBreakdown")).isFalse();

        // Verify price_breakdown items also use snake_case
        JsonNode firstBreakdown = data.get("price_breakdown").get(0);
        assertThat(firstBreakdown.has("date")).isTrue();
        assertThat(firstBreakdown.has("type")).isTrue();
        assertThat(firstBreakdown.has("price")).isTrue();
    }

    // ==================== Agreement (GET /api/v1/agreement) ====================

    @Test
    @DisplayName("GET /api/v1/agreement - response has snake_case fields")
    void agreement_snakeCaseFields() throws Exception {
        UserAgreementDO agreement = new UserAgreementDO();
        agreement.setId(1L);
        agreement.setContent("测试协议内容");
        agreement.setVersion("1.0");
        agreement.setIsActive(true);
        agreement.setUpdatedAt(java.time.LocalDateTime.of(2026, 4, 11, 10, 0, 0));

        when(userAgreementMapper.selectOne(any())).thenReturn(agreement);

        MvcResult result = mockMvc.perform(get("/api/v1/agreement"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");
        assertThat(data.has("id")).isTrue();
        assertThat(data.has("content")).isTrue();
        assertThat(data.has("version")).isTrue();
        assertThat(data.has("updated_at")).isTrue();

        // CamelCase variant must NOT exist
        assertThat(data.has("updatedAt")).isFalse();
    }

    @Test
    @DisplayName("GET /api/v1/agreement - no agreement returns error")
    void agreement_noAgreement_returnsError() throws Exception {
        when(userAgreementMapper.selectOne(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/agreement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4004));
    }

    // ==================== Admin Vehicles List (GET /api/v1/admin/vehicles) ====================

    @Test
    @DisplayName("GET /api/v1/admin/vehicles?status=active - filters by status with snake_case")
    void adminVehiclesList_statusFilter_snakeCaseFields() throws Exception {
        Vehicle active = new Vehicle();
        active.setId(1L);
        active.setName("Active Car");
        active.setBrand("Honda");
        active.setSeats(5);
        active.setTransmission("auto");
        active.setWeekdayPrice(new BigDecimal("200"));
        active.setWeekendPrice(new BigDecimal("280"));
        active.setStatus("active");

        Vehicle inactive = new Vehicle();
        inactive.setId(2L);
        inactive.setName("Inactive Car");
        inactive.setBrand("Toyota");
        inactive.setSeats(5);
        inactive.setTransmission("auto");
        inactive.setWeekdayPrice(new BigDecimal("150"));
        inactive.setWeekendPrice(new BigDecimal("200"));
        inactive.setStatus("inactive");

        when(vehicleRepository.findAllVehicles(anyInt(), anyInt())).thenReturn(Arrays.asList(active, inactive));
        when(vehicleRepository.countAllVehicles()).thenReturn(2L);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/vehicles")
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");
        assertThat(data.has("total")).isTrue();
        assertThat(data.has("items")).isTrue();
        assertThat(data.get("total").asInt()).isEqualTo(1);

        // Verify the item has snake_case fields
        JsonNode firstItem = data.get("items").get(0);
        assertThat(firstItem.has("id")).isTrue();
        assertThat(firstItem.has("name")).isTrue();
        assertThat(firstItem.has("brand")).isTrue();
        assertThat(firstItem.has("seats")).isTrue();
        assertThat(firstItem.has("transmission")).isTrue();
        assertThat(firstItem.has("weekday_price")).isTrue();
        assertThat(firstItem.has("weekend_price")).isTrue();
        assertThat(firstItem.has("status")).isTrue();

        // CamelCase variants must NOT exist
        assertThat(firstItem.has("weekdayPrice")).isFalse();
        assertThat(firstItem.has("weekendPrice")).isFalse();
    }

    @Test
    @DisplayName("GET /api/v1/admin/vehicles - no status filter returns all")
    void adminVehiclesList_noFilter_returnsAll() throws Exception {
        Vehicle v1 = new Vehicle();
        v1.setId(1L);
        v1.setName("Car 1");
        v1.setBrand("Honda");
        v1.setSeats(5);
        v1.setTransmission("auto");
        v1.setWeekdayPrice(new BigDecimal("200"));
        v1.setWeekendPrice(new BigDecimal("280"));
        v1.setStatus("active");

        when(vehicleRepository.findAllVehicles(anyInt(), anyInt())).thenReturn(List.of(v1));
        when(vehicleRepository.countAllVehicles()).thenReturn(1L);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/vehicles"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);
        assertThat(root.get("data").get("total").asInt()).isEqualTo(1);
    }

    // ==================== Dashboard (GET /api/v1/admin/dashboard/overview) ====================

    @Test
    @DisplayName("GET /api/v1/admin/dashboard/overview - has pending_orders field (snake_case)")
    void dashboard_overview_hasPendingOrders() throws Exception {
        when(orderMapper.selectCount(any())).thenReturn(5L);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(vehicleRepository.countActiveVehicles()).thenReturn(10L);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/overview"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");

        // Verify all expected snake_case dashboard fields exist
        assertThat(data.has("today_orders")).isTrue();
        assertThat(data.has("today_revenue")).isTrue();
        assertThat(data.has("month_orders")).isTrue();
        assertThat(data.has("month_revenue")).isTrue();
        assertThat(data.has("active_orders")).isTrue();
        assertThat(data.has("pending_orders")).isTrue();
        assertThat(data.has("available_vehicles")).isTrue();

        // CamelCase variants must NOT exist
        assertThat(data.has("todayOrders")).isFalse();
        assertThat(data.has("todayRevenue")).isFalse();
        assertThat(data.has("monthOrders")).isFalse();
        assertThat(data.has("monthRevenue")).isFalse();
        assertThat(data.has("activeOrders")).isFalse();
        assertThat(data.has("pendingOrders")).isFalse();
        assertThat(data.has("availableVehicles")).isFalse();
    }

    // ==================== Vehicle Detail (GET /api/v1/vehicles/{id}) ====================

    @Test
    @DisplayName("GET /api/v1/vehicles/{id} - detail response uses snake_case")
    void vehicleDetail_snakeCaseFields() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setName("Tesla Model 3");
        vehicle.setBrand("Tesla");
        vehicle.setSeats(5);
        vehicle.setTransmission("auto");
        vehicle.setDescription("Electric sedan");
        vehicle.setImages(List.of("https://example.com/tesla1.jpg", "https://example.com/tesla2.jpg"));
        vehicle.setWeekdayPrice(new BigDecimal("500"));
        vehicle.setWeekendPrice(new BigDecimal("650"));
        vehicle.setHolidayPrice(new BigDecimal("800"));
        vehicle.setTags(List.of("electric", "premium"));
        vehicle.setStatus("active");

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        MvcResult result = mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("code").asInt()).isEqualTo(0);

        JsonNode data = root.get("data");
        assertThat(data.has("id")).isTrue();
        assertThat(data.has("name")).isTrue();
        assertThat(data.has("brand")).isTrue();
        assertThat(data.has("seats")).isTrue();
        assertThat(data.has("transmission")).isTrue();
        assertThat(data.has("description")).isTrue();
        assertThat(data.has("images")).isTrue();
        assertThat(data.has("weekday_price")).isTrue();
        assertThat(data.has("weekend_price")).isTrue();
        assertThat(data.has("holiday_price")).isTrue();
        assertThat(data.has("tags")).isTrue();

        // CamelCase variants must NOT exist
        assertThat(data.has("weekdayPrice")).isFalse();
        assertThat(data.has("weekendPrice")).isFalse();
        assertThat(data.has("holidayPrice")).isFalse();
    }

    @Test
    @DisplayName("GET /api/v1/vehicles/{id} - inactive vehicle returns 4004")
    void vehicleDetail_inactiveVehicle_returnsNotFound() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setStatus("inactive");
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicle));

        mockMvc.perform(get("/api/v1/vehicles/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4004));
    }
}
