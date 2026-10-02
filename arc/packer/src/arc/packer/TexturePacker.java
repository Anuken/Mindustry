package arc.packer;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** @author Nathan Sweet */
public class TexturePacker{
    String rootPath;
    private final Settings settings;
    private final Packer packer;
    private final ImageProcessor imageProcessor;
    private final Seq<InputImage> inputImages = new Seq<>();

    /** Page image names claimed by this packer, or by every packer taking part in the same batch. Page rendering is asynchronous, so a claimed file may not exist on disk yet. */
    private Set<File> claimedFiles = Collections.synchronizedSet(new HashSet<File>());
    /** Buffers messages so that the output of packers running concurrently doesn't interleave. Null if messages are printed directly. */
    private ByteArrayOutputStream logBuffer;

    /** @param rootDir See {@link #setRootDir(File)}. */
    public TexturePacker(File rootDir, Settings settings){
        this.settings = settings;

        if(settings.pot){
            if(settings.maxWidth != Mathf.nextPowerOfTwo(settings.maxWidth))
                throw new RuntimeException("If pot is true, maxWidth must be a power of two: " + settings.maxWidth);
            if(settings.maxHeight != Mathf.nextPowerOfTwo(settings.maxHeight))
                throw new RuntimeException("If pot is true, maxHeight must be a power of two: " + settings.maxHeight);
        }

        if(settings.multipleOfFour){
            if(settings.maxWidth % 4 != 0)
                throw new RuntimeException("If mod4 is true, maxWidth must be evenly divisible by 4: " + settings.maxWidth);
            if(settings.maxHeight % 4 != 0)
                throw new RuntimeException("If mod4 is true, maxHeight must be evenly divisible by 4: " + settings.maxHeight);
        }

        if(settings.grid)
            packer = new GridPacker(settings);
        else
            packer = new MaxRectsPacker(settings);

        imageProcessor = new ImageProcessor(settings);
        setRootDir(rootDir);
    }

    public TexturePacker(Settings settings){
        this(null, settings);
    }

    /** @param rootDir Used to strip the root directory prefix from image file names, can be null. */
    public void setRootDir(File rootDir){
        if(rootDir == null){
            rootPath = null;
            return;
        }
        rootPath = rootDir.getAbsolutePath().replace('\\', '/');
        if(!rootPath.endsWith("/")) rootPath += "/";
    }

    /** Shares the set of claimed page files between packers that write to the same directory, see {@link #claimedFiles}. */
    void setClaimedFiles(Set<File> claimedFiles){
        this.claimedFiles = claimedFiles;
    }

    /** Messages are held back until {@link #flushLog()} is called. */
    void bufferLog(){
        logBuffer = new ByteArrayOutputStream();
        setLog(new PrintStream(logBuffer));
    }

    /** Prints held back messages, and everything from here on is printed directly. */
    void flushLog(){
        if(logBuffer == null) return;
        ByteArrayOutputStream buffer = logBuffer;
        logBuffer = null;
        PrintStream stream = log();
        stream.flush();
        setLog(System.out);
        System.out.print(buffer.toString());
        System.out.flush();
    }

    private PrintStream log;

    /** @return the stream messages should be printed to. */
    PrintStream log(){
        return log == null ? System.out : log;
    }

    private void setLog(PrintStream stream){
        log = stream;
        imageProcessor.log = stream;
        if(packer instanceof MaxRectsPacker) ((MaxRectsPacker)packer).log = stream;
        if(packer instanceof GridPacker) ((GridPacker)packer).log = stream;
    }

    public void addImage(File file){
        InputImage inputImage = new InputImage();
        inputImage.file = file;
        inputImage.rootPath = rootPath;
        inputImages.add(inputImage);
    }

    public void addImage(Pixmap image, String name){
        InputImage inputImage = new InputImage();
        inputImage.image = image;
        inputImage.name = name;
        inputImages.add(inputImage);
    }

    public void pack(File outputDir, String packFileName){
        outputDir.mkdirs();

        Seq<Seq<Page>> scaled = packScales();
        Seq<FutureTask<Void>> renders = write(outputDir, packFileName, scaled);
        Tasks.joinAll(renders);
    }

