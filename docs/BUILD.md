# Build and reproduction

The published JAR is identified by `metadata/builds.json` and `SHA256SUMS`. The final supported SHA256 is `0f34574ebbe06259cabb04e3cf8918d7cedc7bc1a6aa8bc9d10b2523d41d2f93`. A rebuild can have different ZIP timestamps; compare per-entry bytes as well as whole-file hashes.

## Packaging rebuild using the published native DLL

This is the supported way to reproduce/change Java patches and the local editor without rebuilding Skia. It verifies the three LFS artifacts, extracts the already-built Skiko DLL from the current published JAR, and rebuilds the Windows package from the exact original Windows input. This reuses the native binary; it is not a full native source bootstrap.

Requirements: Bash, Python3.11+, curl, Git LFS, JDK 25, and the pinned Python native-build dependencies. Set `NEBULA_JDK_BIN` or `JAVA_HOME` to the Linux build JDK. Runtime testing uses a separate Windows JDK.

```sh
git clone https://github.com/WatchSteinsGate/freebubu.git
cd freebubu
git lfs pull
python3 -m venv .venv
. .venv/bin/activate
python -m pip install -r tests/native-build-requirements.txt
python tools/prepare-build-inputs.py
export NEBULA_JDK_BIN=/path/to/jdk-25/bin
./build.sh
```

`prepare-build-inputs.py` rejects LFS pointers, wrong sizes and wrong SHA256s. It extracts `skiko-windows-x64.dll` into `.cache/native-inputs/` and verifies its packaged hash. `build.sh` defaults to the checked-out Windows/Linux inputs and this extracted DLL. Build output is `build/artifacts/Nebula-Windows-direct.jar`; diagnostics/manifests remain under ignored `build/`.

Explicit inputs may be supplied:

```sh
export NEBULA_BASE_JAR="$PWD/artifacts/Nebula-Windows-original.jar"
export NEBULA_LINUX_REFERENCE="$PWD/artifacts/Nebula-Linux-reference.jar"
export NEBULA_SKIKO_DLL="$PWD/.cache/native-inputs/skiko-windows-x64.dll"
./build.sh
```

The Linux reference contributes four matching Skia Java facades during consolidation; its Linux native libraries are not bundled into the Windows output. The original input checksum is mandatory. An existing dependency classpath can be passed with `NEBULA_CLASSPATH`; otherwise `tools/resolve_classpath.py` downloads and verifies `metadata/dependencies.lock.json` inputs.

## Build stages

1. Compile `StatsPatch`, `CosmeticsPatch` and `BuildDirect` with JDK 25. Merge stats/cosmetics into five existing classes, retain the original mixins, remove inherited add-on entries and embed the committed editor HTML.
2. Compile organization templates/tools. Redirect Skiko resources, disable bundled OkHttp pin enforcement, flatten the original loader archive, consolidate resources/version metadata and apply the hash-guarded native debugging plan. JNI bridge exports are updated for the consolidated class owners.
3. Compile consolidation templates/tools. Move compatible helper fields/methods into existing classes; replace framebuffer adapters with the matching Skia facades and supplied rebuilt Skiko DLL; remove20 helper class files. Recompute existing native integrity metadata.
4. Relocate native/data resources and the access widener. Keep `zip.nebula.loader.Loader` and `Key` as separate original identities. Remove the two unused captured TypeSwitch files. Verify native hashes without injecting layout lookup redirects.

The patcher/template classes, diagnostics, test agents and intermediate outputs are not shipped runtime helper classes. Do not remove the recorded bootstrap table or introduce guessed numeric results. Changing the native debug payload requires updating all compiled integrity expectations, including the Loader template; do not bypass the checks to force a build.

## Editor regeneration

The generated `editor/editor.html` is committed, so a packaging rebuild does not require Node. If changing the frontend:

```sh
npm ci --prefix editor/build-tools
node tools/build-editor.mjs
./build.sh
```

The lockfile records the UI build dependencies. The builder uses guarded transforms of the supplied minified mirror modules; inspect failing transform guards rather than silently skipping a transformation when inputs change. Preserve the `data:`/`blob:` `connect-src` permission required by the viewer and the loopback server's token/Host/Origin protections.

## Full native DLL rebuild

