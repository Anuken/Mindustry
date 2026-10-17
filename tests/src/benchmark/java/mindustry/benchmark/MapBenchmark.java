package mindustry.benchmark;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.mock.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.server.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Loads a save on the real headless server and times a fixed number of ticks, as fast as the machine can run them.
 * <p>
 * Every tick runs the whole server update loop (posted tasks, Logic, NetServer, ServerControl) with a fixed 1/60s timestep,
 * so two runs simulate exactly the same game time no matter how fast they are. The JIT is warmed up on the same save first,
 * then the save is reloaded before every measured run so that each run starts from the same state.
 * <p>
 * Run with: gradle tests:benchmark   (-Pbenchmark.map=name -Pbenchmark.ticks=N -Pbenchmark.warmup=N -Pbenchmark.runs=N)
 * Results are printed and written to tests/build/benchmark.
 */
public class MapBenchmark{
    static final String slot = "benchmark";

    static final String map = System.getProperty("benchmark.map", "transcendence");
    static final int
        ticks = Integer.getInteger("benchmark.ticks", 2 * 60 * 60),
        warmupTicks = Integer.getInteger("benchmark.warmup", 60 * 60),
        runs = Integer.getInteger("benchmark.runs", 5);

    /** Counters that are measured inside of another one, shown indented below it. Looked up by name, as they differ between branches. */
    static final String[] counterOrder = {
        "frame", "stateUpdate", "entityUpdate", "entityMisc", "unitPhysicsWait", "unitPhysicsAsync", "unitUpdate", "powerUpdate", "fluidUpdate", "buildingUpdate", "bulletUpdate"
    };
    static final ObjectSet<String> nestedCounters = ObjectSet.with("entityMisc", "unitUpdate", "powerUpdate", "fluidUpdate", "buildingUpdate", "bulletUpdate");
    static final ObjectSet<String> physicsCounters = ObjectSet.with("unitPhysicsWait", "unitPhysicsAsync");

    static Recorder recorder;

    @BeforeAll
    static void boot(){
        ServerHarness.boot();
        ServerHarness.run(() -> {
            //a fixed timestep, even for code that reads the frame time directly (fixed rate entity groups, state.tick)
            Core.graphics = new FixedGraphics((MockGraphics)Core.graphics);
            Time.setDeltaProvider(() -> 1f);
            //run updates back to back instead of at 60 per second
            var interval = HeadlessApplication.class.getDeclaredField("renderInterval");
            interval.setAccessible(true);
            interval.setLong(Core.app, 0L);

            Core.app.addListener(recorder = new Recorder());
        });
    }

    @AfterAll
    static void shutdown(){
        ServerHarness.shutdown();
    }

    @Test
    void benchmark() throws IOException{
        InputStream stream = MapBenchmark.class.getClassLoader().getResourceAsStream(map + "." + saveExtension);
        assertNotNull(stream, "No save named '" + map + "." + saveExtension + "' in the test resources.");
        Fi save = saveDirectory.child(slot + "." + saveExtension);
        try(stream){
            save.write(stream, false);
        }

        String version = gitVersion();
        Log.info("[Benchmark] @ on @: @ warmup ticks, @ runs of @ ticks", map, version, warmupTicks, runs, ticks);

        load();
        String mapName = ServerHarness.call(() -> state.map.plainName());
        Log.info("[Benchmark] Warming up...");
        long warmupStart = Time.millis();
        ServerHarness.ticks(warmupTicks);
        Log.info("[Benchmark] Warmup took @ms.", Time.timeSinceMillis(warmupStart));

        Seq<RunResult> results = new Seq<>();
        for(int i = 0; i < runs; i++){
            load();
            System.gc();

            GcStats gcBefore = GcStats.now();
            ServerHarness.run(() -> recorder.start(ticks));
            //one more tick, as the recorder needs the end of the tick before the first measured one
            ServerHarness.ticks(ticks + 1);
            RunResult result = ServerHarness.call(() -> recorder.finish(GcStats.now().minus(gcBefore), state.isGame() && !state.gameOver && state.map.plainName().equals(mapName)));
            assertTrue(result.valid, "The game ended or changed during the benchmark, so the results would not be comparable.");

            Log.info("[Benchmark] Run @/@: @", i + 1, runs, result.summary());
            results.add(result);
        }

        ServerHarness.command("stop");
        ServerHarness.assertNoErrors();

        String report = report(version, mapName, results);
        Log.info("\n" + report);

        Fi out = new Fi(System.getProperty("benchmark.out.dir", "benchmark")).child(map + "-" + version.replace('/', '_').replace(' ', '_') + ".txt");
        out.writeString(report);
        Log.info("[Benchmark] Written to @", out.absolutePath());
    }