    /**
     * Loads and packs the images for every scale. No files are written, so this can run concurrently with other packers.
     * @return the pages for each scale, in the order of {@link Settings#scale}.
     */
    Seq<Seq<Page>> packScales(){
        int n = settings.scale.length;
        Seq<Seq<Page>> result = new Seq<>(n);
        for(int i = 0; i < n; i++){

            imageProcessor.setScale(settings.scale[i]);
            imageProcessor.setResampling(settings.scaleResampling);

            imageProcessor.addAll(inputImages);
            result.add(packer.pack(imageProcessor.getImages()));

            imageProcessor.clear();
        }
        return result;
    }

    /**
     * Assigns page file names, writes the pack files and starts rendering the page images in the background. This is the only
     * part of packing which depends on what is already in the output directory, so when several packers write to the same place
     * it must be called for each of them in the same order every time. Rendering does not depend on any shared state.
     * @return the running page renders. Wait for all of them before using the output.
     */
    Seq<FutureTask<Void>> write(File outputDir, String packFileName, Seq<Seq<Page>> scaled){
        if(packFileName.endsWith(settings.atlasExtension))
            packFileName = packFileName.substring(0, packFileName.length() - settings.atlasExtension.length());
        outputDir.mkdirs();

        Seq<FutureTask<Void>> renders = new Seq<>();
        try{
            for(int i = 0, n = scaled.size; i < n; i++){
                Seq<Page> pages = scaled.get(i);
                String scaledPackFileName = settings.getScaledPackFileName(packFileName, i);
                writeImages(outputDir, scaledPackFileName, pages, renders);
                try{
                    writePackFile(outputDir, scaledPackFileName, pages);
                }catch(IOException ex){
                    throw new RuntimeException("Error writing pack file.", ex);
                }
            }
        }catch(RuntimeException | Error e){
            Tasks.cancelAll(renders);
            throw e;
        }
        return renders;
    }

    /** Computes page sizes and names, then queues rendering of each page into the list. */
    private void writeImages(File outputDir, String scaledPackFileName, Seq<Page> pages, Seq<FutureTask<Void>> renders){
        File packFileNoExt = new File(outputDir, scaledPackFileName);
        File packDir = packFileNoExt.getParentFile();
        String imageName = packFileNoExt.getName();

        int fileIndex = 0;
        for(int p = 0, pn = pages.size; p < pn; p++){
            final Page page = pages.get(p);

            int width = page.width, height = page.height;
            int edgePadX, edgePadY;
            if(settings.edgePadding){
                edgePadX = settings.paddingX;
                edgePadY = settings.paddingY;
                if(settings.duplicatePadding){
                    edgePadX /= 2;
                    edgePadY /= 2;
                }
                page.x = edgePadX;
                page.y = edgePadY;
                width += edgePadX * 2;
                height += edgePadY * 2;
            }
            if(settings.pot){
                width = Mathf.nextPowerOfTwo(width);
                height = Mathf.nextPowerOfTwo(height);
            }
            if(settings.multipleOfFour){
                width = width % 4 == 0 ? width : width + 4 - (width % 4);
                height = height % 4 == 0 ? height : height + 4 - (height % 4);
            }
            width = Math.max(settings.minWidth, width);
            height = Math.max(settings.minHeight, height);
            page.imageWidth = width;
            page.imageHeight = height;

            //sync point: pick a free file name and claim it, since the file itself isn't written until later
            final File outputFile;
            while(true){
                File candidate = new File(packDir, imageName + (fileIndex++ == 0 ? "" : fileIndex) + ".png");
                if(!candidate.exists() && claimedFiles.add(candidate)){
                    outputFile = candidate;
                    break;
                }
            }
            new Fi(outputFile).parent().mkdirs();
            page.imageName = outputFile.getName();

            if(!settings.silent) log().println("| Writing " + width + "x" + height + ": " + outputFile);

            //take a snapshot of the draw order, as writing the pack file re-sorts the page's rects
            final Seq<Rect> drawOrder = new Seq<>(page.outputRects);
            final int canvasWidth = width, canvasHeight = height;
            renders.add(Tasks.submit(() -> {
                renderPage(page, drawOrder, outputFile, canvasWidth, canvasHeight);
                return null;
            }));
        }
    }

