FROM eclipse-temurin:21-jdk-alpine AS builder
# Set the folder we work in inside the container
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# Download all valid dependencies defined in pom.xml
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring


COPY --from=builder /app/target/*.jar app.jar
# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]