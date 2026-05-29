# ==========================================
# Stage 1: Build Stage (Membangun Aplikasi)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set direktori kerja di dalam kontainer
WORKDIR /app

# Salin file konfigurasi gradle dan source code
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Beri hak akses eksekusi pada gradlew dan lakukan proses build
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

# ==========================================
# Stage 2: Run Stage (Menjalankan Aplikasi)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Salin HANYA file .jar hasil build dari Stage 1 (membuat ukuran kontainer jauh lebih kecil)
COPY --from=builder /app/build/libs/*.jar app.jar

# Buka port 3002 agar bisa diakses dari luar kontainer
EXPOSE 3002

# Perintah yang dijalankan saat kontainer menyala
ENTRYPOINT ["java", "-jar", "app.jar"]
