import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.*;
public class StatsPatchTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory(Path.of("build"),"stats-test-");Path file=dir.resolve("account.properties");
  StatsPatch.spOpen(file,"test-owner");long start=Instant.parse("2026-11-16T23:59:59Z").toEpochMilli();
  StatsPatch.spObserve("test-macro",start);StatsPatch.spObserve("test-macro",start+3000);StatsPatch.spGain("profit","test-macro",1.25);StatsPatch.spGain("profit","test-macro",2.5);StatsPatch.spGain("xp","test-macro",10.5);
  var snap=StatsPatch.spSnapshot();check(snap.getTotalProfit()==3.75,"decimal profit");check(snap.getTotalExpGained()==10.5,"XP gain");
  StatsPatch.spObserve(null,start+4000);StatsPatch.spFlush();Properties data=new Properties();try(var in=Files.newInputStream(file)){data.load(in);}String key=Base64.getUrlEncoder().withoutPadding().encodeToString("test-macro".getBytes());
  check(data.getProperty("time."+key).equals("4000"),"elapsed time");check(data.getProperty("starts."+key).equals("1"),"one start per transition");check(!data.containsKey("active.macro"),"session stopped");check(data.getProperty("daily.2026-11-16."+key).equals("1000"),"first day duration");check(data.getProperty("daily.2026-11-17."+key).equals("3000"),"next day duration");
  StatsPatch.spOpen(file,"test-owner");check(StatsPatch.spSnapshot().getTotalProfit()==3.75,"profit persisted across reload");
  StatsPatch.spObserve("test-macro",start+5000);StatsPatch.spFlush();StatsPatch.spOpen(file,"test-owner");StatsPatch.spFlush();
  try(var in=Files.newInputStream(file)){data.clear();data.load(in);}check(!data.containsKey("active.macro"),"interrupted session not resumed");
  check(data.getProperty("starts."+key).equals("2"),"second start persisted");
  StatsPatch.spGain("profit","test-macro",Double.NaN);StatsPatch.spGain("profit","test-macro",-5);check(StatsPatch.spSnapshot().getTotalProfit()==3.75,"invalid gains ignored");
  System.out.println("PASS: decimal profit, XP, elapsed time, start transitions, atomic persistence, reload, interrupted session, invalid gains");
 }
}
