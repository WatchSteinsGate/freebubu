#!/bin/bash
set -euo pipefail
export APT_CONFIG=/workspace/tools/apt/apt.conf
/usr/bin/apt-get update
/usr/bin/apt-get -o Debug::NoLocking=true --download-only -y install wine wine64 xvfb
for deb in /workspace/tools/apt/archives/*.deb; do dpkg-deb -x "$deb" /workspace/tools/runtime; done
cat > /workspace/tools/runtime/usr/lib/wine/wineserver <<'EOF'
#!/bin/sh
exec /workspace/tools/runtime/usr/lib/wine/wineserver64 -p0 "$@"
EOF
mkdir -p /workspace/tools/runtime/usr/share/wine/wine
ln -sfn ../nls /workspace/tools/runtime/usr/share/wine/wine/nls
. /workspace/tools/wine-env.sh
# First boot can exit with missing zlib before system32 is initialized.
wine64 wineboot.exe -u || true
cp /workspace/tools/runtime/usr/x86_64-w64-mingw32/lib/zlib1.dll /workspace/tools/wine-prefix/drive_c/windows/system32/zlib1.dll
wine64 /workspace/tools/windows-java/jdk-25.0.5+8/bin/java.exe -version
