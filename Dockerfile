# -------- BUILD STAGE --------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# -------- RUN STAGE --------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Railway automatically sets PORT environment variable
# The application.yaml uses ${PORT:8088} which will pick it up
EXPOSE ${PORT:-8088}
ENTRYPOINT ["java","-jar","app.jar"]