    /** Draws all rects onto a canvas and saves it. Must be safe to run on any thread, at the same time as other pages. */
    private void renderPage(Page page, Seq<Rect> drawOrder, File outputFile, int width, int height){
        Pixmap canvas = new Pixmap(width, height);
        try{
            for(int r = 0, rn = drawOrder.size; r < rn; r++){
                Rect rect = drawOrder.get(r);
                Pixmap image = rect.getImage(imageProcessor);
                int iw = image.width;
                int ih = image.height;
                int rectX = page.x + rect.x, rectY = page.y + page.height - rect.y - (rect.height - settings.paddingY);
                if(settings.duplicatePadding){
                    int amountX = settings.paddingX / 2;
                    int amountY = settings.paddingY / 2;
                    if(rect.rotated){
                        // Copy corner pixels to fill corners of the padding.
                        for(int i = 1; i <= amountX; i++){
                            for(int j = 1; j <= amountY; j++){
                                canvas.set(rectX - j, rectY + iw - 1 + i, image.getRaw(0, 0));
                                canvas.set(rectX + ih - 1 + j, rectY + iw - 1 + i, image.getRaw(0, ih - 1));
                                canvas.set(rectX - j, rectY - i, image.getRaw(iw - 1, 0));
                                canvas.set(rectX + ih - 1 + j, rectY - i, image.getRaw(iw - 1, ih - 1));
                            }
                        }
                        // Copy edge pixels into padding.
                        for(int i = 1; i <= amountY; i++){
                            for(int j = 0; j < iw; j++){
                                canvas.set(rectX - i, rectY + iw - 1 - j, image.getRaw(j, 0));
                                canvas.set(rectX + ih - 1 + i, rectY + iw - 1 - j, image.getRaw(j, ih - 1));
                            }
                        }
                        for(int i = 1; i <= amountX; i++){
                            for(int j = 0; j < ih; j++){
                                canvas.set(rectX + j, rectY - i, image.getRaw(iw - 1, j));
                                canvas.set(rectX + j, rectY + iw - 1 + i, image.getRaw(0, j));
                            }
                        }
                    }else{
                        // Copy corner pixels to fill corners of the padding.
                        for(int i = 1; i <= amountX; i++){
                            for(int j = 1; j <= amountY; j++){
                                canvas.set(rectX - i, rectY - j, image.getRaw(0, 0));
                                canvas.set(rectX - i, rectY + ih - 1 + j, image.getRaw(0, ih - 1));
                                canvas.set(rectX + iw - 1 + i, rectY - j, image.getRaw(iw - 1, 0));
                                canvas.set(rectX + iw - 1 + i, rectY + ih - 1 + j, image.getRaw(iw - 1, ih - 1));
                            }
                        }
                        // Copy edge pixels into padding.
                        for(int i = 1; i <= amountY; i++){
                            copy(image, 0, 0, iw, 1, canvas, rectX, rectY - i, rect.rotated);
                            copy(image, 0, ih - 1, iw, 1, canvas, rectX, rectY + ih - 1 + i, rect.rotated);
                        }
                        for(int i = 1; i <= amountX; i++){
                            copy(image, 0, 0, 1, ih, canvas, rectX - i, rectY, rect.rotated);
                            copy(image, iw - 1, 0, 1, ih, canvas, rectX + iw - 1 + i, rectY, rect.rotated);
                        }
                    }
                }
                copy(image, 0, 0, iw, ih, canvas, rectX, rectY, rect.rotated);

                //the source image has been fully drawn, release its memory right away
                if(rect.ownsPixmap) image.dispose();
            }

            if(settings.bleed){
                Pixmaps.bleed(canvas, settings.bleedIterations);
            }

            PixmapIO.writePng(new Fi(outputFile), canvas);
        }finally{
            canvas.dispose();
        }
    }

    private static void copy(Pixmap src, int x, int y, int w, int h, Pixmap dst, int dx, int dy, boolean rotated){
        if(rotated){
            for(int i = 0; i < w; i++)
                for(int j = 0; j < h; j++)
                    dst.set(dx + j, dy + w - i - 1, src.getRaw(x + i, y + j));
        }else{
            dst.draw(src, x, y, w, h, dx, dy, w, h);
        }
    }

