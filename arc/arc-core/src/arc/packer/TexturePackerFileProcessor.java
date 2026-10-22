package arc.packer;

import arc.files.*;
import arc.packer.TexturePacker.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** @author Nathan Sweet */
public class TexturePackerFileProcessor extends FileProcessor{
    private final Settings defaultSettings;
    private ObjectMap<File, Settings> dirToSettings = new ObjectMap<>();
    private Json json = new Json();
    private String packFileName;
    private File root;
    Seq<File> ignoreDirs = new Seq<>();
    boolean countOnly;
    int packCount;
    /** Runs the packing of directories in the background. Only exists while the actual processing pass is running. */
    private PackQueue queue;

    public TexturePackerFileProcessor(){
        this(new Settings(), "pack.aatls");
    }

    public TexturePackerFileProcessor(Settings defaultSettings, String packFileName){
        this.defaultSettings = defaultSettings;

        if(packFileName.toLowerCase().endsWith(defaultSettings.atlasExtension.toLowerCase()))
            packFileName = packFileName.substring(0, packFileName.length() - defaultSettings.atlasExtension.length());
        this.packFileName = packFileName;

        setFlattenOutput(true);
        addInputSuffix(".png", ".jpg", ".jpeg");

        // Sort input files by name to avoid platform-dependent atlas output changes.
        setComparator(Structs.comparing(File::getName));
    }

    @Override
    public Seq<Entry> process(File inputFile, File outputRoot) throws Exception{
        root = inputFile;

        // Collect pack.json setting files.
        final Seq<File> settingsFiles = new Seq<>();
        FileProcessor settingsProcessor = new FileProcessor(){
            @Override
            protected void processFile(Entry inputFile){
                settingsFiles.add(inputFile.inputFile);
            }
        };
        settingsProcessor.addInputRegex("pack\\.h?json");
        settingsProcessor.process(inputFile, null);
        // Sort parent first.
        settingsFiles.sort(Structs.comparingInt(file -> file.toString().length()));
        for(File settingsFile : settingsFiles){
            // Find first parent with settings, or use defaults.
            Settings settings = null;
            File parent = settingsFile.getParentFile();
            while(true){
                if(parent.equals(root)) break;
                parent = parent.getParentFile();
                settings = dirToSettings.get(parent);
                if(settings != null){
                    settings = settings.copy();
                    break;
                }
            }
            if(settings == null) settings = defaultSettings.copy();
            // Merge settings from current directory.
            merge(settings, settingsFile);
            dirToSettings.put(settingsFile.getParentFile(), settings);
        }

        // Count the number of texture packer invocations.
        countOnly = true;
        super.process(inputFile, outputRoot);
        countOnly = false;

        // Do actual processing.
        return super.process(inputFile, outputRoot);
    }

    void merge(Settings settings, File settingsFile){
        try{
            json.readFields(settings, Jval.read(new Fi(settingsFile)));
        }catch(Exception ex){
            throw new ArcRuntimeException("Error reading settings file: " + settingsFile, ex);
        }
    }

    @Override
    public Seq<Entry> process(File[] files, File outputRoot) throws Exception{
        // Delete pack file and images.
        if(countOnly && outputRoot.exists()) deleteOutput(outputRoot);
        if(countOnly) return super.process(files, outputRoot);

        //directories are packed in the background as they are found, and the results are written out in order once they're ready
        PackQueue queue = this.queue = new PackQueue();
        try{
            Seq<Entry> result = super.process(files, outputRoot);
            queue.finish();
            return result;
        }finally{
            this.queue = null;
            queue.close();
        }
    }

    protected void deleteOutput(File outputRoot) throws Exception{
        // Load root settings to get scale.
        File settingsFile = new File(root, "pack.hjson");
        //use JSON as fallback
        if(!settingsFile.exists()) settingsFile = new File(root, "pack.json");
        Settings rootSettings = defaultSettings;
        if(settingsFile.exists()){
            rootSettings = rootSettings.copy();
            merge(rootSettings, settingsFile);
        }

        String atlasExtension = rootSettings.atlasExtension == null ? "" : rootSettings.atlasExtension;
        atlasExtension = Pattern.quote(atlasExtension);

        for(int i = 0, n = rootSettings.scale.length; i < n; i++){
            FileProcessor deleteProcessor = new FileProcessor(){
                @Override
                protected void processFile(Entry inputFile) throws Exception{
                    inputFile.inputFile.delete();
                }
            };
            deleteProcessor.setRecursive(false);

            File packFile = new File(rootSettings.getScaledPackFileName(packFileName, i));

            String prefix = packFile.getName();
            int dotIndex = prefix.lastIndexOf('.');
            if(dotIndex != -1) prefix = prefix.substring(0, dotIndex);
            deleteProcessor.addInputRegex("(?i)" + prefix + "\\d*\\.(png|jpg|jpeg)");
            deleteProcessor.addInputRegex("(?i)" + prefix + atlasExtension);

            String dir = packFile.getParent();
            if(dir == null)
                deleteProcessor.process(outputRoot, null);
            else if(new File(outputRoot + "/" + dir).exists()) //
                deleteProcessor.process(outputRoot + "/" + dir, null);
        }
    }

