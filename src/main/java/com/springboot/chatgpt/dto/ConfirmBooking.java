package com.springboot.chatgpt.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfirmBooking {
    private String patientName;
    private String phone;
}
