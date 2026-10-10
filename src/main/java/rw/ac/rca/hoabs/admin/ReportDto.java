package rw.ac.rca.hoabs.admin;

import java.util.Map;

public record ReportDto(
        long totalAppointments,
        long confirmedCount,
        long completedCount,
        long cancelledCount,
        long noShowCount,
        double overallNoShowRate,
        Map<String, Map<String, Long>> departmentCounts,
        Map<String, Double> doctorNoShowRates,
        Map<String, Double> departmentNoShowRates
) {}
