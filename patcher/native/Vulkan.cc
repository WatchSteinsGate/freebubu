// JNI bindings for the existing org.jetbrains.skia Vulkan/GaneshInterop classes.
#include <jni.h>
#include <vector>
#include <string>
#include "include/gpu/ganesh/GrDirectContext.h"
#include "include/gpu/ganesh/GrBackendSurface.h"
#include "include/gpu/ganesh/GrBackendSemaphore.h"
#include "include/gpu/ganesh/vk/GrVkBackendSurface.h"
#include "include/gpu/ganesh/vk/GrVkBackendSemaphore.h"
#include "include/gpu/ganesh/vk/GrVkDirectContext.h"
#include "include/gpu/ganesh/vk/GrVkTypes.h"
#include "include/gpu/ganesh/SkSurfaceGanesh.h"
#include "include/gpu/vk/VulkanBackendContext.h"
#include "include/gpu/vk/VulkanExtensions.h"
#include "include/gpu/vk/VulkanMutableTextureState.h"
#include "include/gpu/MutableTextureState.h"
#include "include/core/SkSurface.h"
#include "interop.hh"
namespace skgpu { enum class ThreadSafe : bool { kNo=false,kYes=true }; }
namespace skgpu::VulkanMemoryAllocators {
sk_sp<VulkanMemoryAllocator> Make(const VulkanBackendContext&, ThreadSafe);
}
static std::vector<std::string> strings(JNIEnv* env,jobjectArray array) {
 std::vector<std::string> out;
 if(!array)return out;
 for(jsize i=0;i<env->GetArrayLength(array);++i) {
  auto s=(jstring)env->GetObjectArrayElement(array,i);
  if(!s)continue;
  const char* p=env->GetStringUTFChars(s,nullptr);
  if(p){out.emplace_back(p);env->ReleaseStringUTFChars(s,p);}env->DeleteLocalRef(s);
 }
 return out;
}
extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_VulkanKt__1nMakeVulkan(JNIEnv* env,jclass,
 jlong instance,jlong physical,jlong device,jlong queue,jint family,jlong getInstance,jlong getDevice,
 jint version,jobjectArray instanceNames,jobjectArray deviceNames,jlong features,jlong features2) {
 if(!instance||!physical||!device||!queue||!getInstance||!getDevice)return 0;
 skgpu::VulkanBackendContext c{};
 c.fInstance=(VkInstance)instance;c.fPhysicalDevice=(VkPhysicalDevice)physical;
 c.fDevice=(VkDevice)device;c.fQueue=(VkQueue)queue;c.fGraphicsQueueIndex=family;c.fMaxAPIVersion=version;
 auto ip=(PFN_vkGetInstanceProcAddr)getInstance;auto dp=(PFN_vkGetDeviceProcAddr)getDevice;
 c.fGetProc=[ip,dp](const char* n,VkInstance i,VkDevice d){return d?dp(d,n):ip(i,n);};
 auto in=strings(env,instanceNames),dn=strings(env,deviceNames);if(env->ExceptionCheck())return 0;
 std::vector<const char*> ins,dns;for(auto& s:in)ins.push_back(s.c_str());for(auto& s:dn)dns.push_back(s.c_str());
 skgpu::VulkanExtensions extensions;extensions.init(c.fGetProc,c.fInstance,c.fPhysicalDevice,ins.size(),ins.data(),dns.size(),dns.data());
 c.fVkExtensions=&extensions;c.fDeviceFeatures=(const VkPhysicalDeviceFeatures*)features;c.fDeviceFeatures2=(const VkPhysicalDeviceFeatures2*)features2;
 c.fMemoryAllocator=skgpu::VulkanMemoryAllocators::Make(c,skgpu::ThreadSafe::kYes);
 if(!c.fMemoryAllocator)return 0;
 return (jlong)GrDirectContexts::MakeVulkan(c).release();
}
extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_VulkanKt__1nMakeVulkanImage(JNIEnv*,jclass,jboolean protect,jint width,jint height,jlong image,jint format,jint usage,jint layout,jint family,jint tiling,jint samples,jint levels,jint sharing) {
 if(!image||width<=0||height<=0)return 0;
 GrVkImageInfo i{};i.fImage=(VkImage)image;i.fFormat=(VkFormat)format;i.fImageUsageFlags=usage;i.fImageLayout=(VkImageLayout)layout;i.fCurrentQueueFamily=family;
 i.fImageTiling=(VkImageTiling)tiling;i.fSampleCount=samples;i.fLevelCount=levels;i.fSharingMode=(VkSharingMode)sharing;i.fProtected=protect?skgpu::Protected::kYes:skgpu::Protected::kNo;
 auto t=GrBackendTextures::MakeVk(width,height,i);if(!t.isValid())return 0;return (jlong)new GrBackendTexture(t);
}
static std::vector<GrBackendSemaphore> semaphores(JNIEnv* e,jlongArray a) {
 std::vector<GrBackendSemaphore> out;if(!a)return out;
 jsize n=e->GetArrayLength(a);std::vector<jlong> handles(n);e->GetLongArrayRegion(a,0,n,handles.data());
 for(auto h:handles)out.push_back(GrBackendSemaphores::MakeVk((VkSemaphore)h));return out;
}
extern "C" JNIEXPORT jboolean JNICALL Java_org_jetbrains_skia_VulkanKt__1nWaitVulkan(JNIEnv* e,jclass,jlong context,jlongArray waits) {
 if(!context)return false;auto s=semaphores(e,waits);return ((GrDirectContext*)context)->wait(s.size(),s.data(),false);
}
extern "C" JNIEXPORT jboolean JNICALL Java_org_jetbrains_skia_VulkanKt__1nTransitionVulkanTexture(JNIEnv*,jclass,jlong context,jlong texture,jint layout,jint family) {
 if(!context||!texture)return false;auto state=skgpu::MutableTextureStates::MakeVulkan((VkImageLayout)layout,family);
 return ((GrDirectContext*)context)->setBackendTextureState(*(GrBackendTexture*)texture,state);
}
extern "C" JNIEXPORT jboolean JNICALL Java_org_jetbrains_skia_VulkanKt__1nFlushVulkan(JNIEnv* e,jclass,jlong context,jlong surface,jlongArray signals,jboolean change,jint layout,jint family,jboolean sync) {
 if(!context)return false;auto c=(GrDirectContext*)context;auto s=semaphores(e,signals);GrFlushInfo info{};info.fNumSemaphores=s.size();info.fSignalSemaphores=s.data();
 auto state=skgpu::MutableTextureStates::MakeVulkan((VkImageLayout)layout,family);
 auto result=surface?c->flush((SkSurface*)surface,info,change?&state:nullptr):c->flush(info);
 bool submitted=c->submit(sync?GrSyncCpu::kYes:GrSyncCpu::kNo);return submitted&&(s.empty()||result==GrSemaphoresSubmitted::kYes);
}
extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_GaneshInteropKt__1nWrapBackendTexture(JNIEnv* e,jclass,jlong context,jlong texture,jint origin,jint samples,jint color,jlong space,jintArray props) {
 if(!context||!texture)return 0;auto p=skija::SurfaceProps::toSkSurfaceProps(e,props);
 return (jlong)SkSurfaces::WrapBackendTexture((GrRecordingContext*)context,*(GrBackendTexture*)texture,(GrSurfaceOrigin)origin,samples,(SkColorType)color,sk_ref_sp((SkColorSpace*)space),p.get()).release();
}

#include "include/gpu/ganesh/SkImageGanesh.h"
#include "include/core/SkImage.h"
extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_ImageKt__1nBorrowTextureFrom(JNIEnv*,jclass,jlong context,jlong texture,jint origin,jint color,jint alpha,jlong space) {
 if(!context||!texture)return 0;
 return (jlong)SkImages::BorrowTextureFrom((GrRecordingContext*)context,*(GrBackendTexture*)texture,(GrSurfaceOrigin)origin,(SkColorType)color,(SkAlphaType)alpha,sk_ref_sp((SkColorSpace*)space)).release();
}
