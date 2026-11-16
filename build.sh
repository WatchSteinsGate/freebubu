#!/bin/bash
set -euo pipefail
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd "$project_root"
mkdir -p build/classes build/artifacts
jdk_bin=${NEBULA_JDK_BIN:-${JAVA_HOME:+$JAVA_HOME/bin}}
if [[ -n "$jdk_bin" ]]; then javac_cmd="$jdk_bin/javac"; java_cmd="$jdk_bin/java"; else javac_cmd=javac; java_cmd=java; fi
base_jar=${NEBULA_BASE_JAR:-$project_root/.cache/Nebula-Windows-original.jar}
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
