# Етап 1: Збірка (Build stage)
# Використовуємо образ з Maven та JDK 21 для компіляції проекту
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копіюємо файл конфігурації Maven (pom.xml) та завантажуємо залежності
# Це дозволяє кешувати цей шар, якщо pom.xml не змінився
COPY pom.xml .
RUN mvn dependency:go-offline

# Копіюємо вихідний код проекту
COPY src ./src

# Збираємо проект у JAR-файл, пропускаючи тести для швидкості
RUN mvn clean package -DskipTests

# Етап 2: Запуск (Runtime stage)
# Використовуємо легкий образ JRE (лише для запуску Java)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копіюємо скомпільований JAR-файл з етапу збірки
# Важливо: Spring Boot створює jar у папці target. Ім'я може залежати від version/artifactId у pom.xml
# Використовуємо wildcard (*), щоб не залежати від конкретної версії у назві файлу
COPY --from=build /app/target/*.jar app.jar

# Відкриваємо порт 8080 (стандартний для Spring Boot)
EXPOSE 8080

# Команда для запуску застосунку
ENTRYPOINT ["java", "-jar", "app.jar"]