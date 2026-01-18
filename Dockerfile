FROM eclipse-temurin:17-jdk-jammy

# Set build arguments for SDK components
ARG ANDROID_SDK_VERSION=11076708_latest
ARG ANDROID_BUILD_TOOLS=34.0.0
ARG ANDROID_PLATFORM=android-34

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/tools/bin:$PATH

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget unzip ca-certificates dos2unix && \
    rm -rf /var/lib/apt/lists/*

# Install Android command line tools
RUN mkdir -p /opt/android-sdk/cmdline-tools && \
    cd /opt/android-sdk/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip && \
    rm cmdline-tools.zip && \
    mv cmdline-tools latest

# Install required platform, build-tools and platform-tools, accept licenses
RUN yes | sdkmanager --licenses >/dev/null && \
    sdkmanager "platform-tools" "platforms;${ANDROID_PLATFORM}" "build-tools;${ANDROID_BUILD_TOOLS}"

WORKDIR /workspace

# Pre-download Gradle wrapper and dependencies (cache friendly)
COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY gradle.properties gradle.properties
RUN dos2unix gradlew && chmod +x gradlew && ./gradlew --no-daemon tasks > /dev/null || true

# Default command builds the debug APK
CMD ["sh", "-c", "dos2unix gradlew 2>/dev/null || true && chmod +x gradlew && ./gradlew --no-daemon assembleDebug"]
