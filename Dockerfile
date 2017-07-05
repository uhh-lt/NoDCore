FROM openjdk:8

RUN apt-get update && apt-get upgrade -y

ENV SCALA_VERSION 2.12.2
ENV SBT_VERSION 0.13.15

RUN \
  curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion


COPY . /app
WORKDIR /app

RUN sbt clean compile assembly

CMD java -jar /app/target/scala-2.11/NoDCore-assembly-1.0.jar --dir /app/content --format compressed