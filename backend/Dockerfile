FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
ENV TZ=Asia/Kolkata
ENTRYPOINT ["java","-jar","app.jar"]
