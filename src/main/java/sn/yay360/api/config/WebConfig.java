package sn.yay360.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    /**
     * Origines autorisées, séparées par des virgules (variable YAY360_CORS_ORIGINS).
     * Par défaut : web Angular et Flutter Web en local, émulateur Android.
     * En production, n'indiquer que les domaines réels (ex. https://app.yay360.sn).
     */
    @Value("${yay360.cors.origines:http://localhost:*,http://127.0.0.1:*,http://10.0.2.2:*}")
    private String origines;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.stream(origines.split(","))
                .map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Authentification par jeton dans l'en-tête Authorization : aucun cookie à partager
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
