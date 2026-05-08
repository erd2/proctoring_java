package com.aiu.proctoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Proctoring System main application.
 * Backend service for academic integrity monitoring during remote exams.
 */
@SpringBootApplication
public class ProctoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProctoringApplication.class, args);
    }
}
