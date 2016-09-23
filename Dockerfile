FROM registry.opensource.zalan.do/stups/openjdk:8-42

MAINTAINER Zalando SE

RUN mkdir /appdynamics
COPY appdynamics /appdynamics

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 70) $(newrelic-agent) $(appdynamics-agent) -jar /kio.jar

COPY target/kio.jar /
COPY target/scm-source.json /scm-source.json
