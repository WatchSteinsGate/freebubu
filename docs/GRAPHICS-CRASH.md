# Windows Vulkan binding repair

The supplied Windows crash reaches the original Vulkan renderer and fails at `VulkanKt._nMakeVulkan`: the shipped Windows Skiko DLL lacks its JNI bindings. The supplied Linux build contains them. Both use Skiko 0.153.0 and Skia m153-e63f5d6129.

The existing `thatonecatgif/native/windows-x64/skiko-windows-x64.dll` is rebuilt from the matching Skiko source and matching Windows Skia libraries, with `patcher/native/Vulkan.cc` providing context creation, borrowed Vulkan image textures, semaphore wait/signal, image-state transitions, and backend texture surfaces. The matching Linux Java facades replace the Windows framebuffer adapters. All original native exports are retained. No additional DLL filename, Java class, or mixin is introduced. Minecraft's selected graphics backend is preserved.

The original OpenGL workaround was rejected and is excluded from this build. Current artifact identity and scoped evidence are in `metadata/auth-compatibility-build.json` and `docs/evidence/current/`.

The final artifact reached Minecraft's first-run screen under Windows JDK 25/Wine with explicit `--graphicsBackend vulkan`, using Mesa llvmpipe Vulkan 1.4.305. A startup Java agent observed all 15 animated cape textures with zero unchanged-state rereads or texture replacement over ten seconds. A JDWP debugger attached successfully. The real embedded editor served the corrected CSP and rendered its cape preview. Hardware NVIDIA/AMD Windows testing remains for the user.

The native OpenGL texture probe also passed surface rendering, borrowed-image creation and pixel readback after the remaining borrowed-image JNI method was included. This verifies removal of the old framebuffer adapters without relying on their hardcoded native addresses.

The export comparison retained all 1660 original exports and added seven JNI exports. The final authentication repair preserved this DLL byte-for-byte; it reverted the separate Loader/Key ABI experiment. See BUILD.md for both packaging and full-native build paths.
