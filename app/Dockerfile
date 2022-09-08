# Install the base requirements for the app.
# This stage is to support development.
FROM gradle:7.5.1-jdk11 AS builder
WORKDIR /home/gradle/src
COPY . /home/gradle/src
RUN gradle clean
RUN gradle installDist

FROM openjdk:17-oracle
RUN microdnf install findutils
COPY --from=builder /home/gradle/src/build/install/src/ /app/
COPY --from=builder /home/gradle/src/build/resources/main/ /app/resources
WORKDIR /app

CMD ["bin/src"]
