#!/usr/bin/env sh
set -eu

MAVEN_VERSION="3.9.9"
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
ARCHIVE="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$WRAPPER_DIR"
  if [ ! -f "$ARCHIVE" ]; then
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" -o "$ARCHIVE"
  fi
  tar -xzf "$ARCHIVE" -C "$WRAPPER_DIR"
fi

exec "$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$BASE_DIR/.mvn/repository" "$@"
