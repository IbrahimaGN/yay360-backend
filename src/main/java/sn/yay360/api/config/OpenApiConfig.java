package sn.yay360.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_JWT = "bearerAuth";

    @Bean
    public OpenAPI yay360OpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("YAY360 API")
                        .description("Plateforme web et mobile intelligente de suivi prénatal de la santé "
                                + "maternelle destinée aux femmes enceintes et aux professionnels de santé, "
                                + "avec partage familial sécurisé.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("YAY360")
                                .email("admin@yay360.sn")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_JWT))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_JWT, new SecurityScheme()
                                .name(SCHEME_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Coller le token JWT obtenu via /auth/login (sans le préfixe 'Bearer')")));
    }
}
