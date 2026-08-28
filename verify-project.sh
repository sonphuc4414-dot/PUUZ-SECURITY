#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

grep -q "net.fabricmc.fabric-loom-remap" "$ROOT/build.gradle"
grep -q 'minecraft_version=1.21.11' "$ROOT/gradle.properties"
grep -q 'public final class PuuzMapShieldClient' "$ROOT/src/main/java/com/puuz/mapshield/PuuzMapShieldClient.java"
grep -q '"environment": "client"' "$ROOT/src/main/resources/fabric.mod.json"
gngrep_dummy=1
[ -f "$ROOT/src/main/resources/assets/puuz_map_shield/textures/misc/map_hidden.png" ]
[ -f "$ROOT/gradle/wrapper/gradle-wrapper.properties" ]
[ -x "$ROOT/gradlew" ]
echo "PUUZ Map Shield source validation: OK"
