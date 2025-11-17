# -----------------------------------------------
# 1단계: 빌드(Build) 환경 (Java 17 JDK)
# -----------------------------------------------
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# 소스코드 전체를 컨테이너로 복사
COPY . .

# ⚡️ [핵심 수정] Windows 줄바꿈(\r) 제거 (오류 해결)
RUN sed -i 's/\r$//' ./gradlew

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 빌드 실행 (데몬 없이)
RUN ./gradlew bootJar --no-daemon

# -----------------------------------------------
# 2단계: 실행(Run) 환경 (Java 17 JRE)
# -----------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

# 빌드 결과물 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]