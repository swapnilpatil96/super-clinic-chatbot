package com.springboot.chatgpt.data;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "doctors")
@Data
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer doctorId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialty;

    public Doctor() {
        //default constructor
    }

    public Doctor(Integer doctorId, String name, String specialty, List<DoctorAvailability> availabilities) {
        this.doctorId = doctorId;
        this.name = name;
        this.specialty = specialty;
        this.availabilities = availabilities;
    }

    // Optional: for bidirectional relationship
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoctorAvailability> availabilities;

    // Getters & Setters
}
