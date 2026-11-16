"""Log connection metadata only: never request headers, query strings or bodies."""
from pathlib import Path
import json,os,time,ipaddress
LOG=Path(os.environ['NEBULA_MITM_LOG'])
def emit(event,**values):
 with LOG.open('a') as f:f.write(json.dumps({'time':time.time(),'phase':os.environ.get('NEBULA_PROXY_PHASE','native-routing'),'event':event,**values})+'\n')
def tls_clienthello(data):
 sni=data.client_hello.sni
 if sni and data.context.server.address:
  host,port=data.context.server.address
  try:ipaddress.ip_address(host)
  except ValueError:pass
  else:
   data.context.server.address=(sni,port)
   emit('upstream_sni_route',sni=sni)
 emit('client_hello',sni=sni,conn_id=str(data.context.client.id))
def tls_established_client(data):emit('client_tls_ok',sni=data.conn.sni,version=data.conn.tls_version,conn_id=str(data.conn.id))
def tls_failed_client(data):emit('client_tls_failed',sni=data.conn.sni,error=str(data.conn.error),conn_id=str(data.conn.id))
def tls_failed_server(data):emit('upstream_tls_failed',sni=data.conn.sni,error=str(data.conn.error),conn_id=str(data.conn.id))
def http_connect(flow):emit('http_connect',authority=flow.request.authority,conn_id=str(flow.client_conn.id))
def route_category(flow):
 path=flow.request.path.split('?',1)[0].lower()
 return next((kind for kind in ('changelog','leaderboard') if kind in path),'other')
def request(flow):emit('http_request',conn_id=str(flow.client_conn.id),host=flow.request.pretty_host,method=flow.request.method,upgrade=flow.request.headers.get('upgrade',''),route_category=route_category(flow),authorization_present=bool(flow.request.headers.get('authorization')))
def response(flow):emit('http_response',conn_id=str(flow.client_conn.id),host=flow.request.pretty_host,status=flow.response.status_code,route_category=route_category(flow))
def error(flow):emit('flow_error',host=flow.request.pretty_host if flow.request else '',error=str(flow.error))
def websocket_start(flow):emit('websocket_open',host=flow.request.pretty_host)
def websocket_end(flow):emit('websocket_close',host=flow.request.pretty_host)
