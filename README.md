🏥 Doctor’s Assistant Chatbot

A Spring Boot + OpenAI Function Calling powered chatbot that helps patients book doctor appointments using natural language.
It integrates with a relational database (SQL Server in this setup) to fetch doctor availability, recommend specialists based on symptoms, and update bookings.

✨ Features

💬 Conversational chatbot flow using OpenAI Completion API with Function Calling

🩺 Symptom-based doctor recommendation (e.g., rashes → dermatologist)

📅 Real-time doctor availability check from SQL Server DB

🔄 Alternate slot suggestions if requested time is unavailable

📝 Appointment booking with patient details stored in DB

🔐 Retry and rate limiting logic for stable OpenAI API calls

🏗️ Tech Stack

Java 24

Spring Boot 3.x

Maven for dependency management

OpenAI API (Function Calling)

SQL Server (Doctor & Availability DB)

Hibernate / JPA
