#!/bin/bash
set -euo pipefail
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd "$project_root"
if [[ ! -f build/classpath.txt ]]; then echo 'Run ./build.sh first.' >&2; exit 1; fi
jdk_bin=${NEBULA_JDK_BIN:-${JAVA_HOME:+$JAVA_HOME/bin}}
if [[ -n "$jdk_bin" ]]; then javac_cmd="$jdk_bin/javac"; java_cmd="$jdk_bin/java"; else javac_cmd=javac; java_cmd=java; fi
cp_value=$(cat build/classpath.txt)
"$javac_cmd" -cp "build/classes:$cp_value" -d build/classes patcher/src/StatsPatchTest.java
"$java_cmd" -Duser.timezone=UTC -cp "build/classes:$cp_value" StatsPatchTest
