# Managed environment and runtime

Each cloud task is already isolated. Reuse its existing checkout; do not create
another Git worktree unless requested. GitHub publication and cloud-environment
publication are separate operations.

## Prepared machine

- Native Linux JDK 25: `/workspace/tools/linux-java/jdk-25.0.5+8`.
- Windows JDK 25: `/workspace/tools/windows-java/jdk-25.0.5+8`.
- Wine 10 and Debian dependencies: `/workspace/tools/runtime`.
- Wine activation: `source /workspace/tools/wine-env.sh`.
- Test instance: `/workspace/minecraft`.
- Minecraft 26.2, Fabric Loader 0.20.1, Fabric API 0.161.0+26.2,
  Fabric Language Kotlin 1.14.2+kotlin.2.4.21.
- Xvfb display: `:99`, 1280×800.
- Editor: loopback port 6969; no public preview URL is provided.

Wine was extracted into a user-writable prefix because this machine lacked the
usual system-package installation helper. Debian APT signature/package checks were
kept enabled. The relocations required a workspace wineserver wrapper, the Wine
NLS data path, and the native zlib DLL in the Wine prefix's system32 directory.
Both JDKs were SHA256-verified using their official Adoptium package metadata.

Local scripts retained on that machine:

```text
/workspace/tools/install-wine.sh
/workspace/tools/wine-env.sh
/workspace/minecraft/prepare.py
/workspace/minecraft/download_assets.py
/workspace/minecraft/launch.py
/workspace/minecraft/start.sh
```

These scripts are included under `tools/workspace-runtime/` for reference. They
use the original `/workspace` layout and are not a portable system installer.
They require their retained metadata/caches and the direct build to exist.

## Starting and changing the mod

Run `/workspace/minecraft/start.sh` in a managed background terminal session with
output redirected to a log. It starts Xvfb if needed. Wait for the client main menu
and for the editor to respond; a PID alone is not readiness evidence.

Stop only the client process you started before replacing its JAR. Keep exactly
one Nebula variant in `mods`. Copy the final direct JAR there and restart. Wine,
JDKs, dependencies and saved files persist in the filesystem; live processes do
not necessarily survive publication or a new task.

`launch.py` accepts an optional `NEBULA_TEST_AGENT` path for the separate read-only
probe agent. Normal startup does not set this variable and needs no test agent.
Do not infer that this proves dynamic attach or arbitrary agents are supported.

## Validation

On the existing machine, a repository build can reuse the installed dependency
classpath without downloading it again:

```sh
NEBULA_JDK_BIN=/workspace/tools/linux-java/jdk-25.0.5+8/bin \
NEBULA_BASE_JAR=/workspace/downloads/Nebula-Windows.jar \
NEBULA_CLASSPATH="$(cat /workspace/nebula-work/direct/classpath.txt)" \
./build.sh

NEBULA_JDK_BIN=/workspace/tools/linux-java/jdk-25.0.5+8/bin ./test-stats.sh
```

Install the pinned Python packages in a project virtual environment and ensure
Chromium is installed, then run `tests/test_mirror_editor.py`, `tests/test_mirror_highres.py` and `tests/test_cape_preview.py`.
Their `--live` modes use the actual editor and modify/reset its saved cosmetics.
Only use them against a dedicated test profile. Test screenshots/exports go to
`build/test-output`, which Git ignores.

The test identity was unauthenticated. Login, authenticated multiplayer and production macro activity remain separate checks. Java interception and native TLS/HTTP 101 were subsequently observed, but complete authenticated native sessions and native debugging remain unverified. BUILD.md supersedes the older minimal rebuild example above.
Never put platform credentials, private keys, or storage.to owner tokens into Git.

Build the external read-only probe using `tools/build-probe.sh` with JDK 25. Its
manifest is tracked. To use it in the retained workspace, set
`NEBULA_TEST_AGENT=/workspace/freebubu/build/probe.jar` only for the test launch.
The probe reports pass/fail in the client log and does not modify classes.

## Current native/software Vulkan runtime

The cloud Vulkan smoke test adds `/workspace/tools/vulkan-runtime/usr/lib/x86_64-linux-gnu` to `LD_LIBRARY_PATH` and selects `/workspace/tools/vulkan-runtime/usr/share/vulkan/icd.d/lvp_icd.json` through `VK_ICD_FILENAMES`. These are test-machine Mesa inputs, not new files shipped inside the Windows JAR. Windows users use their installed Vulkan driver.

The investigation launchers archived under `tools/workspace-runtime/` accept `NEBULA_DEV_JVM_ARGS` and an optional test agent, and the Vulkan variant passes Minecraft's game argument `--graphicsBackend vulkan`. JVM proxy configuration and native routing/trust setup are separate; see `tests/network/README.md`. Temporary GUI keybind/proxy changes used for testing were restored. The supported JAR was copied back into the test mods directory after the broken-build comparison.
