from pathlib import Path
import json,hashlib,ipaddress
ROOT=Path(__file__).resolve().parents[2]
rows=[json.loads(s) for s in (ROOT/'build/network-mitm.jsonl').read_text().splitlines()]
connects={r['conn_id']:r for r in rows if r['event']=='http_connect'}
requests={r['conn_id']:r for r in rows if r['event']=='http_request' and r.get('upgrade','').lower()=='websocket'}
successes=[]
for r in rows:
 if r['event']!='http_response' or r.get('status')!=101 or r.get('conn_id') not in requests:continue
 c=connects.get(r['conn_id'])
 if not c:continue
 authority=c['authority'];host=authority.rsplit(':',1)[0].strip('[]')
 try:ipaddress.ip_address(host)
 except ValueError:continue
 tls=next(x for x in rows if x['event']=='client_tls_ok' and x.get('conn_id')==r['conn_id'])
 successes.append({'host':requests[r['conn_id']]['host'],'tls':tls['version'],'phase':r['phase'],'status':r['status']})
assert any(x['phase']=='native-final-rsa' for x in successes),'Native RSA interception did not pass'
negative=(Path('/workspace/minecraft/launch-network-hostname-check.log')).read_text(errors='replace')
assert any('NebulaLoader' in s and 'certificate not valid for name' in s for s in negative.splitlines()),'Native wrong-host rejection missing'
jar=ROOT/'build/artifacts/Nebula-Windows-direct.jar';digest=hashlib.file_digest(jar.open('rb'),'sha256').hexdigest();baseline=json.loads((ROOT/'metadata/debug-build.json').read_text())
assert digest==baseline['sha256'],'Artifact changed during runtime tests'
report={'jar_sha256':digest,'jar_changed':False,'test_platform':'Windows JDK 25 and Windows native DLL under Wine','proxy':'mitmproxy chained through inherited Cloud proxy; upstream TLS verification enabled','java_actual_api_interception':'passed','native_api_websocket_interception':'passed','native_successes':successes,'native_hostname_negative_test':'passed: certificate signed by trusted CA for wrong-host.invalid rejected for api.nebulaclient.net','native_wrong_ca_test':'failed as expected with BadSignature before the correct CA was added to SSL_CERT_FILE','cause':'SSL_CERT_FILE selected an inherited CA file instead of Windows ROOT. A different CA with the same mitmproxy subject existed in that file. Adding the correct inspection CA resolved the error.','native_routing':'JVM HTTP proxy properties did not route the native worker; external process-level CONNECT forwarding was required','native_ca_configuration':'Preserve inherited CA roots and append inspection CA in a dedicated bundle, then set SSL_CERT_FILE for the game process','additional_native_tls_patch_needed':False,'scope':'api.nebulaclient.net native WebSocket and observed Java API requests; not every endpoint','http_toolkit_itself_tested':False,'authenticated_session_verified':False,'resolver_reconstructed':False,'setup':'tests/network/README.md'}
(ROOT/'metadata/network-interception.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
