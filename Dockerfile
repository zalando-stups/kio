FROM registry.opensource.zalan.do/stups/openjdk:latest

MAINTAINER Zalando SE

RUN mkdir /appdynamics
COPY appdynamics /appdynamics
COPY resources/api/kio-api.yaml /zalando-apis/

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 70) $(newrelic-agent) $(appdynamics-agent) -jar /kio.jar

COPY target/kio.jar /
