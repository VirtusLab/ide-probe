FROM gitpod/workspace-java-11:latest

USER root
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
    && curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add \
    && curl -fsSL https://pkgs.tailscale.com/stable/ubuntu/focal.gpg | sudo apt-key add - \
    && curl -fsSL https://pkgs.tailscale.com/stable/ubuntu/focal.list | sudo tee /etc/apt/sources.list.d/tailscale.list \
    && curl -s --compressed "https://virtuslab.github.io/scala-cli-packages/KEY.gpg" | sudo apt-key add - \
    && sudo curl -s --compressed -o /etc/apt/sources.list.d/scala_cli_packages.list "https://virtuslab.github.io/scala-cli-packages/debian/scala_cli_packages.list" \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y tailscale sbt \
       libxtst6 libx11-6 libxrender1 xvfb openssh-server python3 \
       python3-pip libssl-dev pkg-config x11-apps imagemagick xorg scala-cli \
    && echo "nohup /usr/bin/Xvfb :0.0 -screen 0 1024x768x24 &" > /etc/init.d/xvfb \
    && sudo update-rc.d xvfb defaults