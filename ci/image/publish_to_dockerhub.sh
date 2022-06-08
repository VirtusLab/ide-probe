#!/usr/bin/env bash

# script to setup new image (best to do on each intellij version bump)

NAME=ide-probe-tests
read -p "Username: " USERNAME
IJ_VERSION=221.5591.52
IMAGE="${USERNAME}/${NAME}:${IJ_VERSION}"

rm -f Dockerfile || true && cp ci/image/Dockerfile Dockerfile || exit 1

#docker login --username="$USERNAME"
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build --tag "$IMAGE" .
#docker push "$IMAGE"
