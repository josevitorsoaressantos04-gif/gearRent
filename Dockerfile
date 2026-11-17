# Etapa 1: Build da aplicação com Maven e Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copia os arquivos de configuração do Maven e o código-fonte
COPY pom.xml .
COPY src ./src

# Executa o build pulando os testes para acelerar a imagem
RUN mvn clean package -DskipTests

# Etapa 2: Execução com JRE 17 enxuto
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia o JAR gerado na etapa de build
COPY --from=builder /app/target/gearRent-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando para inicializar o back-end
ENTRYPOINT ["java", "-jar", "app.jar"]