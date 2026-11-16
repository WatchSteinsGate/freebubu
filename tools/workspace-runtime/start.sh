#!/bin/bash
set -euo pipefail
. /workspace/tools/wine-env.sh
export DISPLAY=:99
if ! xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
 /workspace/tools/runtime/usr/bin/Xvfb "$DISPLAY" -screen 0 1280x800x24 -nolisten tcp > /workspace/minecraft/xvfb.log 2>&1 &
fi
cd /workspace/minecraft
exec python /workspace/minecraft/launch.py
