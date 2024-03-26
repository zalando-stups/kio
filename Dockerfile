FROM container-registry.zalando.net/library/eclipse-temurin-8-jdk:latest

MAINTAINER Zalando SE

COPY resources/api/kio-api.yaml /zalando-apis/
COPY target/kio.jar /
COPY java-dynamic-memory-opts /usr/local/bin/java-dynamic-memory-opts
RUN chmod +x /usr/local/bin/java-dynamic-memory-opts

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 70) -jar /kio.jar
