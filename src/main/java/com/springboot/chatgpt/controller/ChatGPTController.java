package com.springboot.chatgpt.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.springboot.chatgpt.data.DoctorWithAvailabilityDTO;
import com.springboot.chatgpt.dto.ConfirmBooking;
import com.springboot.chatgpt.dto.PromptRequest;
import com.springboot.chatgpt.service.ChatGPTService;
import com.springboot.chatgpt.service.DoctorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatGPTController {

    private final ChatGPTService chatGPTService;
    private final DoctorService doctorService;

    public ChatGPTController(ChatGPTService chatGPTService, DoctorService doctorService) {
        this.chatGPTService = chatGPTService;
        this.doctorService = doctorService;
    }

    @PostMapping(path = "/completion-api")
    public String chat(@RequestBody PromptRequest promptRequest) throws JsonProcessingException {
        return chatGPTService.getChatResponse(promptRequest);
    }

    @GetMapping(path = "/getAllDoctors")
    public List<DoctorWithAvailabilityDTO> getAllDoctorsAvailability(){
        return doctorService.getAllDoctorsWithAvailability();
    }

    @PostMapping(path = "/handle-symptoms")
    public String handleSymptoms(@RequestParam String sessionId, @RequestBody PromptRequest promptRequest) throws JsonProcessingException {
        return chatGPTService.handleSymptoms(sessionId, promptRequest.prompt());
    }

    @PostMapping(path = "/confirm-booking")
    public String confirmBooking(@RequestParam String sessionId, @RequestBody ConfirmBooking confirmBooking) throws JsonProcessingException {
        return chatGPTService.confirmBooking(sessionId, confirmBooking);
    }
}
