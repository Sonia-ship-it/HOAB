package rw.ac.rca.hoabs.appointment;

import rw.ac.rca.hoabs.common.exception.ApiException;

public final class AppointmentStateMachine {

    private AppointmentStateMachine() {}

    public static boolean isLegalTransition(AppointmentStatus from, AppointmentStatus to) {
        if (from == to) {
            return true;
        }

        return switch (from) {
            case REQUESTED -> to == AppointmentStatus.CONFIRMED || to == AppointmentStatus.CANCELLED;
            case CONFIRMED -> to == AppointmentStatus.COMPLETED
                    || to == AppointmentStatus.CANCELLED
                    || to == AppointmentStatus.RESCHEDULED
                    || to == AppointmentStatus.NO_SHOW;
            case RESCHEDULED -> to == AppointmentStatus.CONFIRMED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }

    public static void assertTransition(AppointmentStatus from, AppointmentStatus to) {
        if (!isLegalTransition(from, to)) {
            throw ApiException.conflict("Illegal appointment status transition: " + from + " → " + to);
        }
    }
}
