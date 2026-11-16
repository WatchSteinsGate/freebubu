# Historical handoff archive

This preserves superseded descriptions. Use [the current handoff](../HANDOFF.md) for the supported artifact and current claims.

> Current artifact: [authentication compatibility build](../../metadata/auth-compatibility-build.json). The Loader/Key rename and merge from the layout build are reverted. Their original Java names and the native binaries from the working Vulkan build are retained; resource folders remain reorganized. An identical-proxy comparison restored the menu API requests and changelog HTTP200. Leaderboard returned403 with the dummy test account; a real authenticated session remains unverified.

> Current artifact: [layout build metadata](../../metadata/layout-build.json) and [JAR layout](../jar-layout.md). The loader is now `thatonecatgif.loader.Loader`; native bindings and resource paths follow the reviewed layout. Key fields are merged into Loader and two unused captured TypeSwitch classes are removed. Vulkan, debugger attachment and animated cape checks passed.

> Latest build (16 November 2026): [Vulkan and consolidation artifact metadata](../../metadata/vulkan-consolidation-build.json), [direct consolidation](../HELPER-CONSOLIDATION.md), and [Vulkan repair](../GRAPHICS-CRASH.md) supersede the archive layout and artifact details below. The current JAR removes 20 remaining helper class files, adds no files, repairs the real Windows Vulkan bindings and cape preview, and retains the animated cape/stats/debug changes. The recorded bootstrap table remains necessary. Earlier artifact descriptions below are historical.

# Full handoff — 16 November 2026

Current development build: see [debug-build metadata](../../metadata/debug-build.json), [current JAR layout](../jar-layout.md), and [resolver/debugging/TLS status](../BOOTSTRAP-AND-DEBUGGING.md). The release-validation details below describe the earlier handoff artifact.

## Current state and deliverables

The final deliverable is `Nebula-Windows-direct.jar`, stored with Git LFS under `artifacts/`, alongside its original input,
`Nebula-Windows-original.jar`; the GitHub handoff release links to these files. The input
is the unmodified `Nebula-Windows.jar` originally supplied through storage.to.
Use the manifest/checksum files at the repository root to identify exact artifacts.

A temporary mirror was also uploaded to `https://storage.to/n0XBFdX0o`, with expiry
`2026-11-23T05:57:36Z` (23 November, 11:42 AM Nepal time). The GitHub release is the
handoff's persistent reference; the storage.to link expires.

Earlier `Nebula-Windows-local-editor.jar` builds are superseded. Do not install
both modified variants. The test instance currently uses the direct build, and
its installed JAR was checked against the delivered artifact's SHA256.

## Scope and provenance

The supplied input JAR already contained `thatonecatgif` bootstrap, native
compatibility, statistics, and cosmetics additions. These were not stock-vendor
provenance verified by this work. “Original” in earlier discussion referred to
what was present in the supplied input, not a verified pristine vendor build.

The first iteration replaced only the embedded editor HTML. The final iteration
moves the inherited statistics/cosmetics functionality into existing original
Java classes and removes their separate helper classes and mixin wrappers. This
is a direct bytecode patch, not another added mixin layer. Unrelated bootstrap,
rendering/native compatibility code, and native DLLs are retained.

## Exact archive changes

Seven existing entries change:

1. The original profile API implementation:
   `zip/nebula/client/lIIlIIllIlIIllllllIlllIlllIIllIlllIlIIlIIIllIllIIIllIlIIlllIIIlI.class`.
2. The original cape manager:
   `zip/nebula/client/IlIlIlIllIIlIlIlIlIlIllIlIIIIIIIIlllIllIIlIIIIlIIllIlIIIllIIllIl.class`.
3. The original profit processing class:
   `zip/nebula/client/IIIIIIlllIIIlIIllIIllllIIlIIllIlIIIlllIlIIIllllIIlIIIlllIlIlIllI.class`.
4. The original XP processing class:
   `zip/nebula/client/llIlIIIlIIlIIllIIlIIIIlllIllIIIIIllIllIIIIIlIIIIIIIIIllIIIIlIIII.class`.
