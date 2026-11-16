# Findings, experiments and corrections

These are historical findings and discarded approaches. The current artifact is identified by `metadata/auth-compatibility-build.json`; do not install an intermediate JAR because an earlier narrow test passed. Existing historical JSON reports retain their original hashes and scopes.

## Earlier input/editor work

The supplied Windows input already contained compatibility/bootstrap and local stats/cosmetics code. It was not established as pristine vendor code. The first editor work used saved HTML/modules and screenshots because the hosted page could not be accessed reliably. The later user-supplied `nebulaclient-mirror.zip` supplied normalized Vue/Nuxt components and GIF workers, which replaced that reconstruction. Login, marketing/navigation and browser-draft-only notices were not mounted in the local editor.

The initial stats/cosmetics direct patch removed 23 inherited helper/mixin entries and merged the behavior into existing classes. Packaging then consolidated asset/native roots, flattened the two-class loader archive, removed wrong-platform junixsocket files, generated version metadata and renamed the inherited helper namespace. Later member consolidation removed 20 remaining helper class files; this did not imply that the supplied input had no helpers or that all underlying native compatibility code was vendor-original.

## Animated cape stutter

The manager previously reread/reparsed large JSON state on a repeated timer even when unchanged. File metadata checks, cached parsed state and texture reuse avoid that repeated work. Browser frame updates also reuse a GPU texture. A15-frame probe observed changing textures without unchanged-state rereads/reallocation over10seconds. This is representative evidence, not a complete frame-time/gameplay benchmark.

## Preview false positives and actual CSP defect

An older UI probe used outdated button/tab assumptions and did not actually enter the cape view, so its screenshot did not establish that the preview rendered. The replacement regression checks actual canvas pixels, drag-induced silhouette change and differences between GIF frames. It exposed a real CSP failure: SkinViewer fetches data/blob images and `connect-src 'self'` rejected them. Adding those two schemes to connect-src repaired the preview. Live server headers and a later live preview were also checked.

## Vulkan diagnosis and rejected OpenGL workaround

The user's Windows crash identified `VulkanKt._nMakeVulkan` as missing. Initial wording that the Windows DLL was OpenGL-only was too broad: it had internal Vulkan engine exports but not the custom JNI facade. The supplied Linux JAR contained actual Vulkan JNI bindings; stock Maven Skiko 0.153.0 did not provide these custom bindings.

A prototype edited the existing graphics factory to force OpenGL, and it did get past startup with a stored/explicit Vulkan preference. The user explicitly required actual Vulkan and rejected that fallback. It was removed before the supported deliverables. Its build-time source and original report are archived in `rejected-opengl/`, clearly outside the active patch pipeline.

The final approach matched Skiko 0.153.0/Skia `m153-e63f5d6129`, obtained matching Windows static archives that contain the Vulkan backend, rebuilt the existing DLL and added proper JNI. Cross-build obstacles included Windows header/library casing, WinRT headers, Vulkan headers, missing `.cpp` units, static-vs-dynamic CRT mismatch and export retention. The active build script addresses these. Every original export was retained; seven JNI methods were added.

A standalone GL probe initially lacked Kotlin dependencies. After correcting the test classpath, a wrong texture-id/target argument order in that probe triggered a native assertion. Correcting the probe exposed the genuinely missing borrowed-image JNI method; it was added to the same DLL. The final probe verified surface rendering, borrowed images and pixel readback. These failures do not represent a supported OpenGL fallback.

## Loader/Key cleanup regression

The proposed engine-room layout moved Loader into `thatonecatgif.loader`, merged the original Key fields into it and removed two stored hidden TypeSwitch classes. The native payload contained obfuscated class-name construction, so editing clear UTF-8 strings alone was insufficient.

