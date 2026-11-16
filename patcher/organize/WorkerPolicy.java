package thatonecatgif;
/** Optional Wine recovery hooks must not prevent a debugger from starting. */
public final class WorkerPolicy {
 private static native void install();
 public static void installIfEnabled(){
  if(!Boolean.parseBoolean(System.getProperty("nebula.workerRecovery","true")))return;
  boolean debugger=java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(a->a.startsWith("-agentlib:jdwp")||a.startsWith("-Xrunjdwp"));
  if(debugger){System.out.println("[Nebula] JDWP enabled; Wine worker recovery hooks skipped");return;}
  install();
 }
}
