package com.springboot.chatgpt.data;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class DoctorWithAvailabilityDTO {
    private Integer doctorId;
    private String name;
    private String specialty;
    private List<AvailabilityDTO> availability;

    public DoctorWithAvailabilityDTO(Integer doctorId, String name, String specialty, List<AvailabilityDTO> availability) {
        this.doctorId = doctorId;
        this.name = name;
        this.specialty = specialty;
        this.availability = availability;
    }

    @Data
    @Builder
    public static class AvailabilityDTO {
        private LocalDate date;
        private LocalTime time;
        private boolean isAvailable;

        public AvailabilityDTO(LocalDate date, LocalTime time, boolean isAvailable) {
            this.date = date;
            this.time = time;
            this.isAvailable = isAvailable;
        }

    }
}

