FROM openjdk:19-jdk-slim
LABEL authors="Bohdan_Sh"

WORKDIR /app

COPY target/booking-app-bs-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
