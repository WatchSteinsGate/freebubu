import json,subprocess,os,pathlib,shlex
R=pathlib.Path('/workspace/minecraft');cp=json.load(open(R/'classpath.json'))
def win(p):return 'Z:'+str(p).replace('/','\\')
cmd=['/workspace/tools/runtime/usr/lib/wine/wine64','/workspace/tools/windows-java/jdk-25.0.5+8/bin/java.exe','-Xmx3G',*shlex.split(os.environ.get('NEBULA_DEV_JVM_ARGS','')),'-Dfabric.gameVersion=26.2','-cp',';'.join(win(p) for p in cp),'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','LocalTester','--version','26.2-fabric','--gameDir',win(R),'--assetsDir',win(R/'assets'),'--assetIndex',json.load(open('/workspace/tools/minecraft-26.2.json'))['assetIndex']['id'],'--uuid','00000000000000000000000000000000','--accessToken','0','--userType','msa','--versionType','release','--width','1280','--height','800']
if os.environ.get('NEBULA_TEST_AGENT'):
 cmd.insert(2,'-javaagent:'+win(os.environ['NEBULA_TEST_AGENT']))
raise SystemExit(subprocess.run(cmd,cwd=R).returncode)
