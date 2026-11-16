#!/bin/bash
set -euo pipefail
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd "$project_root"
mkdir -p build/classes build/artifacts
jdk_bin=${NEBULA_JDK_BIN:-${JAVA_HOME:+$JAVA_HOME/bin}}
if [[ -n "$jdk_bin" ]]; then javac_cmd="$jdk_bin/javac"; java_cmd="$jdk_bin/java"; else javac_cmd=javac; java_cmd=java; fi
base_jar=${NEBULA_BASE_JAR:-$project_root/.cache/Nebula-Windows-original.jar}
if [[ -z "${NEBULA_BASE_JAR:-}" && -f "$project_root/artifacts/Nebula-Windows-original.jar" ]]; then base_jar="$project_root/artifacts/Nebula-Windows-original.jar"; fi
if [[ ! -f "$base_jar" ]]; then
 mkdir -p "$(dirname -- "$base_jar")"
 curl -fSL --retry 2 --max-time 300 https://media.githubusercontent.com/media/WatchSteinsGate/freebubu/handoff-2026-11-16/artifacts/Nebula-Windows-original.jar -o "$base_jar"
fi
python3 - "$base_jar" <<'PY'
import hashlib,json,pathlib,sys
expected=next(x['sha256'] for x in json.loads(pathlib.Path('metadata/builds.json').read_text())['artifacts'] if x['filename']=='Nebula-Windows-original.jar')
with open(sys.argv[1],'rb') as f:actual=hashlib.file_digest(f,'sha256').hexdigest()
if actual!=expected:raise SystemExit('Original input JAR checksum mismatch; use the release asset specified in metadata/builds.json')
PY
cp_value=$(python3 tools/resolve_classpath.py)
cp_value="$cp_value:$base_jar"
printf '%s' "$cp_value" > build/classpath.txt
"$javac_cmd" -cp "$cp_value" -d build/classes patcher/src/StatsPatch.java patcher/src/CosmeticsPatch.java patcher/src/BuildDirect.java
"$java_cmd" -cp "build/classes:$cp_value" BuildDirect "$base_jar" "$project_root" "$project_root/build/artifacts/Nebula-Windows-direct.jar"

# Relocate resources and JNI bridge package after the original-class patches.
mkdir -p build/organize
"$javac_cmd" -cp "$cp_value:build/artifacts/Nebula-Windows-direct.jar" -d build/organize patcher/organize/*.java
python3 - <<'PYEXTRACT'
import pathlib,zipfile
with zipfile.ZipFile('build/artifacts/Nebula-Windows-direct.jar') as z:
 pathlib.Path('build/organize/Skiko-original.class').write_bytes(z.read('org/jetbrains/skiko/LibraryLoader.class'))
 pathlib.Path('build/organize/CertificatePinner-original.class').write_bytes(z.read('okhttp3/CertificatePinner.class'))
PYEXTRACT
"$java_cmd" -cp "build/organize:$cp_value" SkikoResources build/organize/Skiko-original.class build/organize/LibraryLoader.class
"$java_cmd" -cp "build/organize:$cp_value" DevPatches build/organize/CertificatePinner-original.class build/organize/CertificatePinner.class
python3 tools/organize-jar.py build/artifacts/Nebula-Windows-direct.jar build/artifacts/Nebula-Windows-organized.jar build/organize > build/organization.json
mv build/artifacts/Nebula-Windows-organized.jar build/artifacts/Nebula-Windows-direct.jar

# Consolidate the inherited compatibility members into existing mod classes.
linux_jar=${NEBULA_LINUX_REFERENCE:-$project_root/artifacts/Nebula-Linux-reference.jar}
skiko_dll=${NEBULA_SKIKO_DLL:-$project_root/.cache/native-inputs/skiko-windows-x64.dll}
if [[ -z "${NEBULA_SKIKO_DLL:-}" && ! -f "$skiko_dll" && -f "$project_root/build/vulkan-native/skiko-windows-x64.dll" ]]; then skiko_dll="$project_root/build/vulkan-native/skiko-windows-x64.dll"; fi
[[ -f "$linux_jar" ]] || { echo "Set NEBULA_LINUX_REFERENCE to the matching supplied Linux JAR" >&2; exit 1; }
[[ -f "$skiko_dll" ]] || python3 tools/build-vulkan-native.py
mkdir -p build/consolidate
"$javac_cmd" -cp "$cp_value:build/artifacts/Nebula-Windows-direct.jar" -d build/consolidate patcher/consolidate/*.java
"$java_cmd" -cp "build/consolidate:$cp_value" Consolidate build/artifacts/Nebula-Windows-direct.jar build/artifacts/Nebula-Windows-consolidated.jar build/consolidate "$linux_jar" > build/consolidation.log
python3 tools/finish-native-bundle.py build/artifacts/Nebula-Windows-consolidated.jar build/artifacts/Nebula-Windows-final.jar "$skiko_dll" > build/final-native-bundle.json
mv build/artifacts/Nebula-Windows-final.jar build/artifacts/Nebula-Windows-direct.jar

# Apply the resource layout while preserving native-facing Java class identities.
"$java_cmd" -cp "build/consolidate:$cp_value" Layout build/artifacts/Nebula-Windows-direct.jar build/artifacts/Nebula-Windows-layout-java.jar
python3 tools/layout-native.py build/artifacts/Nebula-Windows-layout-java.jar build/artifacts/Nebula-Windows-layout.jar > build/layout-native.json
mv build/artifacts/Nebula-Windows-layout.jar build/artifacts/Nebula-Windows-direct.jar
rm build/artifacts/Nebula-Windows-layout-java.jar
