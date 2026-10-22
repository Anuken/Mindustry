package arc.packer;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData.*;
import arc.struct.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * Unpacks a texture atlas into individual image files.
 * @author Geert Konijnendijk
 * @author Nathan Sweet
 * @author Michael Bazos
 */
public class TextureUnpacker{
    private static final String DEFAULT_OUTPUT_PATH = "output";
    private static final int NINEPATCH_PADDING = 1;
    private static final String HELP = "Usage: atlasFile [imageDir] [outputDir]";
    private static final String ATLAS_FILE_EXTENSION = ".aatls";

    /**
     * Checks the command line arguments for correctness.
     * @return 0 If arguments are invalid, Number of arguments otherwise.
     */
    private int parseArguments(String[] args){
        int numArgs = args.length;
        // check if number of args is right
        if(numArgs < 1) return 0;
        // check if the input file's extension is right
        boolean extension = args[0].endsWith(ATLAS_FILE_EXTENSION);
        // check if the directory names are valid
        boolean directory = true;
        if(numArgs >= 2) directory = checkDirectoryValidity(args[1]);
        if(numArgs == 3) directory &= checkDirectoryValidity(args[2]);
        return extension && directory ? numArgs : 0;
    }

    private boolean checkDirectoryValidity(String directory){
        File checkFile = new File(directory);
        boolean path = true;
        // try to get the canonical path, if this fails the path is not valid
        try{
            checkFile.getCanonicalPath();
        }catch(Exception e){
            path = false;
        }
        return path;
    }

    /** Splits an atlas into seperate image and ninepatch files. */
    public void splitAtlas(TextureAtlasData atlas, String outputDir, boolean quiet){
        // create the output directory if it did not exist yet
        File outputDirFile = new File(outputDir);
        if(!outputDirFile.exists()){
            outputDirFile.mkdirs();
            if(!quiet) System.out.printf("Creating directory: %s%n", outputDirFile.getPath());
        }

        for(AtlasPage page : atlas.pages){
            // load the image file belonging to this page as a Buffered Image
            File file = page.textureFile.file();
            if(!file.exists()) throw new RuntimeException("Unable to find atlas image: " + file.getAbsolutePath());
            Pixmap img = new Pixmap(new Fi(file));

            // regions are independent of each other, so they are extracted and written in parallel
            Seq<FutureTask<Void>> tasks = new Seq<>();
            try{
                for(Region region : atlas.regions){
                    // only regions on this page are of interest
                    if(region.page != page) continue;

                    if(!quiet) System.out.printf("Processing image for %s: x[%s] y[%s] w[%s] h[%s], rotate[%s]%n",
                    region.name, region.left, region.top, region.width, region.height, region.rotate);

                    tasks.add(Tasks.submit(() -> {
                        splitRegion(img, region, outputDirFile, quiet);
                        return null;
                    }));
                }
                Tasks.joinAll(tasks);
            }finally{
                Tasks.cancelAll(tasks);
                //don't dispose while regions might still be reading the page
                Tasks.awaitAll(tasks);
                img.dispose();
            }
        }
    }

    private void splitRegion(Pixmap img, Region region, File outputDirFile, boolean quiet){
        Pixmap splitImage = null;
        String extension = null;

        // check if the region is a ninepatch or a normal image and delegate accordingly
        if(region.splits == null){
            splitImage = extractImage(img, region, 0);
            if(region.width != region.originalWidth || region.height != region.originalHeight){
                Pixmap originalImg = new Pixmap(region.originalWidth, region.originalHeight);
                originalImg.draw(splitImage, (int)region.offsetX, (int)(region.originalHeight - region.height - region.offsetY));
                splitImage.dispose();
                splitImage = originalImg;
            }
            extension = ".png";
        }else{
            splitImage = extractNinePatch(img, region);
            extension = "9.png";
        }

        try{
            // check if the parent directories of this image file exist and create them if not
            File imgOutput = new File(outputDirFile, region.name + extension);
            File imgDir = imgOutput.getParentFile();
            if(!imgDir.exists()){
                //several regions may want the same directory at once, mkdirs is safe with that
                if(imgDir.mkdirs() && !quiet) System.out.printf("Creating directory: %s%n", imgDir.getPath());
            }

            new Fi(imgOutput).writePng(splitImage);
        }finally{
            splitImage.dispose();
        }
    }

