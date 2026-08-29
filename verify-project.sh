#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
python3 - "$ROOT" <<'PY'
import json, sys
from pathlib import Path
root=Path(sys.argv[1])
for rel in [
'src/main/resources/fabric.mod.json',
'src/main/resources/puuz_map_shield.mixins.json',
'src/main/resources/assets/puuz_map_shield/lang/en_us.json',
'src/main/resources/assets/puuz_map_shield/lang/vi_vn.json']:
    json.loads((root/rel).read_text())

mixin=(root/'src/main/java/com/puuz/mapshield/mixin/MapRendererMixin.java').read_text()
assert 'method = "draw"' in mixin
assert 'method = "update"' in mixin
assert 'MapRenderStateAccess' in mixin
assert 'com.llamalad7.mixinextras' not in mixin

client=(root/'src/main/java/com/puuz/mapshield/PuuzMapShieldClient.java').read_text()
assert 'ItemFrameEntity' in client
assert 'getMapId' in client
assert 'currentServerKey' in client

updater=(root/'src/main/java/com/puuz/mapshield/update/UpdateChecker.java').read_text()
assert 'api.github.com/repos/sonphuc4414-dot/PUUZ-SECURITY/releases/latest' in updater
assert 'CompletableFuture' not in updater  # single daemon executor keeps the implementation explicit
assert 'CHECK_INTERVAL_MS' in updater
assert 'CHECK_RUNNING' in updater
assert 'setDaemon(true)' in updater

print('PUUZ Map Shield source verification: OK')
PY
