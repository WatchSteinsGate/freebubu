#!/bin/bash
set -euo pipefail
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_root"
jdk_bin=${NEBULA_JDK_BIN:-${JAVA_HOME:+$JAVA_HOME/bin}}
if [[ -n "$jdk_bin" ]]; then javac_cmd="$jdk_bin/javac"; jar_cmd="$jdk_bin/jar"; else javac_cmd=javac; jar_cmd=jar; fi
mkdir -p build/probe-classes
"$javac_cmd" -d build/probe-classes patcher/src/ProbeAgent.java
"$jar_cmd" cfm build/probe.jar tools/probe-manifest.mf -C build/probe-classes ProbeAgent.class
printf '%s\n' 'Created build/probe.jar; this is external test tooling, not a mod entry.'