    @Override
    protected void processDir(final Entry inputDir, Seq<Entry> files) throws Exception{
        if(ignoreDirs.contains(inputDir.inputFile)) return;

        // Find first parent with settings, or use defaults.
        Settings settings = null;
        File parent = inputDir.inputFile;
        while(true){
            settings = dirToSettings.get(parent);
            if(settings != null) break;
            if(parent == null || parent.equals(root)) break;
            parent = parent.getParentFile();
        }
        if(settings == null) settings = defaultSettings;

        if(settings.ignore) return;

        if(settings.combineSubdirectories){
            // Collect all files under subdirectories and ignore subdirectories without pack.json files.
            files = new FileProcessor(this){
                @Override
                protected void processDir(Entry entryDir, Seq<Entry> files){
                    if(!entryDir.inputFile.equals(inputDir.inputFile) && (new File(entryDir.inputFile, "pack.json").exists() || new File(entryDir.inputFile, "pack.hjson").exists())){
                        files.clear();
                        return;
                    }
                    if(!countOnly) ignoreDirs.add(entryDir.inputFile);
                }

                @Override
                protected void processFile(Entry entry){
                    addProcessedFile(entry);
                }
            }.process(inputDir.inputFile, null);
        }

        if(files.isEmpty()) return;

        if(countOnly){
            packCount++;
            return;
        }

        final Pattern digitSuffix = Pattern.compile("(.*?)(\\d+)$");

        // Sort by name using numeric suffix, then alpha.
        // The name and number are worked out once per file, rather than on every comparison.
        Seq<SortKey> keys = new Seq<>(files.size);
        for(Entry entry : files){
            keys.add(new SortKey(entry, digitSuffix));
        }
        keys.sort((key1, key2) -> {
            int compare = key1.name.compareTo(key2.name);
            if(compare != 0 || key1.number == key2.number) return compare;
            return key1.number - key2.number;
        });
        files.clear();
        for(SortKey key : keys){
            files.add(key.entry);
        }

        // Pack.
        TexturePacker packer = new TexturePacker(root, settings);
        //messages are held back until it's this directory's turn, so output from directories packed at the same time stays readable
        packer.bufferLog();
        if(!settings.silent){
            try{
                packer.log().println(inputDir.inputFile.getCanonicalPath());
            }catch(IOException ignored){
                packer.log().println(inputDir.inputFile.getAbsolutePath());
            }
        }

        for(Entry file : files){
            packer.addImage(file.inputFile);
        }

        queue.add(inputDir, packer, inputDir.outputDir, packFileName);
    }

    /** Sort key for an input file, see {@link #processDir(Entry, Seq)}. */
    private static class SortKey{
        final Entry entry;
        final String name;
        int number;

        SortKey(Entry entry, Pattern digitSuffix){
            this.entry = entry;

            String full = entry.inputFile.getName();
            int dotIndex = full.lastIndexOf('.');
            if(dotIndex != -1) full = full.substring(0, dotIndex);

            String name = full;
            Matcher matcher = digitSuffix.matcher(full);
            if(matcher.matches()){
                try{
                    number = Integer.parseInt(matcher.group(2));
                    name = matcher.group(1);
                }catch(Exception ignored){
                }
            }
            this.name = name;
        }
    }

    /**
     * Packs directories concurrently, but hands out page names and writes atlases in the order the directories were added. Output
     * is therefore identical to processing them one after another.
     */
    private class PackQueue{
        //Mostly waits on Core.executor, which does the real work, so this doesn't need many threads. It's kept small as every
        //directory in progress keeps all of its images in memory.
        final ExecutorService pool = Threads.executor("Packer Directories", Math.max(1, Math.min(OS.cores, 4)));
        final Seq<Job> jobs = new Seq<>();
        /** Page images claimed so far, shared by all jobs, as rendering doesn't create the files immediately. */
        final Set<File> claimed = Collections.synchronizedSet(new HashSet<File>());
        volatile boolean failed;
        Job last;

        void add(Entry dir, TexturePacker packer, File outputDir, String packFileName){
            final Job job = new Job(dir, packer, outputDir, packFileName);
            final Job previous = last;
            last = job;
            packer.setClaimedFiles(claimed);
            jobs.add(job);
            //jobs are started in the order they're submitted, so the one being waited on has always started already
            job.result = pool.submit(() -> run(job, previous));
        }

        void run(Job job, Job previous){
            Seq<FutureTask<Void>> renders = null;
            try{
                if(failed) return;

                //the slow part: loading and packing, independent of anything else
                Seq<Seq<Page>> pages = job.packer.packScales();

                //wait for our turn to touch the output directory
                if(previous != null) previous.named.join();
                if(failed) return;

                job.packer.flushLog();
                renders = job.packer.write(job.outputDir, job.packFileName, pages);
            }catch(RuntimeException | Error t){
                failed = true;
                throw t;
            }finally{
                job.named.complete(null);
            }

            try{
                Tasks.joinAll(renders);
            }catch(RuntimeException | Error t){
                failed = true;
                throw t;
            }
        }

        /** Waits for every directory to be completely written. */
        void finish() throws Exception{
            for(Job job : jobs){
                try{
                    job.result.get();
                }catch(ExecutionException e){
                    failed = true;
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    throw new Exception("Error processing directory: " + job.dir.inputFile.getAbsolutePath(), cause);
                }
            }
        }

        /** Stops accepting work and waits for anything in progress, so nothing is being written once processing ends. */
        void close(){
            //if we get here due to an error, make queued directories bail out instead of packing pointlessly
            for(Job job : jobs){
                if(!job.result.isDone()) failed = true;
            }
            Threads.await(pool);
        }
    }

    private static class Job{
        final Entry dir;
        final TexturePacker packer;
        final File outputDir;
        final String packFileName;
        /** Completed once page names and atlas contents have been decided. */
        final CompletableFuture<Void> named = new CompletableFuture<>();
        Future<?> result;

        Job(Entry dir, TexturePacker packer, File outputDir, String packFileName){
            this.dir = dir;
            this.packer = packer;
            this.outputDir = outputDir;
            this.packFileName = packFileName;
        }
    }

}
