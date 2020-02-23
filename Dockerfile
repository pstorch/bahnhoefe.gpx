FROM maven:3-jdk-11-openj9 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:12

ENV RSAPI_HOME=/opt/services
ENV RSAPI_WORK=/var/rsapi

COPY --from=build /usr/src/app/target/rsapi-1.0.0-SNAPSHOT.jar $RSAPI_HOME/rsapi.jar
COPY config.yml $RSAPI_WORK/
# Add Maven dependencies (not shaded into the artifact; Docker-cached)
COPY --from=build /usr/src/app/target/lib           $RSAPI_HOME/lib

EXPOSE 8080
EXPOSE 8081
WORKDIR $RSAPI_HOME
CMD [ "sh", "-c", "java -jar rsapi.jar db migrate -i $RSAPI_LB_CONTEXT $RSAPI_WORK/config.yml && java -jar rsapi.jar server $RSAPI_WORK/config.yml"]
