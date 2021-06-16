FROM openjdk:11 AS build
ENV APP_HOME=/root/dev/myapp/
RUN mkdir -p $APP_HOME/src/main/java
WORKDIR $APP_HOME
COPY build.gradle gradlew gradlew.bat $APP_HOME
COPY gradle $APP_HOME/gradle
# Dependencies
RUN ./gradlew build -x :bootRepackage -x test --continue
COPY . .
RUN ./gradlew build

FROM openjdk:11

ENV RSAPI_HOME=/opt/services
ENV RSAPI_WORK=/var/rsapi

COPY --from=build /usr/src/app/build/libs/*.jar $RSAPI_HOME
COPY config.yml $RSAPI_WORK/

EXPOSE 8080
EXPOSE 8081
WORKDIR $RSAPI_HOME
CMD [ "sh", "-c", "java -jar rsapi.jar db migrate -i $RSAPI_LB_CONTEXT $RSAPI_WORK/config.yml && java -jar rsapi.jar server $RSAPI_WORK/config.yml"]
