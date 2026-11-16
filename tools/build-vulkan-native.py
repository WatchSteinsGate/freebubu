#!/usr/bin/env python3
"""Cross-build the existing Skiko DLL with Vulkan JNI, using matching Skia archives."""
import concurrent.futures,json,os,pathlib,subprocess,sys,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
skia=pathlib.Path(os.environ.get('NEBULA_SKIA_ROOT',root/'.cache/vulkan-crash/skia-windows'))
skiko=pathlib.Path(os.environ.get('NEBULA_SKIKO_SOURCE',root/'.cache/vulkan-crash/source/skiko-0.153.0/skiko'))
sdk=pathlib.Path(os.environ.get('NEBULA_WINDOWS_SDK','/workspace/tools/windows-sdk'))
tool=pathlib.Path(os.environ.get('NEBULA_NATIVE_TOOLS','/workspace/tools/native-build/usr/bin'))
out=root/'build/vulkan-native';out.mkdir(parents=True,exist_ok=True)
env=os.environ.copy();env['LD_LIBRARY_PATH']=os.environ.get('NEBULA_NATIVE_TOOL_LIBS','/workspace/tools/native-build/usr/lib/x86_64-linux-gnu')

# LLVM's overlay supplies Windows' case-insensitive header lookup on Linux.
def overlay_entry(d):
 return {'type':'directory','name':d.name,'contents':[overlay_entry(f) if f.is_dir() else {'type':'file','name':f.name,'external-contents':str(f)} for f in d.iterdir() if not f.is_symlink()]}
tree=overlay_entry(sdk);tree['name']=str(sdk)
(out/'sdk-overlay.json').write_text(json.dumps({'version':0,'case-sensitive':'false','roots':[tree]}))
# Root every original export so linking does not discard API used by other native code.
sys.path.insert(0,os.environ.get('NEBULA_PEFILE_PATH',str(root/'.cache/native-tools')))
import pefile
with zipfile.ZipFile(os.environ.get('NEBULA_BASE_JAR',str(root/'.cache/Nebula-Windows-original.jar'))) as original:
 native=pefile.PE(data=original.read('skiko-windows-x64.dll'))
names=[s.name.decode() for s in native.DIRECTORY_ENTRY_EXPORT.symbols if s.name and not s.name.startswith(b'Java_')]
(out/'original-exports.def').write_text('EXPORTS\n'+'\n'.join(names)+'\n')

inc=[skia]+[skia/x for x in ['include/third_party/vulkan','include','include/core','include/gpu','include/effects','include/pathops','include/utils','include/codec','include/svg','modules/jsonreader','modules/skottie/include','modules/skparagraph/include','modules/skshaper/include','modules/skunicode/include','modules/sksg/include','modules/svg/include','third_party/externals/harfbuzz/src','third_party/icu','third_party/externals/icu/source/common','third_party/externals/angle2/include','src/gpu']]
inc +=[skiko/x for x in ['src/awtMain/cpp/include','src/jvmMain/cpp/common','src/jvmMain/cpp/include','src/commonMain/cpp/common/include']]
inc +=[pathlib.Path(os.environ.get('NEBULA_WINDOWS_JDK','/workspace/tools/windows-java/jdk-25.0.5+8'))/'include'/x for x in ['.','win32']]
inc +=[sdk/x for x in ['crt/include','sdk/include/ucrt','sdk/include/um','sdk/include/shared','sdk/include/winrt']]
defs='NDEBUG SK_ALLOW_STATIC_GLOBAL_INITIALIZERS=1 SK_FORCE_DISTANCE_FIELD_TEXT=0 SK_GAMMA_APPLY_TO_A8 SK_GAMMA_SRGB SK_SCALAR_TO_FLOAT_EXCLUDED SK_SUPPORT_GPU=1 SK_GANESH SK_GL SK_VULKAN SK_SHAPER_HARFBUZZ_AVAILABLE SK_UNICODE_AVAILABLE SK_SHAPER_UNICODE_AVAILABLE SK_SUPPORT_OPENCL=0 SK_USING_THIRD_PARTY_ICU U_DISABLE_RENAMING=0 U_DISABLE_VERSION_SUFFIX=1 U_HAVE_LIB_SUFFIX=1 U_LIB_SUFFIX_C_NAME=_skiko SK_BUILD_FOR_WIN _CRT_SECURE_NO_WARNINGS _HAS_EXCEPTIONS=0 WIN32_LEAN_AND_MEAN NOMINMAX SK_DIRECT3D SK_ANGLE'.split()
flags=['--target=x86_64-pc-windows-msvc','-c','-O2','-std=c++20','-ivfsoverlay',str(out/'sdk-overlay.json'),'-fms-runtime-lib=static','-Wno-ignored-attributes','-Wno-nonportable-include-path','-Wno-ignored-pragma-intrinsic','-fno-rtti','-fno-exceptions','-D_MT','-fms-compatibility-version=19.44']+['-D'+d for d in defs]+['-I'+str(p) for p in inc]
sources=[]
for rel in ['src/commonMain/cpp/common','src/jvmMain/cpp/common','src/awtMain/cpp/common','src/awtMain/cpp/windows']:
 sources+=list((skiko/rel).rglob('*.cc'))+list((skiko/rel).rglob('*.cpp'))
