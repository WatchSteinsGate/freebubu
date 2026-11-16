# Current mirror-based editor

The current workspace editor is built from `nebulaclient-mirror.zip`, supplied
16 November 2026. Normalized `_nuxt` modules, fonts and page styles are retained
under `mirror/`. The ZIP's malformed crawl paths and Cloudflare scripts are not
used. The current publication is identified in `metadata/builds.json` and the repository README.

`NickDraftEditor` and `CapeDraftEditor` from `zbLLP5Yy.js` are mounted directly,
using the mirror's Vue/Nuxt UI components, nickname formatting controls, cape
builder and GIF worker. The website app/router startup is disabled; its page
header, login, navigation, promotional copy and draft-only notices are not mounted.
The cape viewer hides the player body and does not fetch a demo skin.

`src/host.js` seeds the original browser draft stores from the mod's state and
adapts Save to the existing token-authenticated `/api/state`. Nickname formats
are preserved in the original component's representation. Generated cape GIFs
are decoded into timed PNG frames for the native manager, and their draft file
is retained in `editor.mirror` for reload. Old nickname/cape state migrates on
first load. Icons, fonts and the GIF worker are bundled into the existing HTML
resource, so no website or CDN request is required.

Build:

    npm ci --cache .cache/npm --prefix editor/build-tools
    node tools/build-editor.mjs
    ./build.sh

Use JDK 25 for the Java build as documented at the repository root. The server's
CSP explicitly permits bundled blob workers, data fonts and data/blob image fetches in connect-src; script nonces, Host,
Origin and token checks remain enforced. No new editor helper class or mixin is added. The overall build also contains the separately documented Vulkan/native changes and retained stats patches.

Current tests: `tests/test_mirror_editor.py`, `tests/test_mirror_highres.py` and the pixel-based `tests/test_cape_preview.py`.
They test original nickname animation/format controls and worker-based GIF
rendering, exports, frame timing, API saves and browser reload. Add `--live` for
the dedicated mod test instance; live tests reset its cosmetics. Older editor
regression scripts target the superseded UI and should not be used for this one.

GIF rasterization, palette quantization and compression are real. Available atlas dimensions depend on the source/crop/useful maximum; the verified15-frame output was1024×512 with 100ms timings. A4096×2048 atlas is conditional, not a guaranteed option or per-face resolution, and there is no AI detail enhancement. The selected dimensions describe actual encoded output dimensions.
