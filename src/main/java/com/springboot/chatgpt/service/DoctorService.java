package com.springboot.chatgpt.service;

import com.springboot.chatgpt.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;

    public DoctorService(DoctorRepository doctorRepository, DoctorAvailabilityRepository doctorAvailabilityRepository) {
        this.doctorRepository = doctorRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Optional<Doctor> findByName(String doctorName){
        return doctorRepository.findByNameIgnoreCase(doctorName);
    }

    public Optional<Doctor> findByNameLike(String namePattern){
        return doctorRepository.findByNameLike(namePattern);
    }

    public Optional<DoctorAvailability> findByDoctorAndAvailableDateAndAvailableTime(Doctor doctor, LocalDate date, LocalTime time){
        return doctorAvailabilityRepository.findByDoctorAndAvailableDateAndAvailableTime(doctor.getDoctorId(), date, java.sql.Time.valueOf(time));
    }

    public void updateDoctorAvailability(String sql){
        System.out.println("Update Doctor Availability:" + sql);
        jdbcTemplate.execute(sql);
    }

    public void saveDoctorAppointments(String sql){
        System.out.println("Save appointment:" +sql);
        jdbcTemplate.execute(sql);
    }

    public List<DoctorWithAvailabilityDTO> getAllDoctorsWithAvailability() {
        List<Doctor> doctors = doctorRepository.findAll();

        return doctors.stream().map(doctor -> {
            List<DoctorWithAvailabilityDTO.AvailabilityDTO> slots = doctorAvailabilityRepository
                    .findByDoctorAndIsAvailable(doctor, true)  // only available slots
                    .stream()
                    .map(slot -> new DoctorWithAvailabilityDTO.AvailabilityDTO(
                            slot.getAvailableDate(),
                            slot.getAvailableTime(),
                            slot.getIsAvailable()))
                    .collect(Collectors.toList());

            return new DoctorWithAvailabilityDTO(
                    doctor.getDoctorId(),
                    doctor.getName(),
                    doctor.getSpecialty(),
                    slots
            );
        }).collect(Collectors.toList());
    }

    public void saveDoctorAvailability(DoctorAvailability doctorAvailability) {
        doctorAvailabilityRepository.save(doctorAvailability);
    }
}

