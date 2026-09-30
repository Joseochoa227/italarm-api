package co.italarm.api.shared.infraestructura;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tareas programadas dentro del backend (sección 9.2): TRM diaria y, más adelante, vencimientos.
 */
@Configuration
@EnableScheduling
public class ConfiguracionTareas {}
