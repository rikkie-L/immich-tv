FROM eclipse-temurin:17-jdk-jammy

# Android SDK tools
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends \
        wget unzip git \
    && rm -rf /var/lib/apt/lists/*

# Download Android command-line tools
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
         -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools && \
    mv /tmp/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager \
        "platform-tools" \
        "platforms;android-35" \
        "build-tools;35.0.0"

WORKDIR /project
COPY immich-tv/ .

# Cache Gradle dependencies as a separate layer
RUN ./gradlew dependencies --no-daemon -q || true

# Build the debug APK
RUN ./gradlew assembleDebug --no-daemon

# The APK ends up at:
# /project/app/build/outputs/apk/debug/app-debug.apk
