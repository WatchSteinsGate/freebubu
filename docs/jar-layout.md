# Windows JAR layout

- Root: Fabric metadata, original mixin configuration and generated client build metadata.
- `META-INF/`: manifest, bundled API JAR, licenses and `world-offset.accesswidener`.
- `thatonecatgif/native/windows-x64/`: existing DLLs, native archive, ICU data and integrity metadata.
- `thatonecatgif/data/`: recorded numeric bootstrap table, foraging data and three startup sounds.
- `zip/nebula/`: original client packages, including the native-facing `loader/Loader.class` and `loader/Key.class`.
- `assets/nebula/`: game assets and the embedded editor.
- `lib/`: retained Windows junixsocket resources.
- Third-party Java packages retain their library namespaces.

Loader and Key preserve their original class names and separate identities. The attempted Loader relocation and Key merge are reverted: startup-only tests did not establish compatibility with the later authentication and menu consumers. The native lookup redirects introduced for that layout are removed. All native files are byte-identical to the preceding Vulkan build that the user reported working.

The original Key class is restored byte-for-byte. This is a retained native interface, not a new helper layer. The 20 previously consolidated helper class files stay removed. Two unused captured TypeSwitch class files remain removed.

`Layout.java` changes only resource paths and Fabric's access-widener path. `layout-native.py` verifies the existing checksums and native ABI; it does not patch native lookup functions. The recorded bootstrap table remains required. The original mixin configuration is unchanged. JAR resource paths and the local editor's HTTP routes remain separate.