    static void load(){
        if(ServerHarness.call(() -> state.isGame())) ServerHarness.command("stop");
        //loading is posted to the next update
        ServerHarness.command("load " + slot);
        ServerHarness.waitUntil("the save to be loaded", () -> state.isGame(), 120);
        //the benchmark uses no players, and should not depend on server config
        ServerHarness.run(() -> netServer.config.autoPause = false);
    }

    static String report(String version, String mapName, Seq<RunResult> results){
        StringBuilder out = new StringBuilder();
        RunResult median = results.copy().sort(r -> r.totalNs).get(results.size / 2);
        Runtime rt = Runtime.getRuntime();

        out.append("=== Map benchmark: ").append(mapName).append(" (").append(map).append(") ===\n");
        out.append("version: ").append(version).append('\n');
        out.append("machine: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.arch"))
            .append(", ").append(rt.availableProcessors()).append(" cores, java ").append(System.getProperty("java.version"))
            .append(", max heap ").append(rt.maxMemory() / 1024 / 1024).append("MB\n");
        out.append("ticks: ").append(ticks).append(" per run (").append(Strings.fixed(ticks / 60f, 1)).append("s of game time), ")
            .append(runs).append(" runs, ").append(warmupTicks).append(" warmup ticks\n\n");

        out.append("runs (total ms):");
        for(var r : results) out.append(' ').append(Strings.fixed(r.totalNs / 1e6f, 0));
        out.append('\n');
        out.append(String.format(Locale.ROOT, "median run: %.0f ms  (min %.0f, max %.0f)\n\n",
            median.totalNs / 1e6, results.min(r -> r.totalNs).totalNs / 1e6, results.max(r -> r.totalNs).totalNs / 1e6));

        out.append("median run, per tick:\n");
        out.append(String.format(Locale.ROOT, "  mean %.3f ms | p50 %.3f | p95 %.3f | p99 %.3f | max %.3f | %.0f ticks/s\n",
            median.meanMs(), median.percentileMs(0.5), median.percentileMs(0.95), median.percentileMs(0.99), median.percentileMs(1), ticks / (median.totalNs / 1e9)));
        out.append(String.format(Locale.ROOT, "  gc: %d collections, %d ms\n", median.gc.count, median.gc.millis));
        out.append("  end state: ").append(median.endState).append("\n\n");

