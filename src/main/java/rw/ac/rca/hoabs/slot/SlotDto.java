package rw.ac.rca.hoabs.slot;

import java.time.Instant;
import java.util.UUID;

public record SlotDto(
        UUID id,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        boolean isOpen
) {
    public static SlotDto from(Slot s) {
        return new SlotDto(s.getId(), s.getDoctor().getId(), s.getStartTime(), s.getEndTime(), s.isOpen());
    }
}
