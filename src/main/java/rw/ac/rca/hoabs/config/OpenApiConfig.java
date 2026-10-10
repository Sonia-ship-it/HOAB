package rw.ac.rca.hoabs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Amahoro General Hospital - HOABS API")
                        .description("REST API for Hospital Online Appointment Booking System (HOAB-SRS-2026-001)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Amahoro General Hospital OPD")
                                .email("info@amahoro.gov.rw")
                                .url("https://amahoro.gov.rw"))
                        .license(new License().name("Proprietary").url("https://amahoro.gov.rw/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Dev Server"),
                        new Server().url("https://staging.hoabs.example.rw").description("Staging UAT Server")
                ))
                .components(new Components().addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
