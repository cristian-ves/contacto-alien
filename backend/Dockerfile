# Build stage: compiles the grammars and packages the executable jar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Runtime stage: JRE plus the RISC-V toolchain and emulator used to assemble and run generated programs
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends binutils-riscv64-linux-gnu qemu-user \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /build/target/contacto-alien-backend-0.0.1-SNAPSHOT.jar app.jar
USER app

# Sized for a 512 MB instance: the heap is capped low because every running program also spawns a qemu process
ENV JAVA_OPTS="-XX:+UseSerialGC -Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss512k"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
