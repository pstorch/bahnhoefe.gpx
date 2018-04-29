FROM openjdk:10-jdk

ARG VERSION

ENV JAVA_CONF_DIR=$JAVA_HOME/conf
ENV RSAPI_HOME=/opt/services
ENV RSAPI_WORK=/var/rsapi

# workaround for https://github.com/docker-library/openjdk/issues/101
RUN bash -c '([[ ! -d $JAVA_SECURITY_DIR ]] && ln -s $JAVA_HOME/lib $JAVA_HOME/conf) || (echo "Found java conf dir, package has been fixed, remove this hack"; exit -1)'

COPY config.yml $RSAPI_WORK/
COPY target/lib/ $RSAPI_HOME/lib/
EXPOSE 8080
EXPOSE 8081
WORKDIR $RSAPI_HOME
CMD [ "sh", "-c", "java --module-path 'lib' --add-modules java.xml.bind -m rsapi/org.railwaystations.api.RsApiApp server $RSAPI_WORK/config.yml"]
