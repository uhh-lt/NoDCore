FROM openjdk:8-alpine

RUN apk update && apk upgrade
RUN apk add --no-cache bash curl

ENV SCALA_VERSION 2.12.2
ENV SBT_VERSION 0.13.15

RUN curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz \
    | gunzip \
    | tar -x -C /usr/local

RUN curl -fsL http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz \
    | gunzip \
    | tar -x -C /usr/local

ENV PATH="/usr/local/sbt/bin:${PATH}"
COPY . /app
WORKDIR /app
RUN sbt clean compile assembly
RUN mv target/scala-2.11/NoDCore-assembly-1.0.jar /app/NoDCore-assembly-1.0.jar && rm -rf /app/target

RUN touch crontab.tmp \
    && echo '0 3 * * * /app/docker-run.sh' > crontab.tmp \
    && crontab crontab.tmp \
    && rm -rf crontab.tmp

CMD crond -l 5 -f