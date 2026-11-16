# Bootstrap, recorded constants, debugging, and TLS

## What is present now

The native/bootstrap layer was already in the supplied input. Its vendor
provenance has not been established. It was retained, not rewritten by the direct
stats/cosmetics patch.

| Component | Observed role |
| --- | --- |
| `runtime/loader.jar` | `NativeLoader` and `zip.nebula.loader.Loader`; loads the Windows payload in a separate classloader. |
| `runtime/bridge.dll` | JNI bridge installation plus Skia/framebuffer and worker-policy compatibility exports. |
| `runtime/core.dll` | JNI implementations associated with numerous obfuscated original client classes. |
| `runtime/client.dll` | Main Windows payload loaded by the prelaunch path. Full internals are not audited. |
| `assets/bootstrap.bin` | Map of recorded long-valued obfuscator bootstrap results, not the saved cosmetics/website data. |

`thatonecatgif.Bootstrap` is the declared prelaunch entrypoint. It checks Windows
x86-64, checks the payload SHA256, extracts/resolves embedded resources, loads the
bridge and loader, installs the native bridge, and calls `NebulaClient.earlyLoad`.
Removing this layer without providing its implementations would break the current
build. Payload checksums are integrity checks, not evidence of TLS pinning.

## Recorded bootstrap data

The 22,884,292-byte `bootstrap.bin` has 155,936 records. `BootstrapData` reads a key
of `(owner class, bootstrap name, long argument A, long argument B)` and a long
result. Its bootstrap handler requires a `()long` method type and returns a
`ConstantCallSite`. Missing or incorrectly typed records throw `BootstrapMethodError`.

The original obfuscator bootstrap wrapper delegates to `BootstrapData.bootstrap`.
The data's structure and current use are verified; how the input's author captured
those values, and whether they were complete for every possible path, is not.
The website captures are separate reference material for the cosmetics page.

### Can this be changed?

Yes, but not by substituting guessed numbers. A proposed next task is to enumerate
all call sites using that bootstrap wrapper, prove that each is immutable, match
its owner/name/arguments to the existing table, and replace it with the exact long
constant (for example, a bytecode constant load). Dynamic constants and unusual
call-site forms need separate handling. Assert complete coverage before removing
the table or its wrapper. Test native initialization, client UI, modules, restart,
and previously unvisited paths afterwards.

This removes the external recorded-table lookup, but it still uses the same
recorded values. Deriving fresh results without the snapshot instead requires
recovering the original resolver/native algorithm and its dependencies. Neither
strategy has been implemented in the delivered build.

## What debugging/interception has actually been verified

- **Startup Java agent:** passed. The read-only `ProbeAgent` test was loaded using
  `-javaagent`, inspected actual loaded classes, and invoked the original patched
  statistics method. It was test tooling outside the mod, not an added mod entry.
- **Dynamic agent attach:** unverified.
- **JDWP debugger:** unverified.
- **Native Windows debugger:** unverified.
- **HTTP Toolkit interception:** unverified.
- **Certificate pinning removal:** not performed.

The unchanged `client.dll` contains references to `IsDebuggerPresent`,
`CheckRemoteDebuggerPresent`, and X509-related functionality. These identifiers
alone do not prove that a particular protection is active, nor locate its decision
path. No claim is made that any arbitrary debugger or agent will work.

## Work needed to change anti-debugging or anti-MITM behavior

Treat these as distinct, testable investigations rather than one “disable
protection” flag:

1. Establish separate baselines for startup instrumentation, dynamic attach,
   JDWP, and a native debugger. Record exact failures and their active call paths.
2. Identify active checks in Java and native code, including their initialization
   order and any integrity dependencies. Check whether an available development
   configuration already enables the intended debugging path.
3. Make narrow changes in the owned development build, preserving unrelated
   initialization and behavior. Re-test each attachment mode and normal startup.
4. For TLS interception, first establish which connections use Java HTTP stacks
   versus native networking and whether each honors the configured proxy.
5. Configure the development proxy and its CA explicitly. Distinguish ordinary
   certificate/hostname validation failures from actual leaf/public-key pin checks.
   If a pin check is confirmed, make a scoped development change to that check;
   retain ordinary TLS certificate and hostname validation rather than installing
   a blanket trust-all verifier.
6. Re-test the specific API endpoints, redirects, WebSockets, and native transports
   that matter. An intercepted Java request does not establish interception of
   every native request, and working instrumentation does not establish TLS access.

These changes can affect payload integrity requirements and startup. The supplied
native payload currently has explicit embedded checksums; changes must be handled
as a deliberate new build, not by silently skipping verification. The current
release leaves both the native files and their existing integrity checks intact.
