#FROM ubuntu:bionic
FROM cimg/base:stable

# Install dependencies
RUN sudo apt-get update && sudo apt-get install -y \ 
    build-essential \
    openjdk-8-jre \ 
    openjdk-8-jdk \
    python \
    python3 \
    wget

#Install Depot tools
ARG depot_tools_commit
RUN git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git /home/circleci/depot_tools
RUN cd /home/circleci/depot_tools && git checkout ${depot_tools_commit}
ENV PATH /home/circleci/depot_tools:$PATH
ENV GCLIENT_PY3 0

#Install Node.js 12.x
RUN curl -sL https://deb.nodesource.com/setup_12.x | sudo -E bash -
RUN sudo apt-get install -y nodejs 

#Install appcenter
RUN sudo npm install -g appcenter-cli

#Copy needed files
COPY --chown=circleci:circleci ./src .
COPY --chown=circleci:circleci ./.cipd /home/circleci/.cipd
