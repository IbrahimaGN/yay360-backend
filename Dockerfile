# ==================== STAGE 1 : BUILD ====================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build de l'application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==================== STAGE 2 : RUNTIME ====================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN groupadd -r yay360 && useradd -r -g yay360 yay360

COPY --from=build /app/target/*.jar app.jar

RUN chown -R yay360:yay360 /app
USER yay360

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
