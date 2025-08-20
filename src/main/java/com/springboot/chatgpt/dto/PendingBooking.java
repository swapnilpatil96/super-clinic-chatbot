package com.springboot.chatgpt.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record PendingBooking(Integer doctorId, String doctorName, LocalDate date, LocalTime time) {
}