    private void writePackFile(File outputDir, String scaledPackFileName, Seq<Page> pages) throws IOException{
        Fi packFile = new Fi(outputDir).child(scaledPackFileName + settings.atlasExtension);
        Fi packDir = packFile.parent();
        packDir.mkdirs();

        //sync point
        boolean existed = packFile.exists() && packFile.length() > 0;

        try(Writes write = packFile.writes(true)){
            //write meta to start of file
            if(!existed){
                write.b(TextureAtlasData.formatHeader);
                write.b(TextureAtlasData.formatVersion);
            }

            //write every page; reader is expected to read until EOF
            for(Page page : pages){
                //write a single byte to check for EOF
                write.b(1);
                write.str(page.imageName);
                //size
                write.s(page.imageWidth);
                write.s(page.imageHeight);
                //filters, wrapping
                write.b(settings.filterMin.ordinal());
                write.b(settings.filterMag.ordinal());
                write.b(settings.wrapX.ordinal());
                write.b(settings.wrapY.ordinal());

                //write total rects
                write.i(page.outputRects.sum(i -> 1 + i.aliases.size()));

                page.outputRects.sort();
                for(Rect rect : page.outputRects){
                    writeRect(write, page, rect, rect.name);
                    Seq<Alias> aliases = new Seq<>(rect.aliases.toArray(new Alias[0]));
                    aliases.sort();
                    for(Alias alias : aliases){
                        Rect aliasRect = new Rect();
                        aliasRect.set(rect);
                        alias.apply(aliasRect);
                        writeRect(write, page, aliasRect, alias.name);
                    }
                }
            }
        }
    }

    private void writeRect(Writes write, Page page, Rect rect, String name) throws IOException{
        boolean offsets = rect.originalWidth != rect.regionWidth || rect.originalHeight != rect.regionHeight;

        //name
        write.str(Rect.getAtlasName(name, settings.flattenPaths));
        //xy
        write.s(page.x + rect.x);
        write.s((page.y + page.height - rect.y - (rect.height - settings.paddingY)));
        //size
        write.s(rect.regionWidth);
        write.s(rect.regionHeight);

        //optional offsets
        write.bool(offsets);
        if(offsets){
            //offset xy
            write.s(rect.offsetX);
            write.s((rect.originalHeight - rect.regionHeight - rect.offsetY));
            //original size
            write.s(rect.originalWidth);
            write.s(rect.originalHeight);
        }

        //optional splits
        write.bool(rect.splits != null);
        if(rect.splits != null){
            for(int i = 0; i < 4; i++){
                write.s(rect.splits[i]);
            }
        }
        //optional pads
        write.bool(rect.pads != null);
        if(rect.pads != null){
            for(int i = 0; i < 4; i++){
                write.s(rect.pads[i]);
            }
        }
    }

    /** @author Nathan Sweet */
    public static class Page{
        public String imageName;
        public Seq<Rect> outputRects, remainingRects;
        public float occupancy;
        public int x, y, width, height, imageWidth, imageHeight;
    }

    /**
     * @author Regnarock
     * @author Nathan Sweet
     */
    public static class Alias implements Comparable<Alias>{
        public String name;
        public int index;
        public int[] splits;
        public int[] pads;
        public int offsetX, offsetY, originalWidth, originalHeight;

        public Alias(Rect rect){
            name = rect.name;
            splits = rect.splits;
            pads = rect.pads;
            offsetX = rect.offsetX;
            offsetY = rect.offsetY;
            originalWidth = rect.originalWidth;
            originalHeight = rect.originalHeight;
        }

        public void apply(Rect rect){
            rect.name = name;
            rect.splits = splits;
            rect.pads = pads;
            rect.offsetX = offsetX;
            rect.offsetY = offsetY;
            rect.originalWidth = originalWidth;
            rect.originalHeight = originalHeight;
        }

        @Override
        public int compareTo(Alias o){
            return name.compareTo(o.name);
        }
    }

    /** @author Nathan Sweet */
    public static class Rect implements Comparable<Rect>{
        public String name;
        public int offsetX, offsetY, regionWidth, regionHeight, originalWidth, originalHeight;
        public int x, y;
        public int width, height; // Portion of page taken by this region, including padding.
        public boolean rotated;
        public Set<Alias> aliases;
        public int[] splits;
        public int[] pads;
        public boolean canRotate = true;

