package com.springboot.chatgpt.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByDoctorAndAppointmentDate(Doctor doctor, LocalDate date);
}