5. The original failsafe event DTO:
   `zip/nebula/lIlllIlIIllIIllIIIIlIIlIlllllIIIlllllIlllllIIlIIIIllllIllIIIlllI.class`.
6. `thatonecatgif/FreeCosmeticsLOL/editor.html`.
7. `fabric.mod.json`.

The full removed-entry list is in `evidence/archive-audit.json`: 23 entries, zero
additions. Removed components include `LocalStatsMod`, `LocalStatisticsStore`,
`StatsPath`, `GainAccumulator`, `SerialWriteQueue`, the add-on stats/warning mixins,
`CosmeticsMigration`, `FreeCosmeticsLOL`, `Editor`, their nested classes, and the
`SelfCape` mixin. The existing editor HTML resource remains at its old path.

The only registered mixin configuration is now `nebula.mixins.json`, containing
the original client's mixins. The profile API and cape manager become client
entrypoints implementing `ClientModInitializer`. Their existing no-argument
constructors are made public for Fabric.

## Build implementation

`patcher/src/StatsPatch.java` and `CosmeticsPatch.java` are compile-time templates.
`BuildDirect.java` merges their fields/methods into the original classes with ASM,
remapping self-references, class literals, and lambda handles. The templates'
classes are not added to the output. The patcher recomputes stack-map frames and
asserts the exact number of interception sites. Missing/duplicate matches or
retained instruction references to removed helper classes fail the build.

`metadata/dependencies.lock.json` pins the build classpath to SHA256 values and
records its original artifact URLs or nested-JAR extraction locations. The input
JAR is separately hash checked. Large binaries are Git LFS objects. Direct release-asset upload was rejected
with HTTP 401 by `uploads.github.com` despite working API/repository access.
GitHub Git LFS authentication was tested successfully and used instead.

## Statistics

The private original statistics-fetch method returns the current account's
persisted `zip.nebula.Statistics` snapshot instead of fetching its statistics from
the former service path. Direct insertions record profit, XP, and live failsafe
changes at the inherited mixins' former interception points. Existing gain logic
is otherwise retained. Runtime tick observation records active macro time and
start transitions. The failsafe guard distinguishes live native sending from
constructing historical DTOs.

The “Local macro time / Local failsafes / Local macro starts” UI overrides are
removed with `ProfileCardsMixin`; original labels are restored. This work does
not claim to disable all client telemetry or other native networking.

Storage: `Nebula/stats/<UUID>.properties`, preserving schema 1 and the existing
base64-encoded metric keys, decimal gain values, time zone, and daily buckets.
Legacy `nebula-local-statistics/<UUID>.properties` is copied only when the current
file does not exist. Existing originals are retained. Atomic writes occur on
periodic checkpoints and clean shutdown. An abrupt termination may lose the
uncheckpointed interval (up to about ten seconds), as with the prior design.
Interrupted sessions do not count time while the client was stopped.

The fetch replacement is scoped to the active launcher's account. Other players'
remote statistics are not implemented by this snapshot. Live macro/profit/XP and
failsafe gameplay events were not exercised on an authenticated server.

## Cosmetics and editor

The original cape manager now contains the loopback HTTP editor service, storage,
nickname application, and animated cape texture handling. The original cape
lookup method first checks whether a saved override exists for the requested
UUID. Overrides apply only to the current player's UUID.

Storage: `Nebula/cosmetics/<UUID>.json`. Existing files under
`Nebula/thatonecatgif/FreeCosmeticsLOL` or `Nebula/local-cosmetics` are copied if
the destination is absent, leaving the source files untouched. The JSON retains
schema/owner/revision checks, nickname frames, cape PNG frames, and editor metadata.
Owner changes restore the previous nickname row and release old textures.

The editor listens on `127.0.0.1:6969` by default (override with the JVM property
`nebula.cosmetics.port`). `/api/state` uses a per-process token carried in
`X-Cosmetics-Token`; Host and Origin checks are enforced, and CSP prevents external
scripts. There is no website login, purchase flow, marketing header, or “Local” UI
label. Save/reset feedback is short. State writes are atomic.

