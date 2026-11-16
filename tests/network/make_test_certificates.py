"""Create two inspection-test leaf certificates under a local test proxy CA."""
import argparse,pathlib
from datetime import datetime,timedelta,timezone
from cryptography import x509
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID,ExtendedKeyUsageOID
p=argparse.ArgumentParser();p.add_argument('--ca-with-key',type=pathlib.Path,required=True);p.add_argument('--out',type=pathlib.Path,required=True);p.add_argument('--host',default='api.nebulaclient.net');a=p.parse_args();pem=a.ca_with_key.read_bytes();ca=x509.load_pem_x509_certificate(pem);ca_key=serialization.load_pem_private_key(pem,None);a.out.mkdir(parents=True,exist_ok=True);now=datetime.now(timezone.utc)
for name,host in [('ecdsa',a.host),('wrong-host','wrong-host.invalid')]:
 key=ec.generate_private_key(ec.SECP256R1())
 cert=x509.CertificateBuilder().subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,host)])).issuer_name(ca.subject).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-timedelta(hours=1)).not_valid_after(now+timedelta(days=1)).add_extension(x509.SubjectAlternativeName([x509.DNSName(host)]),critical=False).add_extension(x509.BasicConstraints(ca=False,path_length=None),critical=True).add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]),critical=False).sign(ca_key,hashes.SHA256())
 (a.out/(name+'.pem')).write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption())+cert.public_bytes(serialization.Encoding.PEM)+ca.public_bytes(serialization.Encoding.PEM))
