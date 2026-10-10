package rw.ac.rca.hoabs.slot;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rw.ac.rca.hoabs.doctor.Doctor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"doctor_id", "start_time"})
})
@Getter
@Setter
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private boolean isOpen = true;

    @Version
    @Column(nullable = false)
    private long version = 0;

    public boolean isOpen() {
        return isOpen;
    }

    public void setIsOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
}
