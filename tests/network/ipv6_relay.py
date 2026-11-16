import socket,socketserver,select
class Relay(socketserver.BaseRequestHandler):
 def handle(self):
  with socket.create_connection(('127.0.0.1',8080)) as upstream:
   peers=[self.request,upstream]
   while True:
    for p in select.select(peers,[],[],60)[0]:
     data=p.recv(65536)
     if not data:return
     (upstream if p is self.request else self.request).sendall(data)
class Server(socketserver.ThreadingTCPServer):address_family=socket.AF_INET6;allow_reuse_address=True;daemon_threads=True
Server(('::1',8080),Relay).serve_forever()
