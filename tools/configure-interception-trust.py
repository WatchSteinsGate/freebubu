#!/usr/bin/env python3
"""Create separate Java/native proxy trust files, retaining existing trusted roots."""
import argparse,hashlib,json,os,pathlib,shutil,ssl,subprocess
from cryptography import x509
from cryptography.hazmat.primitives import serialization
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--ca',type=pathlib.Path,required=True,help='Proxy CA certificate (PEM)')
p.add_argument('--java-cacerts',type=pathlib.Path,required=True,help='Original JDK cacerts file')
p.add_argument('--out',type=pathlib.Path,required=True)
p.add_argument('--base-ca-bundle',type=pathlib.Path,help='Defaults to inherited SSL_CERT_FILE or system roots')
p.add_argument('--keytool',default='keytool')
a=p.parse_args();ca=x509.load_pem_x509_certificate(a.ca.read_bytes())
if not ca.extensions.get_extension_for_class(x509.BasicConstraints).value.ca:raise SystemExit('The supplied certificate is not a CA')
a.out.mkdir(parents=True,exist_ok=True);native=(a.out/'native-ca-bundle.pem').resolve();java=(a.out/'java-cacerts').resolve()
base=a.base_ca_bundle or (pathlib.Path(os.environ['SSL_CERT_FILE']) if os.environ.get('SSL_CERT_FILE') else None)
if java==a.java_cacerts.resolve() or (base and native==base.resolve()):raise SystemExit('Output must differ from the original trust files')
if base:
 raw=base.read_bytes();roots=x509.load_pem_x509_certificates(raw)
else:
 roots=[x509.load_der_x509_certificate(c) for c in ssl.create_default_context().get_ca_certs(binary_form=True)]
if ca not in roots:roots.append(ca)
# Serialize certificates only; never copy a CA private key into a trust bundle.
native.write_bytes(b''.join(c.public_bytes(serialization.Encoding.PEM) for c in roots))
shutil.copyfile(a.java_cacerts,java);fingerprint=hashlib.sha256(ca.public_bytes(serialization.Encoding.DER)).hexdigest();public_ca=a.out/'proxy-ca-cert.pem';public_ca.write_bytes(ca.public_bytes(serialization.Encoding.PEM))
r=subprocess.run([a.keytool,'-importcert','-noprompt','-alias','nebula-proxy-'+fingerprint[:16],'-file',str(public_ca),'-keystore',str(java),'-storepass','changeit'],capture_output=True,text=True)
if r.returncode:raise SystemExit(r.stderr or r.stdout)
result={'native_ssl_cert_file':str(native),'java_trust_store':str(java),'native_root_count':len(roots),'proxy_ca_sha256':fingerprint,'original_files_unchanged':True}
(a.out/'configuration.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
