package rw.ac.rca.hoabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import rw.ac.rca.hoabs.appointment.AppointmentStateMachine;
import rw.ac.rca.hoabs.appointment.AppointmentStatus;
import rw.ac.rca.hoabs.common.exception.ApiException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Appointment State Machine Domain Tests (FR-APPT-07, UAT AM-005)")
class AppointmentStateMachineTest {

    @ParameterizedTest(name = "Legal transition: {0} -> {1}")
    @CsvSource({
            "REQUESTED, CONFIRMED",
            "REQUESTED, CANCELLED",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, CANCELLED",
            "CONFIRMED, RESCHEDULED",
            "CONFIRMED, NO_SHOW",
            "RESCHEDULED, CONFIRMED"
    })
    void shouldAllowLegalTransitions(AppointmentStatus from, AppointmentStatus to) {
        assertTrue(AppointmentStateMachine.isLegalTransition(from, to));
        assertDoesNotThrow(() -> AppointmentStateMachine.assertTransition(from, to));
    }

    @ParameterizedTest(name = "Illegal transition: {0} -> {1}")
    @CsvSource({
            "COMPLETED, CONFIRMED",
            "COMPLETED, CANCELLED",
            "COMPLETED, REQUESTED",
            "CANCELLED, CONFIRMED",
            "CANCELLED, COMPLETED",
            "CANCELLED, NO_SHOW",
            "NO_SHOW, CONFIRMED",
            "NO_SHOW, COMPLETED",
            "REQUESTED, COMPLETED"
    })
    void shouldRejectIllegalTransitionsWithConflict(AppointmentStatus from, AppointmentStatus to) {
        assertFalse(AppointmentStateMachine.isLegalTransition(from, to));
        ApiException ex = assertThrows(ApiException.class, () -> AppointmentStateMachine.assertTransition(from, to));
        assertEquals(409, ex.status().value());
        assertTrue(ex.getMessage().contains("Illegal appointment status transition"));
    }

    @Test
    void sameStatusShouldBeNoOp() {
        assertTrue(AppointmentStateMachine.isLegalTransition(AppointmentStatus.CONFIRMED, AppointmentStatus.CONFIRMED));
    }
}
