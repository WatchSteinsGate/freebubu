# Current evidence and scope

The current delivered JAR is identified by `metadata/auth-compatibility-build.json` and `metadata/builds.json`, SHA256 `0f34574ebbe06259cabb04e3cf8918d7cedc7bc1a6aa8bc9d10b2523d41d2f93`.

- `menu-api-comparison.json`: same test account/proxy comparison. The broken layout build sent no changelog/leaderboard menu requests. The corrected build sent both with authorization present; changelog returned 200 and leaderboard403. Header values, request bodies and full paths are not recorded.
- `native-abi-restoration.json`: native file identity against the known working Vulkan build, unchanged original Key class and the four same-path entries changed by resource relocation.
- `native-hashes.json`: checksums of the final native payloads.
- `runtime-excerpts.log`: final-build Vulkan startup, animated-cape and JDWP excerpts. The separate OpenGL probe exercised the identical rebuilt Skiko DLL and restored Skia facades; it is not a complete hardware/gameplay test.
- `cape-preview.json`: canvas pixel/drag/GIF assertions from the editor preview regression. This ran before the later resource-only layout changes; the relevant editor/CSP behavior was retained. A later live editor screenshot is included in `docs/screenshots/`.
- `resolver-analysis.json`: structural/candidate analysis of the recorded table, not a recovered resolver.

Older reports under `metadata/` and `docs/evidence/` are preserved with their original build scopes. Their passing tests do not establish that every historical build supports the current features. No full authenticated native session, real-account leaderboard, arbitrary dynamic agent, native debugger, exhaustive original-site parity or HTTP Toolkit application test is claimed.

The publication packaging rebuild reproduced **every decompressed entry's SHA256**, with no added/removed/changed entries. Whole-JAR timestamps produced a different rebuild checksum; `packaging-rebuild.json` records both hashes. `jar-inventory.tsv` lists every published entry's SHA256, uncompressed size and path; `jar-summary.json` records counts by root. The published executable remains the exact original current deliverable, not the timestamp-varied reproduction.

`stats-regression.log` records the publication-time persistence/profit/XP/elapsed-time regression run. `vulkan-crash-excerpt.txt` contains only technical stack/environment lines from the supplied crash report; the original file's identity is recorded in `metadata/source-provenance.json`.

The source-publication pass found no matches for credential/private-key patterns in staged text files and validated local documentation links. These are publication checks, not a claim to have exhaustively audited proprietary binary internals. No diagnostic key values or private CA/upload state are included.
