FROM mcr.microsoft.com/java/jdk:11-zulu-alpine as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM mcr.microsoft.com/java/jdk:11-zulu-alpine
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./

## iproute2, iptables and tc in container so that pumba can manage network.
RUN apk --no-cache add iproute2 \
  iptables
RUN ln -s /usr/lib/tc /lib/tc

## app: run the app
ENTRYPOINT ["java", \
 "org.springframework.boot.loader.JarLauncher", \
 "--scenario=ReceiveAsync"]