        boolean isPatch;
        Pixmap pixmap;
        /** Whether pixmap was created by the packer, and can be disposed once it has been drawn. */
        boolean ownsPixmap;
        Fi file;
        int score1, score2;

        Rect(Pixmap source, int left, int top, int newWidth, int newHeight, boolean isPatch){
            aliases = new HashSet<>();
            if(source.width ==  newWidth && source.height == newHeight && left == 0 && top == 0){
                this.pixmap = source;
            }else{
                this.pixmap = source.crop(left, top, newWidth, newHeight);
            }
            offsetX = left;
            offsetY = top;
            regionWidth = newWidth;
            regionHeight = newHeight;
            originalWidth = source.width;
            originalHeight = source.height;
            width = newWidth;
            height = newHeight;
            this.isPatch = isPatch;
        }

        public Pixmap getImage(ImageProcessor imageProcessor){
            if(pixmap != null) return pixmap;

            Pixmap image = new Pixmap(file);
            String name = this.name;
            if(isPatch) name += ".9";
            return imageProcessor.processImage(image, name).getImage(null);
        }

        /** Creates a bare node for use by the packing algorithm. Node rects never have aliases. */
        Rect(){
            aliases = Collections.emptySet();
        }

        Rect(Rect rect){
            aliases = Collections.emptySet();
            x = rect.x;
            y = rect.y;
            width = rect.width;
            height = rect.height;
        }

        void set(Rect rect){
            name = rect.name;
            pixmap = rect.pixmap;
            ownsPixmap = rect.ownsPixmap;
            offsetX = rect.offsetX;
            offsetY = rect.offsetY;
            regionWidth = rect.regionWidth;
            regionHeight = rect.regionHeight;
            originalWidth = rect.originalWidth;
            originalHeight = rect.originalHeight;
            x = rect.x;
            y = rect.y;
            width = rect.width;
            height = rect.height;
            rotated = rect.rotated;
            aliases = rect.aliases;
            splits = rect.splits;
            pads = rect.pads;
            canRotate = rect.canRotate;
            score1 = rect.score1;
            score2 = rect.score2;
            file = rect.file;
            isPatch = rect.isPatch;
        }

        @Override
        public int compareTo(Rect o){
            return name.compareTo(o.name);
        }

        @Override
        public boolean equals(Object obj){
            if(this == obj) return true;
            if(obj == null) return false;
            if(getClass() != obj.getClass()) return false;
            Rect other = (Rect)obj;
            if(name == null){
                return other.name == null;
            }else return name.equals(other.name);
        }

        @Override
        public String toString(){
            return name + "[" + x + "," + y + " " + width + "x" + height + "]";
        }

        public static String getAtlasName(String name, boolean flattenPaths){
            return flattenPaths ? new Fi(name).name() : name;
        }
    }

    /**
     * Packs using defaults settings.
     * @see TexturePacker#process(Settings, String, String, String)
     */
    public static void process(String input, String output, String packFileName){
        process(new Settings(), input, output, packFileName);
    }

    /**
     * @param input Directory containing individual images to be packed.
     * @param output Directory where the pack file and page images will be written.
     * @param packFileName The name of the pack file. Also used to name the page images.
     */
    public static void process(Settings settings, String input, String output, String packFileName){
        try{
            TexturePackerFileProcessor processor = new TexturePackerFileProcessor(settings, packFileName);
            processor.process(new File(input), new File(output));
        }catch(Exception ex){
            throw new RuntimeException("Error packing images: " + Strings.getFinalMessage(ex), ex);
        }
    }

    /**
     * @return true if the output file does not yet exist or its last modification date is before the last modification date of
     * the input file
     */
    public static boolean isModified(String input, String output, String packFileName, Settings settings){
        String packFullFileName = output;

        if(!packFullFileName.endsWith("/")){
            packFullFileName += "/";
        }

        // Check against the only file we know for sure will exist and will be changed if any asset changes:
        // the atlas file
        packFullFileName += packFileName;
        packFullFileName += settings.atlasExtension;
        File outputFile = new File(packFullFileName);

        if(!outputFile.exists()){
            return true;
        }

        File inputFile = new File(input);
        if(!inputFile.exists()){
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.getAbsolutePath());
        }

