FROM gradle:4.3.0-jdk8 as builder
USER root
ENV APP_DIR /app
WORKDIR $APP_DIR
COPY . $APP_DIR
RUN ./gradlew build shadowJar

FROM openjdk:8-jre-alpine
ENV APP_DIR /app
WORKDIR $APP_DIR
COPY --from=builder $APP_DIR/src/main/resources/*.txt \
  $APP_DIR/src/main/resources/application.conf \
  $APP_DIR/src/main/resources/logback.xml \
  $APP_DIR/build/libs/smartcam-serve-all.jar \
  $APP_DIR/

CMD ["java", "-jar", "smartcam-serve-all.jar"]
