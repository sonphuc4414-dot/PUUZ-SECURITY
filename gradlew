#!/bin/sh

# PUUZ Map Shield bootstrap wrapper.
# The repository is distributed with the Gradle wrapper bootstrap logic but not
# a pre-bundled wrapper JAR. The JAR is fetched only when building the project.
# The Minecraft mod itself never performs network access.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v9.2.1/gradle/wrapper/gradle-wrapper.jar"
EXPECTED_SHA256="423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9"

mkdir -p "$(dirname "$WRAPPER_JAR")"

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    return 127
  fi
}

if [ ! -s "$WRAPPER_JAR" ] || [ "$(sha256_of "$WRAPPER_JAR" 2>/dev/null || true)" != "$EXPECTED_SHA256" ]; then
  rm -f "$WRAPPER_JAR"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL --retry 3 -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "ERROR: curl or wget is required once to bootstrap the Gradle wrapper." >&2
    exit 1
  fi

  ACTUAL_SHA256="$(sha256_of "$WRAPPER_JAR" 2>/dev/null || true)"
  if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
    rm -f "$WRAPPER_JAR"
    echo "ERROR: Gradle wrapper JAR checksum mismatch." >&2
    exit 1
  fi
fi

JAVA_CMD="${JAVA_HOME:-}/bin/java"
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