The cape-only preview, six face selectors, colors, source replacement/removal,
PNG/JPG/WebP/GIF inputs, drag crop, scroll/slider zoom, center crop, atlas import,
Full/Half/Quarter/Eighth choices, and PNG/GIF export are implemented. The original
captured GIF codec and viewer code are embedded in the nonce-bearing page script.
The player body is hidden to match the supplied cape-only screenshots.

A 350 ms debounce schedules output generation. Rendering-frame and
optimizing/compressing statuses describe actual encoding work. Interactive
previews use at most a 256-wide atlas; saves/downloads use the selected resolution.
The codec runs without the site's missing worker asset, so orchestration and
responsiveness are not claimed to be identical to the hosted worker pipeline.

Bounds: 25 MiB per source image; 10 MiB per complete atlas import; 128 cape frames;
4096-wide output; 134,217,728 total output pixels; 64 MiB JSON state; 16 MiB per PNG
frame; 20–10000 ms cape delays; 50–10000 ms nickname delays. Source GIF decode has a
33,554,432-pixel budget. Multiple animated faces must match frame counts/timing.
Invalid input reports an error. Those bounds are local implementation limits,
not a claim about every hosted-site limit.

## Validation evidence

Completed:

- Windows Java 25 and Wine start the client to the Nebula main menu.
- Statistics tests: decimal profit and XP; elapsed time; daily boundary split;
  start transitions; atomic save/reload; interrupted sessions; invalid gain inputs.
- An external test agent loaded at JVM startup, invoked the patched original
  statistics-fetch method, compared returned totals with the persisted snapshot,
  and confirmed the removed helpers/mixins were absent from loaded classes.
- Live editor tests exercised each face/color, source formats, actual choose and
  replace dialogs, crop/zoom/center, available atlas buttons, remove, animated atlas
  import, PNG export, GIF frame count/timing, save/reload, and 3D rotation.
- A 15-frame GIF showed 4096/2048/1024/512 choices and rendering progress, saved
  1024×512 PNG frames, exported 15 frames at their original 100 ms timing, and
  restored its source after reload and after restarting the client.
- Nickname save/reload passed through the actual patched API.
- Missing tokens, wrong Host, and cross-origin requests were rejected.
- The output archive passed CRC validation; no entries were added; the native
  DLLs match the input; the installed test JAR matches the delivered JAR.

Expected/unresolved observations:

- Authentication/Realms requests fail with the dedicated unauthenticated test
  identity; authenticated multiplayer was not tested.
- Discord integration reports no running Discord client.
- Wine reports Windows hardware/performance-counter warnings. The test runtime
  can also report no usable OpenAL audio device. Gameplay audio was not validated.
- The original client's existing mixin overwrite warnings remain.
- `nebulaclient.net` continued returning HTTP 403. Screenshots/modules were the
  reference; exhaustive live-site parity remains unverified.
- The earlier handoff left protections unchanged. The current development build patches identified native debugger checks, startup process-permission restrictions, and bundled OkHttp pin verification; see the current resolver/debugging/TLS report for scope and remaining limits.

## Workspace at handoff

The managed cloud workspace retains:

```text
/workspace/downloads/Nebula-Windows.jar       supplied input, unchanged
/workspace/nebula-work/                       build lab, earlier outputs, references
/workspace/nebula-work/Nebula-Windows-direct.jar
/workspace/nebula-work/direct/src/            original lab patcher/templates
/workspace/minecraft/                        dedicated Fabric/Wine test instance
/workspace/minecraft/mods/Nebula-Windows-direct.jar
/workspace/tools/                           Wine, Windows/Linux JDKs, package caches
/workspace/attachments/                     uploaded reference material
/workspace/freebubu/                         this GitHub handoff checkout
```

Per-upload storage.to management tokens and platform/GitHub credentials are not
part of this repository. Do not copy them from the workspace into commits.

Environment draft startup instructions were updated for the direct build. The
agent has not published the cloud environment. This GitHub handoff/release is a
separate, explicitly requested publication.