An initial apparent FindClass helper at RVA `0x2428580` did not handle the startup lookup. Another attempted call-site interception around `0x1f14215` treated an unrelated/null argument as a class string and failed; it was discarded before publication. A diagnostic JVMTI agent that logged only class names/call sites located the actual startup routine at call paths `0x83ef3` and `0x84388`. A temporary `.nblay` section redirected old Loader/Key names. This let startup, cape/editor and JDWP tests pass, but later menu token consumers still failed.

The user reported that the tracked-profit/XP GUI showed missing-token errors for both leaderboard and changelog, whereas the previous Vulkan build without `thatonecatgif/` worked. This demonstrated that startup tests were insufficient. Restoring Key alone was tried privately but did not establish complete compatibility. The final fix restores both original class identities and removes all layout-specific native lookup redirects. All native files are byte-identical to the previously working Vulkan build; only resource locations and their Java/Fabric references remain reorganized.

The final same-proxy/same-account comparison is stronger than startup evidence: the broken layout made neither menu request; the corrected build sent both with authorization present. Changelog returned 200. Leaderboard returned 403 for the dummy account. No token/header values were published and no server-authentication bypass was added. The original Key is retained as a native interface, not replaced by a new shim/helper class.

## Resolver reconstruction attempt

The captured snapshot contains 155936 records,3867 owners/class IDs and17915 distinct results. B appears structured as class ID/site index. Simple XOR/subtraction and invertible SplitMix64/Murmur64 seed models were tested on the largest class, with 1624 distinct inferred seeds rather than one fixed seed. No verified original resolver was recovered. The original bootstrap body in the supplied Java delegates to the recorded table. The `cw_tbl` descriptor was inspected, but its resolver/callback relation was not independently resolved.

Keeping the recorded table is intentional. Inlining captured values, changing arbitrary records, or introducing guessed results would not reconstruct the original algorithm. The table's location moved; its required semantics did not disappear.

## Debugging and interception findings

The native plan neutralizes nine identified sites while retaining normal API delegation and resource integrity. It is not proof that every obfuscated anti-debug path is gone. Optional Wine worker-recovery hooks conflicted with JDWP; skipping them when JDWP is enabled allowed debugger startup. Startup agents/JDWP passed. Dynamic attach and WineDbg attachment also failed against unmodified Java control processes, so those results cannot establish universal mod protection removal.

OkHttp pin checking was disabled directly in the existing class. Trust-chain and hostname validation remain. Actual native TLS/HTTP 101 interception succeeded with trusted RSA and ECDSA inspection certificates. An inherited `SSL_CERT_FILE` bundle had a different same-subject mitmproxy CA; Windows ROOT alone did not override it. Preserving the existing roots and adding the actual inspection CA fixed BadSignature. A trusted wrong-host certificate remained rejected.

The native worker ignored JVM HTTP proxy properties; external process-level routing was required. HTTP Toolkit itself was not installed. Native dummy-session interception runs produced Wine illegal-instruction crashes after successful WebSocket upgrades; the cause remains unresolved and no authenticated native session is claimed. Java menu requests were separately tested under explicit proxy/trust settings without relying on successful native protocol authentication.

## Build history references

- The first published `handoff-2026-11-16` tag and its manifests describe the early stats/cosmetics direct build.
- `metadata/debug-build.json` / `network-interception.json` scope the debugging/TLS build and its tests.
- `metadata/vulkan-consolidation-build.json` records the Vulkan/consolidation deliverable.
- `metadata/layout-build.json` records the **superseded broken layout** build.
- `metadata/auth-compatibility-build.json` identifies the supported corrected build.
- `HANDOFF-before-current-publication.md` preserves older narrative material; its older “current/final” labels are historical, not authoritative.

The repository preserves reports, source, targeted diagnostics and useful screenshots. Transient compiler failures, private trust keys, upload management tokens, credential-bearing traffic, enormous SDK/package caches, copied game profiles and executable failed builds are not publication artifacts. Public source archives are pinned for retrieval rather than duplicated wholesale in Git.
