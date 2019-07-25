FROM openjdk:12

ENV JAVA_CONF_DIR=$JAVA_HOME/conf
ENV RSAPI_HOME=/opt/services
ENV RSAPI_WORK=/var/rsapi

ADD config.yml $RSAPI_WORK/
# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD target/lib           $RSAPI_HOME/lib
# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} $RSAPI_HOME/rsapi.jar

EXPOSE 8080
EXPOSE 8081
WORKDIR $RSAPI_HOME
CMD [ "sh", "-c", "java -jar rsapi.jar server $RSAPI_WORK/config.yml"]
