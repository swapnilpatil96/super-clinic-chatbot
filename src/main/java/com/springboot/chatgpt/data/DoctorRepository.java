package com.springboot.chatgpt.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
    Optional<Doctor> findByNameIgnoreCase(String name);

    List<Doctor> findBySpecialtyIgnoreCase(String specialty);

    @Query(value = "SELECT * FROM doctors WHERE name LIKE %:name%", nativeQuery = true)
    Optional<Doctor> findByNameLike(@Param("name") String name);
}

