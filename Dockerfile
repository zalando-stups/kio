FROM zalando/openjdk:8u40-b09-2

MAINTAINER Zalando SE

COPY target/kio.jar /

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $(java-dynamic-memory-opts) -jar /kio.jar
