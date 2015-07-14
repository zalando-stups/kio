FROM zalando/openjdk:8u45-b14-4

MAINTAINER Zalando SE

COPY target/kio.jar /

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $(java-dynamic-memory-opts) $(newrelic-agent) $(appdynamics-agent) -jar /kio.jar

ADD /scm-source.json /scm-source.json