sources+=[root/'patcher/native/Vulkan.cc']
def compile(p):
 obj=out/('_'.join(p.relative_to(skiko).parts) if p.is_relative_to(skiko) else p.name);obj=obj.with_suffix('.obj')
 if obj.exists() and obj.stat().st_mtime>p.stat().st_mtime:return obj
 r=subprocess.run([str(tool/'clang++-19'),*flags,str(p),'-o',str(obj)],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 if r.returncode:
  (out/(obj.name+'.log')).write_bytes(r.stdout);raise RuntimeError(str(p)+'\n'+r.stdout.decode(errors='replace')[:2500])
 return obj
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:objs=list(pool.map(compile,sources))
print('Compiled',len(objs),flush=True)
libs=skia/'out/Release-windows-x64'
# Preserve the original DLL's C++ exports, also used by the pre-existing bridge.
if '--compile-only' in sys.argv:sys.exit()
cmd=[str(tool/'lld-link-19'),'/DLL','/NOLOGO','/OPT:REF','/OPT:ICF','/ignore:4217','/DEF:'+str(out/'original-exports.def'),'/OUT:'+str(out/'skiko-windows-x64.dll'),'/IMPLIB:'+str(out/'skiko.lib'),*map(str,objs),'/LIBPATH:'+str(libs)]
cmd+=['/LIBPATH:'+str(sdk/p) for p in ['crt/lib/x86_64','sdk/lib/um/x86_64','sdk/lib/ucrt/x86_64']]
cmd +=[x+'.lib' for x in ['skia_ganesh_ext','skia','svg','skparagraph','skshaper','skunicode_core','skunicode_icu','icu','harfbuzz','skresources','libpng','libjpeg','libwebp','libwebp_sse41','zlib','expat','d3d12allocator','raw_ptr','allocator_core','allocator_base','Advapi32','gdi32','Dwmapi','ole32','Propsys','shcore','Shlwapi','user32','winmm','kernel32','libcmt','libvcruntime','libucrt','oldnames']]

# Windows library names are case-insensitive; resolve them on the cross-build host.
search=[libs]+[sdk/p for p in ['crt/lib/x86_64','sdk/lib/um/x86_64','sdk/lib/ucrt/x86_64']]
index={f.name.lower():str(f) for d in search for f in d.iterdir() if f.is_file()}
cmd=[index.get(x.lower(),x) if x.endswith('.lib') and '/' not in x else x for x in cmd]
r=subprocess.run(cmd,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(out/'link.log').write_bytes(r.stdout)
if r.returncode:sys.exit(r.stdout.decode(errors='replace')[:7000])
print(out/'skiko-windows-x64.dll')