The complete additional JNI source is `patcher/native/Vulkan.cc`. `tools/build-vulkan-native.py` compiles the matching public Skiko JNI sources and links matching prebuilt Windows Skia static libraries into the **same existing Skiko DLL filename**. It preserves every original export via a generated DEF file. This is a Skiko DLL rebuild using published Skia archives; it does not compile all of Skia from its own source.

Pinned public source/archive URLs and SHA256s are in `metadata/native-build-inputs.json`. Fetch/extract Skiko and Windows Skia with:

```sh
python tools/fetch-native-sources.py
```

The known toolchain is Clang/lld19.1.7, xwin0.10.0, MSVC CRT14.44 and Windows SDK10.0.26100, Windows x64 target, release static CRT. Windows JDK 25 supplies JNI/JVMTI headers. On the retained cloud machine, the compiler tools and Windows SDK were downloaded/extracted into user-writable directories rather than installed globally.

Acquire the SDK/CRT separately with xwin under its applicable terms; they are not redistributed here. After downloading and SHA256-verifying the xwin archive from the manifest:

```sh
/path/to/xwin --accept-license --cache-dir "$PWD/.cache/xwin" splat --output /path/to/windows-sdk
```

Compiler/runtime roots are configurable; the directory containing executables must provide `clang++-19` and `lld-link-19`:

```sh
export NEBULA_SKIA_ROOT="$PWD/.cache/vulkan-crash/skia-windows"
export NEBULA_SKIKO_SOURCE="$PWD/.cache/vulkan-crash/source/skiko-0.153.0/skiko"
export NEBULA_WINDOWS_SDK=/path/to/windows-sdk
export NEBULA_NATIVE_TOOLS=/path/to/llvm/bin
export NEBULA_NATIVE_TOOL_LIBS=/path/to/llvm/runtime-libraries
export NEBULA_WINDOWS_JDK=/path/to/windows-jdk-25
export NEBULA_BASE_JAR="$PWD/artifacts/Nebula-Windows-original.jar"
python tools/build-vulkan-native.py
export NEBULA_SKIKO_DLL="$PWD/build/vulkan-native/skiko-windows-x64.dll"
./build.sh
```

The script creates a case-insensitive LLVM header overlay for the Windows SDK, includes the WinRT headers and Vulkan headers, uses the matching static CRT, resolves Windows library casing, compiles `.cc` and `.cpp` units and preserves original exports. Build logs, objects, import libraries and the rebuilt DLL remain in `build/vulkan-native/`. `NEBULA_PEFILE_PATH` can select an existing Python tool directory; otherwise install the pinned requirements in the active Python environment. The script retains the original cloud paths as defaults for that machine; use the overrides elsewhere.

Do not claim byte-reproducible native output across different compiler/SDK versions. Compare exports and run actual JNI rendering tests. Keep the seven added JNI methods together: leaving out borrowed-image interop can allow startup yet fail later texture operations.

## Validation commands and boundaries

```sh
./test-stats.sh
python -m pip install -r tests/requirements.txt
python tests/test_mirror_editor.py
python tests/test_mirror_highres.py
python tests/test_cape_preview.py
```

The browser tests currently expect Chromium at `/usr/bin/chromium`; adjust its path on another system. Only the mirror/editor tests represent the current UI. Older `test_cape.py`/`test_editor.py` variants are historical. `--live` tests reset/write cosmetics and belong on a dedicated instance.

Java agent/native/proxy checks require a prepared Minecraft/Wine or Windows instance, matching classpath and explicit test inputs; they are not all standalone CI tests. `tests/diagnostics/` contains the JNI class-lookup and JDI probes used in diagnosis, not production agents. They must not log actual key/token values. `tests/SkikoInteropProbe.java` verifies real GL pixels and borrowed images. The native debugging probe exercises only the guarded version's known RVAs.

For interception, follow `tests/network/README.md`: preserve inherited CA/proxy settings, add the actual inspection CA to copies of trust stores, route Java explicitly and route native sockets separately. A TLS handshake/HTTP 101 does not establish authenticated protocol success. Do not treat a dummy-account 403 as a reason to fabricate tokens or remove server authentication.

## Retained cloud environment

Reference scripts under `tools/workspace-runtime/` and metadata under `metadata/` describe the prepared machine. They use `/workspace` paths and require the relevant caches/installed packages; they are not a universal machine installer. The primary test profile was unauthenticated. Before replacing a JAR, stop the test process you own, keep only one Nebula variant in `mods`, copy the desired artifact and restart. A live PID is not proof that the menu/API/cape flow works.
