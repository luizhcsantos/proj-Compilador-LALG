package br.unesp.compilerLALG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompiladorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompiladorApplication.class, args);
        System.out.println("🚀 Servidor Spring Boot rodando em http://localhost:8080");
    }
}
