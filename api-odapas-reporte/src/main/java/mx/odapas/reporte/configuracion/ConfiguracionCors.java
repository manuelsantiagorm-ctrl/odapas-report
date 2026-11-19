package mx.odapas.reporte.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite que el frontend de Angular (http://localhost:4200)
 * consulte esta API (http://localhost:8080) desde el navegador.
 *
 * Sin esta configuracion el navegador bloquea las peticiones
 * entre puertos distintos (politica CORS).
 */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200",
                        "http://aguapp.test:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
