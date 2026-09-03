#!/bin/sh
set -e
MAVEN_VERSION=3.9.9
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ZIP="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.zip"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  echo "Maven não encontrado. Baixando Maven $MAVEN_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "Instale Maven 3.9+ ou curl/wget para o primeiro uso." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP" -d "$BASE_DIR/.mvn"
  else
    echo "O comando unzip é necessário para preparar o Maven." >&2
    exit 1
  fi
  rm -f "$ZIP"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
