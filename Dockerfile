FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Maven installieren
RUN apk add --no-cache curl tar bash procps \
    && curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz | tar xzf - -C /opt \
    && ln -s /opt/apache-maven-3.9.5 /opt/maven

ENV PATH="/opt/maven/bin:${PATH}"

# Projekt kopieren und bauen
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
