# Estágio de Build
FROM maven:3.8.5-openjdk-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

# Estágio de Execução
FROM openjdk:17-jdk-slim
COPY --from=build /home/app/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app.jar"]
