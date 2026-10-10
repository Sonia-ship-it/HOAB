package rw.ac.rca.hoabs.slot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slots")
@Tag(name = "Slots & Availability", description = "Endpoints for viewing real-time doctor availability calendars")
public class SlotController {

    private final SlotRepository slotRepository;

    public SlotController(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get doctor open slots for the next 14 days", description = "Returns available booking slots within the 14-day booking window (FR-SEARCH-04)")
    public ResponseEntity<List<SlotDto>> getDoctorOpenSlots(
            @PathVariable UUID doctorId,
            @RequestParam(required = false) Integer days) {
        int windowDays = (days != null && days > 0 && days <= 30) ? days : 14;
        Instant from = Instant.now();
        Instant to = from.plus(windowDays, ChronoUnit.DAYS);

        List<SlotDto> slots = slotRepository.findOpenSlotsInRange(doctorId, from, to)
                .stream()
                .map(SlotDto::from)
                .toList();

        return ResponseEntity.ok(slots);
    }
}
