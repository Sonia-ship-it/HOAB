package rw.ac.rca.hoabs.department;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@Setter
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalTime openingTime = LocalTime.of(7, 0);

    @Column(nullable = false)
    private LocalTime closingTime = LocalTime.of(17, 0);

    @Column(nullable = false)
    private boolean active = true;
}
