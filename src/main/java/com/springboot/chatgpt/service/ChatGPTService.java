package com.springboot.chatgpt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.chatgpt.data.Appointment;
import com.springboot.chatgpt.data.Doctor;
import com.springboot.chatgpt.data.DoctorAvailability;
import com.springboot.chatgpt.data.DoctorWithAvailabilityDTO;
import com.springboot.chatgpt.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ChatGPTService {

    @Value("${openapi.api.key}")
    private String apiKey;

    @Value("${openapi.api.model}")
    private String model;

    private final RestClient restClient;

    private final DoctorService doctorService;

    private final AppointmentService appointmentService;

    List<ChatGPTRequest.Message> conversationHistory = new ArrayList<>();

    List<ChatMessage> conversationHistoryForFunctionCalling = new ArrayList<>();

    String systemMessage = "You are a chatbot assistant to Super Clinic that answers queries in natural language about doctor availability.\n" +
            "\n" +
            "Always start by greeting the caller: \"Hello and Welcome to the Super Clinic.\"\n" +
            "\n" +
            "Based on the caller's symptoms, recommend a suitable doctor. If there are multiple doctors for the same specialty, list them and ask the caller to choose.\n" +
            "\n" +
            "If a specific doctor is requested, check their availability:\n" +
            "- If the requested date and time is unavailable, suggest an alternative date and time.\n" +
            "- Do not allow appointments for **past dates or times**. You must validate that the requested appointment and doctor availability are both in the **future only** based on today's date.\n" +
            "\n" +
            "If no doctor is available in the future, respond clearly: \"Sorry, there are no future appointments available currently.\"\n" +
            "\n" +
            "Once a slot is agreed upon:\n" +
            "- Ask the caller for their **name and contact number**.\n" +
            "- Only after these details are provided and booking is done, Return **only** the two SQL statements. " +
            " Generate the SQLs to:\n" +
            "  1. Insert a row into the `appointments` table.\n" +
            "  2. Update the `doctor_availability` table to mark the slot as unavailable.\n" +
            "\n" +
            "### Response Format Example (strictly follow):\n" +
            "SQL1 => insert into appointments (doctor_id, patient_name, patient_contact, appointment_date, appointment_time)" +
            "values (1, 'Swapnil Patil', '9764019013', '2025-07-23', '10:00:00')" +
            "#SQL2 => update doctor_availability set is_available=0 where doctor_id=1 and available_date='2025-07-23' and available_time='10:00:00'" +
            //  "⚠\uFE0F Do not add any greeting, confirmation message, or comments before or after the SQLs. Return **only** the two SQL statements, prefixed with `SQL1 =>` and `SQL2 =>`." +
            "Here is the list of available doctors and their availability:\n";

    String systemMessageForFunctionCalling = """
            You are a chatbot assistant for Super Clinic that helps patients book appointments.
            
            Rules:
            1. Always greet first when the user says hello or starts the conversation:
               "Hello and Welcome to the Super Clinic."
            
            2. Function calling rules:
               - Only call the function "recommend_doctor" when the user provides symptoms or specifies a doctor name.
               - Do not call the function if the user only greets, asks general questions, or does not provide symptoms/doctor details.
               - Function must be called with: doctor_name, date (yyyy-MM-dd), and time (HH:mm:ss).
               - Do not include any extra text along with the function call.
            
            3. Understanding intent:
               - If symptoms are given, recommend the most suitable doctor.
                 - If multiple doctors match, list them and ask the user to choose.
               - If a specific doctor is requested, check availability.
               - If the user has not yet provided symptoms or a doctor name, respond naturally in conversation and guide them.
            
            4. Appointment validation:
               - Do not allow bookings for past dates or times. Validate that both the requested appointment and the doctor’s availability are in the future only.
               - If the requested slot is unavailable, suggest the next available one.
               - If no doctors are available in the future, respond:
                 "Sorry, there are no future appointments available currently."
            """;

    private static final Map<String, PendingBooking> sessionBookings = new HashMap<>();

    public ChatGPTService(RestClient restClient, DoctorService doctorService, AppointmentService appointmentService) {
        this.restClient = restClient;
        this.doctorService = doctorService;

        this.appointmentService = appointmentService;
        this.conversationHistory.add(new ChatGPTRequest.Message("system", systemMessage));
        this.conversationHistoryForFunctionCalling.add(new ChatMessage("system", systemMessageForFunctionCalling, null));
    }

    public String getChatResponse(PromptRequest promptRequest) throws JsonProcessingException {

        List<DoctorWithAvailabilityDTO> allDoctorsWithAvailability = doctorService.getAllDoctorsWithAvailability();
        String doctorsAvailabilities = convertToString(allDoctorsWithAvailability);

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        System.out.println("Today is " + currentDate);

        System.out.println("Doctors availability::" + doctorsAvailabilities);

        conversationHistory.add(new ChatGPTRequest.Message("user", doctorsAvailabilities));
        conversationHistory.add(new ChatGPTRequest.Message("user", "Today is " + currentDate));
        conversationHistory.add(new ChatGPTRequest.Message("user", promptRequest.prompt()));

        ChatGPTRequest chatGPTRequest = new ChatGPTRequest(model, conversationHistory);

        System.out.println("System message::" + systemMessage);

        ChatGPTResponse chatGPTResponse = restClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(chatGPTRequest)
                .retrieve()
                .body(ChatGPTResponse.class);

        if (chatGPTResponse.choices().get(0).message().content().contains("SQL")) {
            return confirmBooking(chatGPTResponse);
        }
        conversationHistory.add(new ChatGPTRequest.Message("assistant", chatGPTResponse.choices().get(0).message().content()));
        conversationHistory.remove(1);
        conversationHistory.remove(2);
        return chatGPTResponse.choices().get(0).message().content();
    }

    private String confirmBooking(ChatGPTResponse chatGPTResponse) {
        System.out.println("Response with SQL:" + chatGPTResponse.choices().get(0).message().content());
        System.out.println("==============================================================================");
        conversationHistory.add(new ChatGPTRequest.Message("assistant", chatGPTResponse.choices().get(0).message().content()));
        String[] strings = chatGPTResponse.choices().get(0).message().content().split("#");
        String sqlOneString = strings[0];
        String[] sqlOneSplits = sqlOneString.split("=>");
        String sqlOne = sqlOneSplits[1];
        String sqlTwoString = strings[1];
        String[] sqlTwoSplits = sqlTwoString.split("=>");
        String sqlTwo = sqlTwoSplits[1];
        doctorService.saveDoctorAppointments(sqlOne);
        doctorService.updateDoctorAvailability(sqlTwo);

        //return "Your appointment has been successfully booked. Thank you!";

        conversationHistory.add(new ChatGPTRequest.Message("user", "SQL update has been done so now" +
                " please generate a polite booking confirmation message to the user"));
        ChatGPTRequest confirmationRequest = new ChatGPTRequest(model, conversationHistory);

        ChatGPTResponse confirmationResponse = restClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(confirmationRequest)
                .retrieve()
                .body(ChatGPTResponse.class);

        return confirmationResponse.choices().get(0).message().content();
    }

    private String convertToString(List<DoctorWithAvailabilityDTO> allDoctorsWithAvailability) throws JsonProcessingException {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h a"); // e.g., 10 AM
        List<String> doctorsAvailabilities = new ArrayList<>();
        for (DoctorWithAvailabilityDTO doctor : allDoctorsWithAvailability) {
            for (DoctorWithAvailabilityDTO.AvailabilityDTO slot : doctor.getAvailability()) {
                if (slot.isAvailable()) {
                    String formattedTime = LocalTime.parse(slot.getTime().toString()).format(timeFormatter).toLowerCase();
                    String output = String.format("%d %s with speciality as %s available on %s at %s",
                            doctor.getDoctorId(),
                            doctor.getName(),
                            doctor.getSpecialty(),
                            slot.getDate(),
                            formattedTime);
                    doctorsAvailabilities.add(output);
                }
            }
        }
        return String.join(",", doctorsAvailabilities);
    }

    public String handleSymptoms(String sessionId, String userInput) throws JsonProcessingException {
        List<DoctorWithAvailabilityDTO> allDoctorsWithAvailability = doctorService.getAllDoctorsWithAvailability();
        String doctorsAvailabilities = convertToString(allDoctorsWithAvailability);

        ChatMessage userMessage = new ChatMessage("user", "Symptoms: " + userInput + "\nAvailable doctors:\n" + doctorsAvailabilities, null);

        conversationHistoryForFunctionCalling.add(userMessage);
        conversationHistoryForFunctionCalling.add(new ChatMessage("user", "Today is " + LocalDate.now(), null));

        boolean callFunction = detectMedicalIntent(userInput);
        ChatCompletionRequest request;

        if (callFunction) {
            Map<String, FunctionDefinition.Parameters.Property> props = Map.of(
                    "doctor_name", new FunctionDefinition.Parameters.Property("string", "Name of the doctor for the appointment."),
                    "date", new FunctionDefinition.Parameters.Property("string", "Appointment date in 'yyyy-MM-dd' format. Example: 2025-08-12"),
                    "time", new FunctionDefinition.Parameters.Property("string", "Appointment time in 24-hour format 'HH:mm:ss'. Example: 14:30:00")
            );

            FunctionDefinition function = new FunctionDefinition("recommend_doctor", "Recommend a doctor based on symptoms and availability",
                    new FunctionDefinition.Parameters(props, List.of("doctor_name", "date", "time"))
            );

            request = new ChatCompletionRequest(
                    model,
                    conversationHistoryForFunctionCalling,
                    List.of(function),
                    new ChatCompletionRequest.FunctionCallRequest("recommend_doctor"),
                    0.5,
                    300);
        } else {
            request = new ChatCompletionRequest(
                    model,
                    conversationHistoryForFunctionCalling,
                    null,
                    null,
                    0.5,
                    300);
        }

        System.out.println("User message::" + userMessage.content());

        ChatCompletionResponse response = restClient.post().header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json").body(request).retrieve().body(ChatCompletionResponse.class);

        System.out.println("GPT Full Response: " + response);

        conversationHistoryForFunctionCalling.add(new ChatMessage("assistant", response.choices().get(0).message().content(), null));
        conversationHistoryForFunctionCalling.remove(1);
        conversationHistoryForFunctionCalling.remove(2);

        ChatMessage assistantMessage = response.choices().get(0).message();
        if (assistantMessage.function_call() != null) {
            return processFunctionCall(sessionId, assistantMessage.function_call().arguments());
        } else {
            return assistantMessage.content();
        }
    }

    private boolean detectMedicalIntent(String input) {
        if (input == null || input.isBlank()) return false;
        final Pattern SYMPTOM_PATTERN = Pattern.compile(
                "\\b(pain|fever|cough|cold|flu|headache|stomach ache|nausea|vomit|infection|rash|sore throat|dizziness)\\b",
                Pattern.CASE_INSENSITIVE);

        final Pattern APPOINTMENT_PATTERN = Pattern.compile(
                "\\b(book|schedule|appointment|see|consult|visit)\\b",
                Pattern.CASE_INSENSITIVE);

        final Pattern DOCTOR_PATTERN = Pattern.compile(
                "\\b(dr\\.?\\s?[A-Z][a-z]+|doctor\\s+[A-Z][a-z]+)\\b",
                Pattern.CASE_INSENSITIVE);

        String lower = input.toLowerCase();

        // Check symptoms
        if (SYMPTOM_PATTERN.matcher(lower).find()) {
            return true;
        }

        // Check if asking for appointment with a doctor
        if (APPOINTMENT_PATTERN.matcher(lower).find() || DOCTOR_PATTERN.matcher(lower).find()) {
            return true;
        }

        return false;
    }

    private String processFunctionCall(String sessionId, String jsonArgs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> args = mapper.readValue(jsonArgs, Map.class);
            String doctorName = args.get("doctor_name");
            LocalDate date = LocalDate.parse(args.get("date"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime time = LocalTime.parse(args.get("time"), DateTimeFormatter.ofPattern("HH:mm:ss"));

            System.out.println("Parsed response::" + date + " and " + time);

            Optional<Doctor> doctor = doctorService.findByNameLike(doctorName);
            if (doctor.isEmpty()) return "Doctor not found.";

            sessionBookings.put(sessionId, new PendingBooking(doctor.get().getDoctorId(), doctorName, date, time));
            return "Recommended appointment with " + doctorName + " on " + date + " at " + time + ".\nPlease provide your name and phone number to confirm the booking.";

        } catch (Exception e) {
            return "Error parsing function call: " + e.getMessage();
        }
    }

    public String confirmBooking(String sessionId, ConfirmBooking confirmBooking) {
        PendingBooking booking = sessionBookings.get(sessionId);
        if (booking == null) return "No pending appointment found for this session.";

        Appointment appointment = buildAppointment(confirmBooking.getPatientName(), confirmBooking.getPhone(), booking);

        appointmentService.save(appointment);

        System.out.println("After saving appointment");

        System.out.println("Booking date:" + booking.date() + " and Booking time:" + booking.time());
        DoctorAvailability doctorAvailability = doctorService.findByDoctorAndAvailableDateAndAvailableTime(buildDoctor(booking), booking.date(), booking.time())
                .orElseThrow(() -> new RuntimeException("Doctor availability not found"));

        doctorAvailability.setIsAvailable(Boolean.FALSE);

        System.out.println("Before updating doctor's availability>>>>" + doctorAvailability.getIsAvailable());
        doctorService.saveDoctorAvailability(doctorAvailability);

        sessionBookings.remove(sessionId);
        return "Appointment confirmed with " + booking.doctorName() + " on " + booking.date() + " at " + booking.time() + ".";
    }

    private static Appointment buildAppointment(String patientName, String phone, PendingBooking booking) {
        return Appointment.builder().doctor(buildDoctor(booking))
                .appointmentDate(booking.date())
                .appointmentTime(booking.time())
                .patientName(patientName)
                .patientContact(phone)
                .build();
    }

    private static Doctor buildDoctor(PendingBooking booking) {
        return Doctor.builder().doctorId(booking.doctorId()).build();
    }
}
