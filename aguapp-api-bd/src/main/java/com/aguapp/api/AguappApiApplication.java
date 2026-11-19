package com.aguapp.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST de AguApp.
 * Al ejecutarla, Spring Boot levanta un servidor Tomcat en http://localhost:8080
 */
@SpringBootApplication
public class AguappApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AguappApiApplication.class, args);
    }
}