    /**
     * Extract an image from a texture atlas.
     * @param page The image file related to the page the region is in
     * @param region The region to extract
     * @param padding padding (in pixels) to apply to the image
     * @return The extracted image
     */
    private Pixmap extractImage(Pixmap page, Region region, int padding){
        Pixmap splitImage;

        // get the needed part of the page and rotate if needed
        splitImage = page.crop(region.left, region.top, region.width, region.height);

        // draw the image to a bigger one if padding is needed
        if(padding > 0){
            Pixmap paddedImage = new Pixmap(splitImage.getWidth() + padding * 2, splitImage.getHeight() + padding * 2);
            paddedImage.draw(splitImage, padding, padding);
            splitImage.dispose();
            return paddedImage;
        }else{
            return splitImage;
        }
    }

    /**
     * Extract a ninepatch from a texture atlas, according to the android specification.
     * @param page The image file related to the page the region is in
     * @param region The region to extract
     * @see <a href="http://developer.android.com/guide/topics/graphics/2d-graphics.html#nine-patch">ninepatch specification</a>
     */
    private Pixmap extractNinePatch(Pixmap page, Region region){
        Pixmap splitImage = extractImage(page, region, NINEPATCH_PADDING);

        // Draw the four lines to save the ninepatch's padding and splits
        int startX = region.splits[0] + NINEPATCH_PADDING;
        int endX = region.width - region.splits[1] + NINEPATCH_PADDING - 1;
        int startY = region.splits[2] + NINEPATCH_PADDING;
        int endY = region.height - region.splits[3] + NINEPATCH_PADDING - 1;
        if(endX >= startX) splitImage.drawLine(startX, 0, endX, 0, Color.blackRgba);
        if(endY >= startY) splitImage.drawLine(0, startY, 0, endY, Color.blackRgba);
        if(region.pads != null){
            int padStartX = region.pads[0] + NINEPATCH_PADDING;
            int padEndX = region.width - region.pads[1] + NINEPATCH_PADDING - 1;
            int padStartY = region.pads[2] + NINEPATCH_PADDING;
            int padEndY = region.height - region.pads[3] + NINEPATCH_PADDING - 1;
            splitImage.drawLine(padStartX, splitImage.getHeight() - 1, padEndX, splitImage.getHeight() - 1, Color.blackRgba);
            splitImage.drawLine(splitImage.getWidth() - 1, padStartY, splitImage.getWidth() - 1, padEndY, Color.blackRgba);
        }

        return splitImage;
    }

    public static void main(String[] args){
        TextureUnpacker unpacker = new TextureUnpacker();

        String atlasFile = null, imageDir = null, outputDir = null;

        // parse the arguments and display the help text if there is a problem with the command line arguments
        switch(unpacker.parseArguments(args)){
            case 0:
                System.out.println(HELP);
                return;
            case 3:
                outputDir = args[2];
            case 2:
                imageDir = args[1];
            case 1:
                atlasFile = args[0];
        }

        File atlasFileHandle = new File(atlasFile).getAbsoluteFile();
        if(!atlasFileHandle.exists()) throw new RuntimeException("Atlas file not found: " + atlasFileHandle.getAbsolutePath());
        String atlasParentPath = atlasFileHandle.getParentFile().getAbsolutePath();

        // Set the directory variables to a default when they weren't given in the variables
        if(imageDir == null) imageDir = atlasParentPath;
        if(outputDir == null) outputDir = (new File(atlasParentPath, DEFAULT_OUTPUT_PATH)).getAbsolutePath();

        // Opens the atlas file from the specified filename
        TextureAtlasData atlas = new TextureAtlasData(new Fi(atlasFile), new Fi(imageDir), false);
        unpacker.splitAtlas(atlas, outputDir, false);
    }
}
