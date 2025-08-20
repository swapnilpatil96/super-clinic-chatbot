package com.springboot.chatgpt.service;

import com.springboot.chatgpt.data.Appointment;
import com.springboot.chatgpt.data.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public void save(Appointment appointment) {
        appointmentRepository.save(appointment);
    }
}
