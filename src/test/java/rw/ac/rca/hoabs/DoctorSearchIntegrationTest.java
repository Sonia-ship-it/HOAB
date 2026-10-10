package rw.ac.rca.hoabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Doctor Search Integration Tests (DS-001..DS-006, FR-SEARCH-*)")
class DoctorSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DS-001: Active departments list loads")
    void testListDepartments() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].name", hasItems("Cardiology", "Paediatrics", "General Medicine")));
    }

    @Test
    @DisplayName("DS-002: Keyword search by doctor name returns match")
    void testSearchDoctorByName() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("query", "Patrick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].fullName", containsString("Patrick Mugisha")))
                .andExpect(jsonPath("$[0].staffId", is("DOC-001")));
    }

    @Test
    @DisplayName("DS-003: Keyword search by specialty returns match")
    void testSearchDoctorBySpecialty() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("query", "Cardiology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].specialty", is("Cardiology")));
    }

    @Test
    @DisplayName("DS-004: Zero hits returns empty list without error")
    void testSearchZeroHits() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("query", "Neurosurgery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("DS-005: Doctor search includes next available open slot")
    void testDoctorProfileWithNextSlot() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("query", "Patrick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nextAvailableSlot", notNullValue()));
    }

    @Test
    @DisplayName("DS-006: Inactive doctor (DOC-002) is hidden from patient-facing search")
    void testInactiveDoctorHiddenFromSearch() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("query", "Aline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
