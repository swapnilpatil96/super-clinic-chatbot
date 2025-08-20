package com.springboot.chatgpt.data;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_availability")
@Data
public class DoctorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer availabilityId;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "available_date", nullable = false, columnDefinition = "DATE")
    private LocalDate availableDate;

    @Column(name = "available_time", nullable = false, columnDefinition = "TIME")
    private LocalTime availableTime;

    private Boolean isAvailable = true;

    // Getters & Setters
}
