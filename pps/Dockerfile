FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/pps-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
