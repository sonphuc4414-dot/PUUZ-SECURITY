#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILE="$ROOT/src/main/java/com/puuz/mapshield/PuuzMapShieldClient.java"
if grep -Fq 'long window = client.getWindow().getHandle();' "$FILE"; then
  echo "ERROR: old Quick Pay window-handle code remains" >&2; exit 1;
fi
grep -Fq 'net.minecraft.client.util.Window window = client.getWindow();' "$FILE"
grep -Fq 'InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)' "$FILE"
grep -Fq 'InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT)' "$FILE"
python3 - "$FILE" <<'PY2'
from pathlib import Path
import sys
s=Path(sys.argv[1]).read_text()
assert s.count('{') == s.count('}')
assert s.count('(') == s.count(')')
print('Quick Pay source checks: PASS')
PY2
