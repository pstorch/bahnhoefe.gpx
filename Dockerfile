FROM anapsix/alpine-java:jre8
COPY config.yml /opt/services/
COPY target/bahnhoefe.service-0.0.1-SNAPSHOT.jar /opt/services/service.jar
EXPOSE 8080
EXPOSE 8081
WORKDIR /opt/services
CMD ["java", "-jar", "service.jar", "server", "config.yml"]
