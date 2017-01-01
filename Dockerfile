FROM java:8-jre

ARG VERSION

COPY config.yml /opt/services/
COPY target/bahnhoefe.service-$VERSION.jar /opt/services/service.jar
EXPOSE 8080
EXPOSE 8081
WORKDIR /opt/services
CMD ["java", "-jar", "service.jar", "server", "config.yml"]
