FROM openjdk:8-jre-slim

ENV STYX_HOME=/styx
ENV CONFIG_PATH=/conf
WORKDIR ${STYX_HOME}

# Copy over my custom startup file
COPY distribution/conf/default.yml ${STYX_HOME}/default.yml
COPY ory-hydra-styx-plugin-0.0.1-jar-with-dependencies.jar /
COPY distribution/target/styx-1.0-SNAPSHOT-osx-x86_64.zip ${STYX_HOME}/styx.zip

RUN unzip ${STYX_HOME}/styx.zip \
    && mv styx-1.0-SNAPSHOT styx \
    && rm styx.zip \
    && mkdir -p default-config \
    && cp styx/conf/styx-env.sh default-config/. \
    && cp ${STYX_HOME}/default.yml default-config/default.yml \
    && cp styx/conf/origins.yml default-config/. \
    && mkdir -p config

EXPOSE 8080 8443 9000

CMD ["default-config/default.yml"]

ENTRYPOINT ["styx/bin/startup"]