        return isModified(inputFile, outputFile.lastModified());
    }

    private static boolean isModified(File file, long lastModified){
        if(file.lastModified() > lastModified) return true;
        File[] children = file.listFiles();
        if(children != null){
            for(File child : children)
                if(isModified(child, lastModified)) return true;
        }
        return false;
    }

    public static boolean processIfModified(String input, String output, String packFileName){
        // Default settings (Needed to access the default atlas extension string)
        Settings settings = new Settings();

        if(isModified(input, output, packFileName, settings)){
            process(settings, input, output, packFileName);
            return true;
        }
        return false;
    }

    public static boolean processIfModified(Settings settings, String input, String output, String packFileName){
        if(isModified(input, output, packFileName, settings)){
            process(settings, input, output, packFileName);
            return true;
        }
        return false;
    }

    public interface Packer{
        Seq<Page> pack(Seq<Rect> inputRects);
    }

    static final class InputImage{
        File file;
        String rootPath, name;
        Pixmap image;
    }

    /** @author Nathan Sweet */
    public static class Settings implements Cloneable{
        public boolean pot = true;
        public boolean multipleOfFour;
        public int paddingX = 2, paddingY = 2;
        public boolean edgePadding = true;
        public boolean duplicatePadding = false;
        public boolean rotation;
        public int minWidth = 16, minHeight = 16;
        public int maxWidth = 1024, maxHeight = 1024;
        public boolean square = false;
        public boolean stripWhitespaceX, stripWhitespaceY;
        /** Whether to strip whitespace in a way that keeps the region rectangle centered. */
        public boolean stripWhitespaceCenter;
        /** Paths containing these strings do not have whitespace stripped. */
        public String[] ignoredWhitespaceStrings = {};
        public int alphaThreshold;
        public TextureFilter filterMin = TextureFilter.nearest, filterMag = TextureFilter.nearest;
        public TextureWrap wrapX = TextureWrap.clampToEdge, wrapY = TextureWrap.clampToEdge;
        public boolean alias = true;
        public boolean ignoreBlankImages = true;
        public boolean fast = true; //with fast = false packing takes an eternity, I have no idea why that wasn't the default before
        public boolean silent;
        public boolean printAliases;
        public boolean combineSubdirectories;
        public boolean ignore;
        public boolean flattenPaths;
        public boolean bleed = true;
        public int bleedIterations = 2;
        public boolean grid;
        public float[] scale = {1};
        public String[] scaleSuffix = {""};
        public boolean scaleResampling = true;
        public String atlasExtension = ".aatls";

        public Settings copy(){
            try{
                return (Settings)clone();
            }catch(Exception e){
                throw new RuntimeException("java is a disaster", e);
            }
        }

        public String getScaledPackFileName(String packFileName, int scaleIndex){
            // Use suffix if not empty string.
            if(scaleSuffix[scaleIndex].length() > 0)
                packFileName += scaleSuffix[scaleIndex];
            else{
                // Otherwise if scale != 1 or multiple scales, use subdirectory.
                float scaleValue = scale[scaleIndex];
                if(scale.length != 1){
                    packFileName = (scaleValue == (int)scaleValue ? Integer.toString((int)scaleValue) : Float.toString(scaleValue))
                    + "/" + packFileName;
                }
            }
            return packFileName;
        }
    }

    public static void main(String[] args) throws Exception{
        /*
        //for testing locally
        if(true){
            File rootDir = new File("/home/anuke/Projects/Mindustry");
            process(new File(rootDir, "core/assets-raw/sprites_out/").getAbsolutePath(), new File(rootDir, "core/assets/sprites/").getAbsolutePath(), "sprites.aatls");

            return;
        }*/

        Settings settings = null;
        String input = null, output = null, packFileName = "pack.aatls";

        switch(args.length){
            case 4:
                settings = new Json().fromJson(Settings.class, new FileReader(args[3]));
            case 3:
                packFileName = args[2];
            case 2:
                output = args[1];
            case 1:
                input = args[0];
                break;
            default:
                System.out.println("Usage: inputDir [outputDir] [packFileName] [settingsFileName]");
                System.exit(0);
        }

        if(output == null){
            File inputFile = new File(input);
            output = new File(inputFile.getParentFile(), inputFile.getName() + "-packed").getAbsolutePath();
        }
        if(settings == null) settings = new Settings();

        process(settings, input, output, packFileName);
    }
}
