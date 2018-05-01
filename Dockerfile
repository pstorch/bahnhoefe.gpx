FROM java:9-jre

ARG VERSION

ENV JAVA_CONF_DIR=$JAVA_HOME/conf

# workaround for https://github.com/docker-library/openjdk/issues/101
RUN bash -c '([[ ! -d $JAVA_SECURITY_DIR ]] && ln -s $JAVA_HOME/lib $JAVA_HOME/conf) || (echo "Found java conf dir, package has been fixed, remove this hack"; exit -1)'

COPY config.yml /opt/services/
COPY target/rsapi-$VERSION.jar /opt/services/service.jar
EXPOSE 8080
EXPOSE 8081
WORKDIR /opt/services
CMD ["java", "-jar", "service.jar", "server", "config.yml"]
