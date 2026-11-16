# Interception test setup

The mod can be inspected through a trusted HTTPS proxy. Java and native networking need separate configuration. `metadata/network-interception.json` records the earlier debugging build; the current menu/API comparison is in `metadata/auth-compatibility-build.json` and `docs/evidence/current/menu-api-comparison.json`. No CA private key, proxy agent or forwarding library is included in the mod.

## Test proxy dependencies

```sh
python3 -m venv .cache/interception-tools
.cache/interception-tools/bin/pip install -r tests/network/requirements.txt
```

Use that environment's Python for the trust helper if `cryptography` is not otherwise installed. `run_proxy.py` expects its `mitmdump` executable at this location.

## Trust files

Generate copies of the existing trust stores with the inspection proxy's public CA added:

```sh
python tools/configure-interception-trust.py \
  --ca /path/to/proxy-ca-cert.pem \
  --java-cacerts /path/to/jdk/lib/security/cacerts \
  --out /path/to/inspection-trust
```

The helper requires `cryptography` and `keytool`. `--base-ca-bundle` optionally selects the existing PEM roots; otherwise it preserves the inherited `SSL_CERT_FILE` bundle or the platform's default roots. Original files stay unchanged. The output is a Java trust store, a native PEM bundle and a configuration JSON.

For the Minecraft process, add these JVM arguments, substituting the proxy's actual port and the generated Java trust-store path:

```text
-Dhttp.proxyHost=127.0.0.1
-Dhttp.proxyPort=PROXY_PORT
-Dhttps.proxyHost=127.0.0.1
-Dhttps.proxyPort=PROXY_PORT
-Dhttp.nonProxyHosts=localhost|127.*
-Djavax.net.ssl.trustStore=PATH_TO_GENERATED_JAVA_CACERTS
-Djavax.net.ssl.trustStorePassword=changeit
```

Set the **same process's** `SSL_CERT_FILE` to the generated native PEM bundle. On Windows, use a Windows path; under Wine, use its mapped `Z:/workspace/...` path. This is an explicit per-process trust configuration, not a trust-all verifier. Leave inherited HTTP proxy variables intact.

When `SSL_CERT_FILE` is set, Rustls's native-certificate loader uses that file instead of the Windows ROOT store. Importing a CA into Windows alone did not fix the native connection in this environment. A same-name CA with a different key in the inherited bundle caused `BadSignature`; adding the correct inspection CA resolved it.

## Native routing

The native WebSocket worker ignored JVM HTTP proxy settings in the observed run. It also needs per-process TCP forwarding to the inspection proxy. A Windows process-level redirector must provide that routing; a system/JVM HTTP proxy setting alone was insufficient in this test. HTTP Toolkit itself was not installed or directly tested; the test proxy was mitmproxy.

For the cloud/Wine test, `observe.c` was loaded into a separate Wine prefix's processes/server using `LD_PRELOAD`. With `NEBULA_REDIRECT_ENABLE_FILE` absent it logged direct connection metadata. Creating that file enabled port-443 CONNECT forwarding to loopback port 8080. `ipv6_relay.py` supplied the IPv6 loopback listener. The proxy then chained through the inherited Cloud proxy, with upstream TLS verification enabled. All forwarded traffic followed the existing outbound policy.

Compile the external observer/forwarder before starting that Wine prefix:

```sh
gcc -shared -fPIC -O2 -Wall -o /path/to/observe.so tests/network/observe.c -ldl
```

Its process variables are `LD_PRELOAD`, `NEBULA_NETWORK_LOG` (JSONL output), and `NEBULA_REDIRECT_ENABLE_FILE` (activation-file path). It must be present when the prefix's Wine server starts, not just when an already-running server launches a Java child. Use an isolated test prefix; do not replace a shared library while processes have it mapped.

`run_proxy.py` starts loopback mitmproxy through the inherited upstream proxy and CA trust. Its optional test controls are `NEBULA_PROXY_PHASE`, `NEBULA_TLS_CLIENT_MAX` and `NEBULA_MITM_CERT`. `observe_mitm.py` records only connection metadata, hosts, methods, status codes and WebSocket upgrades; it does not record headers, query strings, paths or bodies. Logs and test CA material belong in ignored `build/` and `.cache/` directories.

A successful TLS handshake and `101 Switching Protocols` establish interception of the native API WebSocket. They do not establish an authenticated session or coverage of every endpoint.

The controlled certificate variants can be generated with `make_test_certificates.py` from the private CA created by this test proxy. Keep that key and the generated PEM files in ignored `.cache/` storage. `WindowsTrustProbe.java` independently confirmed the public CA was in Windows ROOT and accepted by Windows-store-backed Java validation; its `--delete` mode targets only certificates exactly matching that test CA and verifies their absence. On this Wine build the Java store deletion did not persist, so the exact test-certificate thumbprint key was removed from the Wine user ROOT registry and a fresh store load confirmed it was absent.

The metadata observer records only host/method/upgrade/status, a coarse changelog/leaderboard/other route category and whether an authorization header exists. It does not record authorization values, full paths, query strings or bodies. The supported final build restored the original Loader/Key identities; do not infer full authenticated native-session success from a TLS/101 result. Dummy-session proxy tests also encountered unclassified native Wine crashes after upgrade, documented in the handoff.
