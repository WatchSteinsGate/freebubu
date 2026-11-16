"""Run a loopback MITM through the inherited Cloud proxy and CA trust."""
import os,pathlib,subprocess,urllib.parse
root=pathlib.Path(__file__).resolve().parents[2];conf=root/'.cache/interception/mitm';conf.mkdir(parents=True,exist_ok=True)
proxy=os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY');assert proxy,'Inherited upstream proxy is required'
u=urllib.parse.urlsplit(proxy);safe_proxy=urllib.parse.urlunsplit((u.scheme,f'{u.hostname}:{u.port or 80}',u.path,u.query,u.fragment));ca=os.environ.get('SSL_CERT_FILE') or os.environ.get('REQUESTS_CA_BUNDLE') or '/etc/ssl/certs/ca-certificates.crt'
args=[str(root/'.cache/interception-tools/bin/mitmdump'),'-q','--listen-host','127.0.0.1','--listen-port','8080','--mode','upstream:'+safe_proxy,'--set','confdir='+str(conf),'--set','ssl_verify_upstream_trusted_ca='+ca,'--set','connection_strategy=lazy','--set','upstream_cert=false','-s',str(root/'tests/network/observe_mitm.py')]
if os.environ.get('NEBULA_TLS_CLIENT_MAX'):args+=['--set','tls_version_client_max='+os.environ['NEBULA_TLS_CLIENT_MAX']]
if os.environ.get('NEBULA_MITM_CERT'):args+=['--certs','*='+os.environ['NEBULA_MITM_CERT']]
if u.username:args+=['--upstream-auth',urllib.parse.unquote(u.username)+':'+urllib.parse.unquote(u.password or '')]
os.environ['NEBULA_MITM_LOG']=str(root/'build/network-mitm.jsonl')
raise SystemExit(subprocess.run(args).returncode)
