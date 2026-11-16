# Resolver, debugging and TLS — current development build

The supplied input already contained the recorded resolver and native compatibility layer. Their vendor provenance has not been established. The current native client is intentionally patched for debugging; packaging generates its new resource SHA-256 and updates the bootstrap's expected payload hash. Integrity verification stays enabled.

## Resolver reconstruction attempt

`thatonecatgif/data/bootstrap.bin` still holds 155,936 records (22,884,292 bytes), covering 3,867 owners and 17,915 distinct results. Its keys contain owner, bootstrap name and two long arguments. Argument B's high 32 bits identify the class; its low 32 bits identify the site. Each observed class ID maps to exactly one owner. A and B are individually unique across the snapshot.

The supplied Java bootstrap body delegates to the table; the original algorithm is absent from that Java method. Native entry points, registration wrappers and the exported `cw_tbl` descriptor were inspected. The latter's relationship to the resolver and callback ABI remain unverified. Tests of simple XOR/subtraction and invertible SplitMix64/Murmur64 seed models did not produce a constant seed for the largest class (1,624 records).

No independently verified replacement algorithm was recovered. The table and resolver wrapper remain required. `tools/analyze_bootstrap.py` reproduces the structural/candidate analysis and writes a JSON report; `build/resolver-reconstruction.json` contains this run. Completing reconstruction requires further native reverse engineering or the original resolver implementation. Inlining the recordings would not reconstruct the original algorithm.

## Native changes

`patcher/native/debug-patch.json` contains expected bytes and nine guarded patch locations. `tools/patch_native_debug.py` accepts only the exact input native-client SHA-256. Two PE sections hold position-independent leaf functions and a native import table; all original function RVAs stay stable for the existing bridge.

- `IsDebuggerPresent`: returns false.
- `CheckRemoteDebuggerPresent`: writes false and returns success for the valid output pointer used by the client.
- Named `NtQueryInformationProcess` lookups: route debug-port (7), debug-object (30) and debug-flags (31) queries to no-debugger results. Other classes and symbol lookups delegate to the genuine Windows API. Debug-object queries return `STATUS_PORT_NOT_SET`.
- `NtSetInformationThread`: class 17 (`ThreadHideFromDebugger`) returns success without hiding the thread. Other classes delegate to Windows.
- Two `SetSecurityInfo` call sites in JNI startup are replaced with success results. These calls set a kernel-object DACL and restrict process access. The general Windows security API is unchanged.

These address identified paths; this is not a complete audit of every obfuscated native check. The `zip.nebula` native ABI is still retained.

The WorkerPolicy methods, now consolidated into `zip.nebula.loader.Loader`, skip the optional Wine recovery hooks when JDWP is enabled, fixing a reproduced `Cannot install Linux recovery worker policy` startup failure. `-Dnebula.workerRecovery=false` also disables these hooks explicitly. Normal startup retains the existing recovery behavior.

## Certificate pinning

The packaged OkHttp `CertificatePinner.check$okhttp` method now returns without enforcing pins. Ordinary trust-chain and hostname verification remain enabled. This changes pin enforcement in the mod's bundled Java HTTP stack even when callers configure pins.

A local TLS test used a dedicated CA and an intentionally mismatched pin. The original JAR rejected it with `Certificate pinning failure`; the patched JAR accepted it when the CA was trusted, and still rejected an untrusted CA and a hostname mismatch. Test CA material stays outside the artifact.

Actual client interception has now been tested with mitmproxy. Java requests to `api.nebulaclient.net` passed through it. The native WebSocket also completed TLS 1.3 and received `101 Switching Protocols`, with both RSA and ECDSA inspection certificates. No additional native TLS patch was needed for this endpoint.

The native worker needed process-level TCP forwarding; JVM HTTP proxy settings alone did not route it. Its initial `BadSignature` failure came from the inherited `SSL_CERT_FILE` bundle, which selected a different CA with the same mitmproxy subject instead of using the Windows ROOT store. Preserving that bundle's existing roots and adding the correct inspection CA fixed it. A trusted certificate for `wrong-host.invalid` was still rejected for `api.nebulaclient.net`, confirming native hostname validation remained active.

These results cover the observed API WebSocket and Java requests, not every endpoint or an authenticated session. HTTP Toolkit itself was not installed; the test used the same HTTP CONNECT/TLS interception model through mitmproxy. See [setup instructions](../tests/network/README.md) and [test metadata](../metadata/network-interception.json). The helper `tools/configure-interception-trust.py` creates separate Java and native trust files without changing the originals.

## Validation

- Direct native functions exercised under Windows Java/Wine: debugger flags, debug queries, thread hiding and startup DACL-call removal passed; normal process queries, security APIs and symbol lookups still delegate correctly.
- Actual Java API requests and native API WebSocket interception: passed. Native trusted-wrong-host rejection: passed. The tested JAR SHA-256 stayed unchanged.
- Startup Java agent: passed.
- JDWP: client initialized with the agent enabled; socket attachment, VM inspection and detach passed.
- Dynamic agent attach: not verified working. Wine returned access denied for the client. A separate unmodified Java control process also failed dynamic attachment, with a remote-thread error (`0xc0000005`), establishing an additional Wine/JDK limitation.
- Native debugger attachment: unverified working. WineDbg returned access denied (error 5) for both the client and a separate unmodified Java control process, so this environment cannot establish native-attachment support.

`tests/NativeDebugProbe.java`, `tests/PinningProbe.java`, `tests/JdwpProbe.java` and `tests/DebugAttachProbe.java` are external validation tools, not shipped mod classes. Regenerating the native patch plan additionally uses pefile and Keystone (`tools/generate-debug-patch.py`); applying the checked-in native plan itself needs only Python's standard library. Full packaging also uses the pinned pefile dependency; see BUILD.md.
