# Current build handoff — 16 November 2026

This document supersedes earlier artifact/layout descriptions. The supported artifact is the **authentication compatibility build**, not the failed Loader/Key layout build or the rejected OpenGL workaround.

## Artifact identity and inputs

| File | Role | SHA256 |
|---|---|---|
| `Nebula-Windows-direct.jar` | Current Windows deliverable; 192425795 bytes | `0f34574ebbe06259cabb04e3cf8918d7cedc7bc1a6aa8bc9d10b2523d41d2f93` |
| `Nebula-Windows-original.jar` | Exact supplied Windows patch input | `6bcb4aa0c1dda8a756a59014349305dcd14ce3212fd779cba26c64e038995621` |
| `Nebula-Linux-reference.jar` | Supplied experimental Linux reference used to match Skia/Vulkan bindings | `350c4e156b3036e596e26766e97f1064d1f175c1121675c05a6531c315c1e7f6` |

The artifacts are under `artifacts/` in Git LFS. See [the current release](https://github.com/WatchSteinsGate/freebubu/releases/tag/handoff-2026-11-16-auth-compatibility), [build manifest](../metadata/builds.json) and [checksums](../SHA256SUMS). The temporary storage.to mirror is `https://storage.to/jZYkQzjYt`, expiring 23 November 2026 at 19:35:47 Nepal time (13:50:47 UTC). The GitHub tag/LFS reference is the persistent handoff.

Target: Minecraft26.2, Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, Fabric Language Kotlin1.14.2+kotlin2.4.21, Windows x86-64 and Java 25. Wine 10/Windows JDK 25.0.5 supplied the cloud test environment. Do not install multiple Nebula variants in one mods directory.

## Provenance and requested scope

The user supplied an already modified Windows JAR, saved cosmetics pages/modules, screenshots, a website mirror ZIP, a crash report and an experimental Linux JAR. Existing compatibility, statistics, cosmetics and recorded-bootstrap additions were already in the input. “Original” means the supplied input, not a verified pristine vendor build.

The final work keeps statistics and personal cosmetics, removes unwanted login/marketing UI from the local editor, reuses the supplied mirror components, repairs cape preview/rendering, reduces animated-cape stutter, supplies real Windows Vulkan bindings, patches identified debugging/pinning paths, and consolidates compatible helper members directly into existing classes. The user requested no additional runtime helper class/mixin/DLL layer. Build-time templates, patchers, diagnostic agents and test libraries are separate and are not packaged into the mod.

## Final archive layout

```text
fabric.mod.json
nebula.mixins.json
nebulaclient.json
META-INF/
  MANIFEST.MF
  jars/hm-api-1.0.4+26.2.jar
  licenses/...
  world-offset.accesswidener
thatonecatgif/
  native/windows-x64/
    client.dll
    core.dll
    bridge.dll
    skiko-windows-x64.dll
    skiko-windows-x64.dll.sha256
    icudtl.dat
    hyperlane-windows-x86_64.dll
    cinewire-windows-x86_64.zip
    checksums.properties
  data/
    bootstrap.bin
    foraging-trees.dat
    metal-pipe.wav
    scream.wav
    kuudra-lego.wav
zip/nebula/
  loader/Loader.class
  loader/Key.class
  client/...
  ... original DTOs, roles, statistics and client packages
assets/nebula/
  images/...
  shaders/...
  fonts/...
  dungeons/...
  miningqol/...
  editor/index.html
lib/
  ... retained Windows junixsocket resources
... original third-party Java packages
```

**Loader and Key deliberately stay at their original names.** The proposed `thatonecatgif.loader` namespace and Key merge were reverted after the menu authentication regression. There is no compatibility shim. Two unused captured `Loader$$TypeSwitch+...` files are removed. Existing native filenames remain; the Skiko file is rebuilt in place. The nested cinewire archive retains its own supplied native dependencies.

The original `nebula.mixins.json` remains unchanged; inherited stats/cosmetics add-on mixins are removed. Fabric's access-widener path is updated. The editor's archive location does not imply that its HTTP route is the same path. `nebulaclient.json` version is generated from `fabric.mod.json`. AIX/OS400 junixsocket resources were removed. The original `zip.nebula` client namespace and library resource conventions remain where ABI/library contracts require them.

## Statistics and cosmetics implementation

`patcher/src/BuildDirect.java` merges the `StatsPatch` and `CosmeticsPatch` templates into original classes using ASM, remapping self-references, class literals and lambda handles. Template classes are not runtime JAR entries.

| Existing target | Change |
|---|---|
| `zip/nebula/client/lIIlIIllIlIIllllllIlllIlllIIllIlllIlIIlIIIllIllIIIllIlIIlllIIIlI` | Statistics fetch/snapshot, persisted counters and profit/XP bookkeeping |
| `zip/nebula/client/IlIlIlIllIIlIlIlIlIlIllIlIIIIIIIIlllIllIIlIIIIlIIllIlIIIllIIllIl` | Local editor/server, nickname/cape state and texture lookup |
| `zip/nebula/client/IIIIIIlllIIIlIIllIIllllIIlIIllIlIIIlllIlIIIllllIIlIIIlllIlIlIllI` | Profit update hook |
| `zip/nebula/client/llIlIIIlIIlIIllIIlIIIIlllIllIIIIIllIllIIIIIlIIIIIIIIIllIIIIlIIII` | XP update hook |
| `zip/nebula/lIlllIlIIllIIllIIIIlIIlIlllllIIIlllllIlllllIIlIIIIllllIllIIIlllI` | Failsafe event counter |

The UI labels omit the unwanted “Local” prefix. Statistics/profit/XP/failsafe paths use existing classes and lifecycle events. Original native/bootstrap compatibility code is not certified as vendor-original. Initial direct statistics/cosmetics work removed 23 inherited add-on entries; subsequent consolidation removed 20 more helper class files, including the former `nebula/bridge` layer and root `NativeLoader.class`.

Consolidated members live in the existing Loader, NebulaClient, original obfuscated numeric bootstrap holder and `dev.obliqa.L36xKgeb`. A standard URLClassLoader retains the native payload's isolated loading context. Key stays independent. See [consolidation details](HELPER-CONSOLIDATION.md).

## Editor, GIF behavior and cape stutter

The active editor is built from the user-supplied normalized mirror. It mounts the original nickname/cape components and worker rather than the site's login/router/marketing shell. Fonts, icons, worker code and the default cape are bundled; no CDN/demo-skin request is required. The mirror's other modules are retained as reference source but are not all mounted in the editor.

`tools/build-editor.mjs` transforms the supplied modules and bundles `editor/src/app.js` and `host.js`. The host seeds original draft stores and adapts Save to the existing mod state API. Mirror module/version changes require revisiting the guarded transforms; this is not a generic website compiler.

Controls include nickname text/prefix sections, colours, formatting and animation; cape face selection, colour/image, GIF uploads, crop/zoom, replacement/removal, atlas choices, import/export, save/reset and reload. Representative controls were tested. Exhaustive button-for-button parity with the inaccessible live site is **not** certified.

GIF rendering/encoding is real: frames are rasterized into a 2:1 atlas, palette-quantized and compressed. The selected output dimensions are the actual output dimensions. Available sizes are computed from source dimensions/crop/useful maximum; 4096×2048 is not guaranteed. The verified15-frame export/save was1024×512 with 100ms timing per frame. There is no AI upscaling/detail enhancement. A 4096-wide atlas would not make each cape face a 4K image.

The missing3D preview was a CSP defect: SkinViewer fetches `data:`/`blob:` images, while `connect-src 'self'` blocked them. The handler now permits `connect-src 'self' data: blob:`. Script nonce, Host/Origin/token checks, loopback binding and other response protections remain. Pixel-based tests verify a nonblank default cape, drag rotation and changing animated pixels. The cape viewer hides the player skin/body.

Stutter fixes retain state-file metadata checks before reading/parsing large state JSON and cache parsed state/textures. Saves and player changes invalidate the relevant state. The browser viewer updates its existing GPU texture rather than recreating it per GIF frame. The 15-frame agent probe observed animation with zero unchanged-state rereads or texture replacement over10seconds. This is not a universal FPS guarantee or a complete gameplay benchmark.

## Vulkan implementation

The uploaded crash showed `UnsatisfiedLinkError` for `VulkanKt._nMakeVulkan`. The Windows Skiko DLL had internal Vulkan engine code but lacked the custom JNI exports used by the Java facade. Describing it as wholly OpenGL-only was incorrect and was corrected. The supplied Linux build contained the missing bindings.

Matched versions: Skiko 0.153.0, Skia `m153-e63f5d6129`. Matching public Windows Skia archives contain the Vulkan backend implementation. The existing Skiko DLL was rebuilt with `patcher/native/Vulkan.cc`, preserving every original export and adding seven JNI methods for context creation, Vulkan image wrapping, wait/flush/semaphore handling, texture-state transitions, backend surfaces and borrowed images. The matching Linux Skia Java facades replace the old Windows framebuffer adapters; hardcoded-address framebuffer helpers are removed.

The original selected graphics backend is preserved. The rejected OpenGL fallback is not present. Runtime evidence covers Windows Java/Wine with Mesa llvmpipe Vulkan 1.4.305, plus a native OpenGL surface/borrowed-image/pixel-readback probe. Physical NVIDIA/AMD Windows GPUs and full Vulkan gameplay remain to be tested. [Native build details](BUILD.md) and [graphics analysis](GRAPHICS-CRASH.md) contain source/toolchain information.

## Loader/Key regression and repair

The failed layout build renamed Loader, merged Key's fields into it, removed Key and redirected selected native FindClass calls. It passed startup, editor, debugger and cape checks. Those checks missed later menu consumers, and the user reported missing changelogs and:

```text
Could not load leaderboard: Nebula authentication token is unavailable
Could not load changelogs: Nebula authentication token is unavailable
```

The final fix restores the separate original Key class byte-for-byte, keeps both original Java names and restores all native binaries from the preceding working Vulkan build. The layout-specific native redirects are removed. Resource folder organization remains. No credentials were generated and no server authentication was bypassed.

A same-account/same-proxy comparison confirmed that the broken layout build sent neither menu request, while the corrected build sent both with authorization present. Changelog returned HTTP 200. Leaderboard returned 403 with the dummy test account; success for the user's real authenticated account remains unverified. [Evidence](evidence/current/menu-api-comparison.json) records only metadata, not token/header values.

## Preserved profile, clock and key assumptions

The supplied input's LocalProfile behavior is retained in the obfuscated bootstrap holder. It validates launcher username/UUID, derives a local development identifier, and serializes a client-side Dev role plus an `offline` marker. This is a local/development descriptor, not proof of a server-authorized account or multiplayer login; the compatibility fix did not manufacture a server token or grant server permissions.

The preserved clock adapter accepts the diagnostic input marker `1792603363`, caches a system-clock seconds value on its first call and forwards that through the retained native refresh path. This is separate from the captured bootstrap table. Changing such constants without establishing the native contract is not resolver reconstruction. Platform guards still require Windows x86-64. The compiled expected client hash and native resource hashes remain explicit integrity inputs.

There are two different historical classes called Key. The Java-only `BootstrapData$Key` record keyed captured numeric lookups; its replacement uses an unambiguous NUL-delimited String key in the original numeric holder. That helper removal remains. The independent native-facing `zip.nebula.loader.Key` class is retained unchanged. Merging the latter into Loader was the incompatible experiment.

Diagnostic observation of a populated64-character `key` field in both classloader copies did not prove that the later native/menu consumer could locate the expected class and obtain its HTTP token. The A/B request comparison, rather than a field-presence check or startup screenshot, established the functional restoration. Native consumers were not exhaustively decompiled, so the exact internal token getter is not claimed fully reconstructed.

## Resolver findings

The final `thatonecatgif/data/bootstrap.bin` still contains 155936 recorded numeric results,22884292 bytes,3867 owners/class IDs and17915 distinct outputs. B's high32bits identify a class and low32bits a site. The captured Java wrapper delegates to this table; the original resolver algorithm was not recovered. Simple XOR/subtraction and inverse SplitMix64/Murmur64 seed models failed to identify a constant seed in the largest class.

`tools/analyze_bootstrap.py` reproduces the report for old and final resource paths. No guessed resolver is installed. Replacing the table with hardcoded methods or inlined values would still be recorded-data replay, not reconstruction. Further native reverse engineering or original resolver source is required. The exported `cw_tbl` descriptor was inspected, but its resolver/callback relationship is not independently established.

## Debugging and TLS changes

The version-guarded native plan applies nine identified edits: no-debug results for IsDebuggerPresent/CheckRemoteDebuggerPresent and debug-query classes7/30/31, no-op ThreadHideFromDebugger class17, and two startup process-DACL calls replaced with success. Other API classes/symbol lookups delegate normally. Original native function RVAs remain stable for the bridge. Resource checksums and bootstrap expectations are updated; integrity checks remain enabled.

Optional Wine worker recovery hooks are skipped when JDWP is enabled, avoiding a reproduced recovery/JVMTI conflict. Startup Java agents and JDWP attachment passed. Dynamic attach and native debugger attachment were not established: unmodified Java controls also failed under Wine/sandbox restrictions. The identified edits do not prove that every obfuscated protection is removed.

Bundled OkHttp pin checking is disabled; ordinary CA trust and hostname validation remain. A trusted test CA with a mismatched pin passed after patching; an untrusted CA and wrong hostname remained rejected. Native API WebSocket TLS1.3 and HTTP 101 were observed through mitmproxy with RSA and ECDSA inspection certificates. No additional native pinning patch was needed for that observed endpoint.

Native networking ignored JVM proxy settings. External per-process TCP forwarding was needed to intercept it. Rustls used `SSL_CERT_FILE` instead of the Windows ROOT store when that variable was set. A different same-subject CA in the inherited bundle caused BadSignature; preserving roots and adding the actual inspection CA fixed TLS. HTTP Toolkit itself was not installed/tested. Java menu API traffic was captured with explicit proxy/trust configuration.

Native dummy-session proxy runs also produced unresolved Wine illegal-instruction crashes after successful WebSocket upgrades. These runs do **not** establish a complete authenticated native session, and the crash cause is not classified as TLS/pinning, anti-debugging or a new layout failure. The final menu comparison used Java proxying with direct native routing constrained by the cloud network. See [network test setup](../tests/network/README.md) and [debug/TLS analysis](BOOTSTRAP-AND-DEBUGGING.md).

## Validation and material limits

| Area | Evidence/result | Limit |
|---|---|---|
| Menu request regression | Broken vs corrected JAR under identical proxy/account settings; corrected changelog200, leaderboard request authorized | Leaderboard403 for dummy account; real session unverified |
| Native ABI | Final native files identical to working Vulkan baseline; original Key bytes retained | Does not reconstruct original vendor source |
| Vulkan | Windows JDK/Wine startup and rendering with explicit Vulkan | Software GPU; hardware and full gameplay unverified |
| OpenGL interop | Native surface, borrowed image and pixel readback passed | Focused probe, not full gameplay |
| Cape preview | Visible pixels, rotation and changing GIF pixels; later live editor rendering | Not exhaustive browser/GPU coverage |
| Animated cape |15 textures observed; no repeated state read/texture allocation over10s | Not a complete long-session FPS benchmark |
| Editor export/save |15 frames,1024×512,100ms timing, state save/reload | Full live-site parity and all input combinations unverified |
| Debugging | Startup agent/JDWP and targeted native stubs passed | Dynamic/native attach and all protections unverified |
| TLS/proxy | Java API/changelog and native TLS/101 observed | HTTP Toolkit app, every endpoint and authenticated native session unverified |
| Stats | Existing persistence/direct-hook tests and prior startup probes | Real macro gameplay, profit/XP/failsafe events and multiplayer remain separate checks |

[Current evidence index](evidence/current/README.md) separates final-build tests from earlier tests of unchanged components. Older reports retain original hashes/scopes. The final jar/current input hashes are authoritative; not every historical “PASS” applies to every build.

## Build, runtime and continuation

Use [BUILD.md](BUILD.md) for the supported packaging rebuild and the full native rebuild. The packaging path uses the published rebuilt Skiko DLL; full C++ rebuilding requires the pinned public Skiko/Skia inputs, Windows JDK headers, Clang/lld and a separately acquired Windows SDK/CRT. SDKs, private trust material and personal runtime profiles are not repository artifacts.

The retained workspace is `/workspace/freebubu`, Windows test instance `/workspace/minecraft`, Wine environment `/workspace/tools/wine-env.sh`, Xvfb`:99`, editor port6969. Existing workspace scripts are reference implementations; they are not portable installers. Current browser tests are the mirror/preview tests; older editor tests target a superseded UI. Live tests reset/write cosmetics and must use a dedicated profile.

Next work should preserve Loader/Key identity, the original mixin set and the user's selected graphics API. Priorities are real-account leaderboard/menu verification, Windows hardware Vulkan/gameplay, animated-cape performance on the user's device, authenticated native-session/proxy behavior, dynamic/native debugger support on actual Windows, and resolver reconstruction only with independently verified semantics.

The repository includes source, pinned inputs, manifests, current/older evidence, selected screenshots, diagnostics and failed-approach notes. It excludes access tokens, proxy CA private keys, storage.to owner/visitor management tokens, environment credentials, personal profiles, unfiltered traffic, enormous dependency caches and executable failed builds. [Historical findings](history/FINDINGS.md) explain those experiments without promoting them as supported builds.
