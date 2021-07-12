FROM openjdk:11 AS build
ENV APP_HOME=/root/dev/rsapi/
RUN mkdir -p $APP_HOME/src/main/java
WORKDIR $APP_HOME
COPY build.gradle settings.gradle gradlew $APP_HOME
COPY gradle $APP_HOME/gradle
# Dependencies
RUN ./gradlew build -x bootJar -x test --continue
COPY . .
RUN ./gradlew build -x pmdMain -x pmdTest -x spotbugsMain -x spotbugsTest

FROM openjdk:11
ENV RSAPI_HOME=/opt/services/
ENV RSAPI_WORK=/var/rsapi
ENV ARTIFACT_NAME=rsapi-0.0.1-SNAPSHOT.jar
WORKDIR $RSAPI_HOME

COPY --from=build /root/dev/rsapi/build/libs/$ARTIFACT_NAME .

EXPOSE 8080
EXPOSE 8081
CMD [ "java", "-jar", $ARTIFACT_NAME]
