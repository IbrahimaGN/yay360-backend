# ==================== STAGE 1 : BUILD ====================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build de l'application (les tests tournent séparément : mvn test)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==================== STAGE 2 : RUNTIME ====================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# curl sert uniquement au healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Utilisateur non-root pour la sécurité
RUN groupadd -r yay360 && useradd -r -g yay360 yay360

COPY --from=build /app/target/*.jar app.jar

# Version déployée (SHA du commit, fourni par la CD) : exposée sur /actuator/info pour la traçabilité
ARG VERSION=dev
ENV YAY360_VERSION=${VERSION}
LABEL org.opencontainers.image.source="https://github.com/IbrahimaGN/yay360-backend" \
      org.opencontainers.image.revision="${VERSION}"

RUN chown -R yay360:yay360 /app
USER yay360

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
