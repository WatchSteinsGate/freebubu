#define _GNU_SOURCE
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
/* External Wine test tooling. Forwarding goes through the local MITM, whose
 * upstream remains the inherited Cloud proxy. This library is not in the JAR. */
static __thread int logging;
static void record(const char *event,const char *host,int port,int result,int err){
 if(logging)return;
 logging=1;
 const char *path=getenv("NEBULA_NETWORK_LOG");
 if(path){
  int fd=open(path,O_WRONLY|O_CREAT|O_APPEND,0600);
  if(fd>=0){
   char b[768];int n=snprintf(b,sizeof b,"{\"pid\":%d,\"event\":\"%s\",\"host\":\"%.255s\",\"port\":%d,\"result\":%d,\"errno\":%d}\n",getpid(),event,host?host:"",port,result,err);
   write(fd,b,n<(int)sizeof b?n:sizeof b-1);close(fd);
  }
 }
 logging=0;
}
int getaddrinfo(const char *node,const char *service,const struct addrinfo *hints,struct addrinfo **result){
 static int(*real)(const char*,const char*,const struct addrinfo*,struct addrinfo**);
 if(!real)real=dlsym(RTLD_NEXT,"getaddrinfo");
 int ret=real(node,service,hints,result);record("getaddrinfo",node,service?atoi(service):0,ret,0);return ret;
}
static int tunnel(int fd,const struct sockaddr *addr,const char *host,int port,int(*real)(int,const struct sockaddr*,socklen_t)){
 int oldflags=fcntl(fd,F_GETFL);if(oldflags<0)return -1;
 if(fcntl(fd,F_SETFL,oldflags&~O_NONBLOCK)<0)return -1;
 int ret;
 if(addr->sa_family==AF_INET){
  struct sockaddr_in proxy={.sin_family=AF_INET,.sin_port=htons(8080)};inet_pton(AF_INET,"127.0.0.1",&proxy.sin_addr);
  ret=real(fd,(void*)&proxy,sizeof proxy);
 }else{
  struct sockaddr_in6 proxy={.sin6_family=AF_INET6,.sin6_port=htons(8080)};inet_pton(AF_INET6,"::1",&proxy.sin6_addr);
  ret=real(fd,(void*)&proxy,sizeof proxy);
 }
 if(ret<0)goto done;
 char request[512];
 if(addr->sa_family==AF_INET6)snprintf(request,sizeof request,"CONNECT [%s]:%d HTTP/1.1\r\nHost: [%s]:%d\r\n\r\n",host,port,host,port);
 else snprintf(request,sizeof request,"CONNECT %s:%d HTTP/1.1\r\nHost: %s:%d\r\n\r\n",host,port,host,port);
 size_t sent=0,total=strlen(request);
 while(sent<total){ssize_t n=send(fd,request+sent,total-sent,MSG_NOSIGNAL);if(n<=0){ret=-1;goto done;}sent+=n;}
 char reply[8192];size_t n=0;
 while(n<sizeof(reply)-1){
  struct pollfd ready={.fd=fd,.events=POLLIN};int p=poll(&ready,1,10000);
  if(p<=0){errno=ETIMEDOUT;ret=-1;goto done;}
  if(recv(fd,reply+n,1,0)!=1){errno=ECONNRESET;ret=-1;goto done;}n++;reply[n]=0;
  if(n>=4&&!memcmp(reply+n-4,"\r\n\r\n",4))break;
 }
 if(n<12||strncmp(reply,"HTTP/1.",7)||memcmp(reply+9,"200",3)){errno=ECONNREFUSED;ret=-1;}
 done:;
 int saved=errno;fcntl(fd,F_SETFL,oldflags);errno=saved;return ret;
}
int connect(int fd,const struct sockaddr *addr,socklen_t len){
 static int(*real)(int,const struct sockaddr*,socklen_t);
 if(!real)real=dlsym(RTLD_NEXT,"connect");
 char host[INET6_ADDRSTRLEN]="";int port=0;
 if(addr&&addr->sa_family==AF_INET){const struct sockaddr_in *a=(void*)addr;inet_ntop(AF_INET,&a->sin_addr,host,sizeof host);port=ntohs(a->sin_port);}
 else if(addr&&addr->sa_family==AF_INET6){const struct sockaddr_in6 *a=(void*)addr;inet_ntop(AF_INET6,&a->sin6_addr,host,sizeof host);port=ntohs(a->sin6_port);}
 int type=0;socklen_t size=sizeof type;const char *enable=getenv("NEBULA_REDIRECT_ENABLE_FILE");
 if(port==443&&enable&&access(enable,F_OK)==0&&getsockopt(fd,SOL_SOCKET,SO_TYPE,&type,&size)==0&&type==SOCK_STREAM){
  int ret=tunnel(fd,addr,host,port,real);int saved=errno;record("native_redirect",host,port,ret,saved);errno=saved;return ret;
 }
 int ret=real(fd,addr,len);int saved=errno;if(*host)record("connect",host,port,ret,saved);errno=saved;return ret;
}
