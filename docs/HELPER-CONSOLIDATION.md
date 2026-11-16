# Direct member consolidation

`patcher/consolidate/Consolidate.java` is a build tool, not part of the mod. It moves the former helper methods and fields into existing classes, remaps callers and method handles, preserves existing initialization order, and changes Fabric prelaunch to the original loader class.

- `zip.nebula.loader.Loader`: prelaunch, native extraction, bridge registration, worker policy, verified startup assets, GUI scale.
- Original obfuscated bootstrap holder: numeric lookup, clock refresh and launcher profile serialization.
- `zip.nebula.client.NebulaClient`: existing startup sequence.
- `dev.obliqa.L36xKgeb`: native core loading.

The 20 helper class files, including all of `nebula/bridge` and root `NativeLoader.class`, are removed. A standard URLClassLoader supplies the native payload's existing isolated loader context. The existing bridge DLL's JNI exports and registered clock target are patched to the retained loader class. Its obsolete framebuffer export is removed. The existing Skiko DLL supplies real texture interop instead of the framebuffer helper classes.

The recorded `assets/nebula/bootstrap.bin` remains necessary: this change relocates its lookup implementation into the original resolver class; it does not reconstruct the lost original resolver algorithm.

The later layout experiment that moved Loader and merged the independent Key class is superseded. Both native-facing classes now keep their original `zip.nebula.loader` identities. Their native consumers extend beyond the startup calls exercised by the initial layout tests. Resource reorganization remains in place; no compatibility shim is added.
