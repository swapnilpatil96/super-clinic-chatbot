package com.springboot.chatgpt.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Integer> {
    List<DoctorAvailability> findByDoctorAndAvailableDateAndIsAvailable(
            Doctor doctor, LocalDate date, Boolean isAvailable);

    @Query(value = "SELECT * FROM doctor_availability " +
            "WHERE doctor_id = :doctorId " +
            "AND available_date = :date " +
            "AND CAST(available_time AS TIME) = CAST(:time AS TIME)",
            nativeQuery = true)
    Optional<DoctorAvailability> findByDoctorAndAvailableDateAndAvailableTime(
            @Param("doctorId") int doctorId, @Param("date") LocalDate date, @Param("time") java.sql.Time time);

    List<DoctorAvailability> findByDoctorAndIsAvailable(Doctor doctor, boolean isAvailable);

}
