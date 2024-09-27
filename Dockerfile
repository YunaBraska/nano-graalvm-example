#LINUX AMD64 && ARM64
# Oracle Images: https://github.com/graalvm/container/tree/master/graalvm-community
FROM debian:bookworm-slim AS graalvm_setup
ENV GRAALVM_HOME="/usr/local/graalvm"
ENV JAVA_VERSION="21"

# Releases: https://github.com/graalvm/graalvm-ce-builds/releases/
ENV AMD64="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz"
ENV ARM64="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-aarch64_bin.tar.gz"

# Prerequisites: https://www.graalvm.org/latest/reference-manual/native-image/#prerequisites
RUN set -eux; \
    GRAALVM_DOWNLOAD_URL=$(if [ "$(uname -m)" = "x86_64" ]; then echo "${AMD64}"; elif [ "$(uname -m)" = "aarch64" ]; then echo "${ARM64}"; else echo "Unsupported architecture [$(uname -m)]" && exit 1; fi) \
	&& mkdir -p "${GRAALVM_HOME}" \
    && apt-get -y update -qqy  \
    && apt-get -y install -qqy curl \
    && apt-get -y install -qqy --no-install-recommends build-essential libz-dev zlib1g-dev \
    && curl -sSL -o graalvm.tar.gz ${GRAALVM_DOWNLOAD_URL} \
    && tar -zxf graalvm.tar.gz -C "${GRAALVM_HOME}" --strip-components 1 \
    && rm -f graalvm.tar.gz \
	&& apt-get -y clean \
	&& rm -rf /var/lib/apt/lists/*

# COPY THE SOURCES
FROM graalvm_setup AS copy_sources
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw pom.xml ./

# BUILD THE NATIVE IMAGE
# [SYSTEM] ocker build -t app-native -f Dockerfile .
# [ARCH] docker buildx build --platform linux/amd64,linux/arm64,linux/386 --output type=docker -t app-native -f Dockerfile .
FROM copy_sources AS native_build
RUN (chmod +x mvnw && JAVA_HOME="${GRAALVM_HOME}" ./mvnw clean package -B -q -DskipTests -Pnative)

# USED TO EXPORT THE NATIVE IMAGE
# [SYSTEM] docker build -t app-native -f Dockerfile . && docker build --target export . --output target
# [ARCH] docker buildx build --platform linux/arm64 --output type=docker -t app-native -f Dockerfile . && docker buildx build --platform linux/arm64 --target export . --output target
FROM scratch AS export
COPY --from=native_build ./target/*.native ./app.native

# RUN THE NATIVE IMAGE
# [SYSTEM] docker build -t app-native -f Dockerfile . && docker run --rm -p 8080:8080 app-native
# [ARCH] docker buildx build --platform linux/arm64 --output type=docker -t app-native -f Dockerfile . && docker run --rm -p 8080:8080 app-native
FROM debian:bookworm-slim AS native_image
COPY --from=native_build ./target/*.native ./app.native
EXPOSE 8080
ENTRYPOINT ["./app.native"]
