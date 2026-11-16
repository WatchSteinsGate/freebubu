#include <windows.h>
#include <jni.h>
#include <jvmti.h>
#include <stdio.h>
static jclass (JNICALL *original)(JNIEnv*,const char*);
static jclass JNICALL lookup(JNIEnv* e,const char* name){
 if(name&&strstr(name,"loader")){HMODULE mod=GetModuleHandleA("client.dll");fprintf(stderr,"[LoaderTrace] %s caller=%p relative=%llx\n",name,__builtin_return_address(0),(unsigned long long)((char*)__builtin_return_address(0)-(char*)mod));fflush(stderr);}
 return original(e,name);
}
static void JNICALL ready(jvmtiEnv* t,JNIEnv* e,jthread thread){
 jniNativeInterface* table=0;jvmtiError err=(*t)->GetJNIFunctionTable(t,&table);if(err){fprintf(stderr,"[LoaderTrace] table error=%d\n",err);return;}original=table->FindClass;table->FindClass=lookup;err=(*t)->SetJNIFunctionTable(t,table);fprintf(stderr,"[LoaderTrace] installed=%d\n",err);(*t)->Deallocate(t,(unsigned char*)table);
}
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm,char* options,void* reserved){jvmtiEnv* t=0;if((*vm)->GetEnv(vm,(void**)&t,JVMTI_VERSION_1_2))return JNI_ERR;jvmtiEventCallbacks cb={0};cb.VMInit=ready;(*t)->SetEventCallbacks(t,&cb,sizeof cb);(*t)->SetEventNotificationMode(t,JVMTI_ENABLE,JVMTI_EVENT_VM_INIT,0);return JNI_OK;}
