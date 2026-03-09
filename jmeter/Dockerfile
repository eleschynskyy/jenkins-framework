FROM ubuntu:22.04

ARG JMETER_VERSION=5.6.3
ARG JMETER_HOME=/opt/jmeter

# Install required utilities
RUN apt-get update && apt-get upgrade -y &&\
    apt-get install -y openjdk-25-jdk unzip zip curl sed wget make apt gcc ca-certificates libc-bin libssl-dev git python3 python3-pip python3-dev libxml2-dev libxslt-dev groff bc python3-aiomeasures jq bc && \
    rm -rf /var/lib/apt/lists/*

# Install JMeter (all downloads/unzips in /tmp to avoid overwriting /bin or /lib)
RUN mkdir -p /opt && cd /tmp && \
    wget -q https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.zip && \
    unzip -q apache-jmeter-${JMETER_VERSION}.zip && \
    mv apache-jmeter-${JMETER_VERSION} ${JMETER_HOME} && rm apache-jmeter-${JMETER_VERSION}.zip && \
    curl -sJLO https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/1.10/jmeter-plugins-manager-1.10.jar && mv jmeter-plugins-manager-1.10.jar ${JMETER_HOME}/lib/ext/jmeter-plugins-manager-1.10.jar && \
    curl -sJLO https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.3/cmdrunner-2.3.jar && mv cmdrunner-2.3.jar ${JMETER_HOME}/lib/cmdrunner-2.3.jar && \
    java -cp ${JMETER_HOME}/lib/ext/jmeter-plugins-manager-1.10.jar org.jmeterplugins.repository.PluginManagerCMDInstaller && \
    curl -sJLO https://repo1.maven.org/maven2/com/blazemeter/jmeter-plugins-random-csv-data-set/0.8/jmeter-plugins-random-csv-data-set-0.8.jar && mv jmeter-plugins-random-csv-data-set-0.8.jar ${JMETER_HOME}/lib/ext/jmeter-plugins-random-csv-data-set-0.8.jar && \
    curl -sJLO https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/2.12.0/commons-pool2-2.12.0.jar && mv commons-pool2-2.12.0.jar ${JMETER_HOME}/lib/commons-pool2-2.12.0.jar && \
    curl -sJLO https://repo1.maven.org/maven2/redis/clients/jedis/3.10.0/jedis-3.10.0.jar && mv jedis-3.10.0.jar ${JMETER_HOME}/lib/jedis-3.10.0.jar && \
    curl -sJLO https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-casutg/2.10/jmeter-plugins-casutg-2.10.jar && mv jmeter-plugins-casutg-2.10.jar ${JMETER_HOME}/lib/ext/jmeter-plugins-casutg-2.10.jar && \
    curl -sJLO https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc8/21.4.0.0.1/ojdbc8-21.4.0.0.1.jar && mv ojdbc8-21.4.0.0.1.jar ${JMETER_HOME}/lib/ext/ojdbc8-21.4.0.0.1.jar && \
    curl -sJLO https://repo1.maven.org/maven2/com/oracle/database/jdbc/ucp/23.3.0.23.09/ucp-23.3.0.23.09.jar && mv ucp-23.3.0.23.09.jar ${JMETER_HOME}/lib/ext/ucp-23.3.0.23.09.jar && \
    curl -sJLO https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.20.0/amqp-client-5.20.0.jar && mv amqp-client-5.20.0.jar ${JMETER_HOME}/lib/ext/amqp-client-5.20.0.jar && \
    curl -sJLO https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-dummy/0.4/jmeter-plugins-dummy-0.4.jar && mv jmeter-plugins-dummy-0.4.jar ${JMETER_HOME}/lib/ext/jmeter-plugins-dummy-0.4.jar && \
    curl -sJLO https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.2/ivy-2.5.2.jar && mv ivy-2.5.2.jar ${JMETER_HOME}/lib/ivy-2.5.2.jar && \
    curl -sJLO https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-tst/2.6/jmeter-plugins-tst-2.6.jar && mv jmeter-plugins-tst-2.6.jar ${JMETER_HOME}/lib/ext/jmeter-plugins-tst-2.6.jar && \
    curl -sJLO https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-cmn-jmeter/0.7/jmeter-plugins-cmn-jmeter-0.7.jar && mv jmeter-plugins-cmn-jmeter-0.7.jar ${JMETER_HOME}/lib/jmeter-plugins-cmn-jmeter-0.7.jar && \
    curl -sJLO https://jdbc.postgresql.org/download/postgresql-42.7.5.jar && mv postgresql-42.7.5.jar ${JMETER_HOME}/lib/postgresql-42.7.5.jar && \
    curl -sJLO https://jmeter-plugins.org/files/packages/awsmeter-3.0.0.zip && unzip -q awsmeter-3.0.0.zip && mv lib/ext/awsmeter-3.0.0.jar ${JMETER_HOME}/lib/ext/awsmeter-3.0.0.jar && \
    rm -rf awsmeter-3.0.0.zip lib 

ENV PATH="${JMETER_HOME}/bin:${PATH}"
# Create non-root user
RUN useradd -m -s /bin/bash jmeter
USER jmeter

WORKDIR /project