        out.append("PerfCounter, mean ms per tick (median run; [min - max] over runs):\n");
        for(String name : counterOrder){
            int index = counterIndex(name);
            if(index == -1 || results.allMatch(r -> r.counterNs[index] == 0)) continue;

            String label = (nestedCounters.contains(name) ? "    " : physicsCounters.contains(name) ? "      " : "  ") + name;
            out.append(String.format(Locale.ROOT, "%-22s %8.3f  [%.3f - %.3f]%s\n", label,
                median.counterMs(index), results.min(r -> r.counterNs[index]).counterMs(index), results.max(r -> r.counterNs[index]).counterMs(index),
                name.equals("unitPhysicsAsync") ? "  (on the physics thread)" : ""));
        }
        //anything else that showed up, such as counters added later
        for(PerfCounter counter : PerfCounter.all){
            if(Structs.contains(counterOrder, counter.name()) || results.allMatch(r -> r.counterNs[counter.ordinal()] == 0)) continue;
            out.append(String.format(Locale.ROOT, "  %-20s %8.3f\n", counter.name(), median.counterMs(counter.ordinal())));
        }
        return out.toString();
    }

    static int counterIndex(String name){
        for(PerfCounter counter : PerfCounter.all){
            if(counter.name().equals(name)) return counter.ordinal();
        }
        return -1;
    }

    /** @return the branch and commit this was built from, if it is a git checkout. */
    static String gitVersion(){
        try{
            File root = new File("../..");
            String branch = exec(root, "git", "rev-parse", "--abbrev-ref", "HEAD");
            String commit = exec(root, "git", "rev-parse", "--short", "HEAD");
            boolean dirty = !exec(root, "git", "status", "--porcelain", "--untracked-files=no").isEmpty();
            return branch + "@" + commit + (dirty ? "-dirty" : "");
        }catch(Exception e){
            return "unknown";
        }
    }

    static String exec(File dir, String... command) throws Exception{
        Process process = new ProcessBuilder(command).directory(dir).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        if(process.waitFor() != 0) throw new IOException(output);
        return output;
    }

    /** Takes the time at the end of every tick, and what the PerfCounters measured during it. Only touched on the server thread. */
    static class Recorder implements ApplicationListener{
        long[] stamps;
        long[] counterNs = new long[PerfCounter.all.length];
        int recorded;

        void start(int ticks){
            stamps = new long[ticks + 1];
            Arrays.fill(counterNs, 0);
            recorded = 0;
        }

        @Override
        public void update(){
            if(stamps == null || recorded >= stamps.length) return;

            stamps[recorded] = Time.nanos();
            //the first stamp only marks where the first measured tick starts
            if(recorded > 0){
                for(PerfCounter counter : PerfCounter.all){
                    //counters that did not run this tick would otherwise repeat their last value
                    counter.checkUpdate();
                    counterNs[counter.ordinal()] += counter.latestValueNs();
                }
            }
            recorded++;
        }

        RunResult finish(GcStats gc, boolean valid){
            assertEquals(stamps.length, recorded, "Not every tick was recorded.");
            long[] durations = new long[stamps.length - 1];
            for(int i = 0; i < durations.length; i++){
                durations[i] = stamps[i + 1] - stamps[i];
            }
            stamps = null;

            String endState = String.format(Locale.ROOT, "tick %.0f, wave %d, %d buildings, %d units, %d bullets",
                state.tick, state.wave, state.entities.build.size(), state.entities.unit.size(), state.entities.bullet.size());
            return new RunResult(durations, counterNs.clone(), gc, endState, valid);
        }
    }

    static class RunResult{
        final long[] sortedDurations;
        final long totalNs;
        final long[] counterNs;
        final GcStats gc;
        final String endState;
        final boolean valid;

        RunResult(long[] durations, long[] counterNs, GcStats gc, String endState, boolean valid){
            this.sortedDurations = durations.clone();
            Arrays.sort(sortedDurations);
            this.totalNs = Arrays.stream(durations).sum();
            this.counterNs = counterNs;
            this.gc = gc;
            this.endState = endState;
            this.valid = valid;
        }

        double meanMs(){
            return totalNs / 1e6 / sortedDurations.length;
        }

        double percentileMs(double fraction){
            return sortedDurations[Math.min((int)(fraction * sortedDurations.length), sortedDurations.length - 1)] / 1e6;
        }

        double counterMs(int index){
            return counterNs[index] / 1e6 / sortedDurations.length;
        }

        String summary(){
            return String.format(Locale.ROOT, "%.0f ms, %.3f ms/tick, p99 %.3f ms, gc %d ms | %s", totalNs / 1e6, meanMs(), percentileMs(0.99), gc.millis, endState);
        }
    }

    record GcStats(long count, long millis){
        static GcStats now(){
            long count = 0, millis = 0;
            for(GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()){
                count += Math.max(bean.getCollectionCount(), 0);
                millis += Math.max(bean.getCollectionTime(), 0);
            }
            return new GcStats(count, millis);
        }

        GcStats minus(GcStats other){
            return new GcStats(count - other.count, millis - other.millis);
        }
    }

    /** Reports a constant 1/60s frame time, but otherwise behaves like the headless application's own graphics. */
    static class FixedGraphics extends MockGraphics{
        final MockGraphics base;

        FixedGraphics(MockGraphics base){
            this.base = base;
        }

        @Override
        public long getFrameId(){
            return base.getFrameId();
        }

        @Override
        public float getDeltaTime(){
            return 1f / 60f;
        }

        @Override
        public int getFramesPerSecond(){
            return base.getFramesPerSecond();
        }
    }
}
