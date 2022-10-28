FROM openjdk:11.0.16-jre

MAINTAINER tangxianglin<txline0420@163.com>

ADD target/general-netsdk-springboot-0.0.1-SNAPSHOT.jar /data/general-netsdk-springboot-0.0.1-SNAPSHOT.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/shanghai" > /etc/timezone;

EXPOSE 8080

ENTRYPOINT ["java","-jar", "/data/general-netsdk-springboot-0.0.1-SNAPSHOT.jar","-XX:G1HeapRegionSize=16MB","-XX:-UseContainerSupport","-server","-XX:+UseStringDeduplication","-XX:+UseG1GC","-XX:+DisableExplicitGC", "-XX:-HeapDumpOnOutOfMemoryError","-XX:+AggressiveOpts"]