# syntax=docker/dockerfile:experimental
FROM openjdk:11

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
    && curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add \
    && apt-get update \
    && apt-get install -y libxtst6 libx11-6 libxrender1 xvfb openssh-server python3 \
        python3-pip sbt libssl-dev pkg-config x11-apps imagemagick

WORKDIR /tmp/prefetch

RUN --mount=type=bind,rw,source=.,target=. set -x && sbt test:compile

WORKDIR /root
RUN rmdir /tmp/prefetch
