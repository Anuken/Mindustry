package arc.backend.sdl;

import arc.util.*;
import arc.util.io.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static arc.util.OS.*;

public class ANGLELoader{
    static private final Random random = new Random();
    static private File egl, gles, lastWorkingDir;

    static String randomUUID(){
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    public static String crc(InputStream input){
        if(input == null) throw new IllegalArgumentException("input cannot be null.");
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        try{
            while(true){
                int length = input.read(buffer);
                if(length == -1) break;
                crc.update(buffer, 0, length);
            }
        }catch(Exception ex){
        }finally{
            Streams.close(input);
        }
        return Long.toString(crc.getValue(), 16);
    }

    private static File extractFile(String sourcePath, File outFile){
        try{
            if(!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs()) throw new ArcRuntimeException(
            "Couldn't create ANGLE native library output directory " + outFile.getParentFile().getAbsolutePath());

            if(outFile.exists()) return outFile;

            try(OutputStream out = new FileOutputStream(outFile); InputStream in = readFile(sourcePath)){
                Streams.copy(in, out);
                return outFile;
            }
        }catch(Throwable t){
            throw new ArcRuntimeException("Couldn't load ANGLE shared library " + sourcePath, t);
        }
    }

    /**
     * Returns a path to a file that can be written. Tries multiple locations and verifies writing succeeds.
     * @return null if a writable path could not be found.
     */
    private static File getExtractedFile(String dirName, String fileName){
        // Temp directory with username in path.
        File idealFile = new File(
        System.getProperty("java.io.tmpdir") + "/arc" + System.getProperty("user.name") + "/" + dirName, fileName);
        if(canWrite(idealFile)) return idealFile;

        // System provided temp directory.
        try{
            File file = File.createTempFile(dirName, null);
            if(file.delete()){
                file = new File(file, fileName);
                if(canWrite(file)) return file;
            }
        }catch(IOException ignored){
        }

        // User home.
        File file = new File(System.getProperty("user.home") + "/.libgdx/" + dirName, fileName);
        if(canWrite(file)) return file;

        // Relative directory.
        file = new File(".temp/" + dirName, fileName);
        if(canWrite(file)) return file;

        // We are running in the OS X sandbox.
        if(System.getenv("APP_SANDBOX_CONTAINER_ID") != null) return idealFile;

        return null;
    }

    /** Returns true if the parent directories of the file can be created and the file can be written. */
    private static boolean canWrite(File file){
        File parent = file.getParentFile();
        File testFile;
        if(file.exists()){
            if(!file.canWrite() || !canExecute(file)) return false;
            // Don't overwrite existing file just to check if we can write to directory.
            testFile = new File(parent, randomUUID());
        }else{
            parent.mkdirs();
            if(!parent.isDirectory()) return false;
            testFile = file;
        }
        try{
            new FileOutputStream(testFile).close();
            return canExecute(testFile);
        }catch(Throwable ex){
            return false;
        }finally{
            testFile.delete();
        }
    }

    private static boolean canExecute(File file){
        try{
            if(file.canExecute()) return true;

            file.setExecutable(true, false);

            return file.canExecute();
        }catch(Exception ignored){
        }
        return false;
    }

    public static boolean isCompatible(){
        String osDir = "";
        String arch = isARM ? (is64Bit ? "arm64" : "arm32") : (is64Bit ? "x64" : "x86");
        String ext = "";
        if(isWindows){
            osDir = "windows";
            ext = ".dll";
        }
        if(isLinux){
            osDir = "linux";
            ext = ".so";
        }
        if(isMac){
            osDir = "macos";
            ext = ".dylib";
        }

        String dir = osDir + "/" + arch + "/angle";

        String eglSource = dir + "/libEGL" + ext;
        String glesSource = dir + "/libGLESv2" + ext;

        return ANGLELoader.class.getClassLoader().getResource(eglSource) != null
        && ANGLELoader.class.getClassLoader().getResource(glesSource) != null;
    }

    private static InputStream readFile(String path){
        InputStream input = ANGLELoader.class.getClassLoader().getResourceAsStream(path);
        if(input == null) throw new ArcRuntimeException("Unable to read file for extraction: " + path);
        return input;
    }

    public static void load(){
        String osDir = "";
        String arch = isARM ? (is64Bit ? "arm64" : "arm32") : (is64Bit ? "x64" : "x86");
        String ext = "";
        if(isWindows){
            osDir = "windows";
            ext = ".dll";
        }
        if(isLinux){
            osDir = "linux";
            ext = ".so";
        }
        if(isMac){
            osDir = "macos";
            ext = ".dylib";
        }

        String dir = osDir + "/" + arch + "/angle";

        String eglSource = dir + "/libEGL" + ext;
        String glesSource = dir + "/libGLESv2" + ext;
        String crc = crc(readFile(eglSource)) + crc(readFile(glesSource));
        egl = getExtractedFile(crc, new File(eglSource).getName());
        gles = getExtractedFile(crc, new File(glesSource).getName());

        if(!isMac){
            extractFile(eglSource, egl);
            System.load(egl.getAbsolutePath());
            extractFile(glesSource, gles);
            System.load(gles.getAbsolutePath());
        }else{
            // On macOS, we can't preload the shared libraries. calling dlopen("path1/lib.dylib")
            // then calling dlopen("lib.dylib") will not return the dylib loaded in the first dlopen()
            // call, but instead perform the dlopen library search algorithm anew. Since the dylibs
            // we extract are not in any paths dlopen knows about, GLFW fails to load them.
            // Instead, we need to copy the shared libraries to the current working directory (which
            // we can't temporarily change in pure Java either...). The dylibs will get deleted
            // in postGlfwInit() once the first window has been created, and GLFW has loaded the dylibs.
            lastWorkingDir = new File(".");
            extractFile(eglSource, new File(lastWorkingDir, egl.getName()));
            extractFile(glesSource, new File(lastWorkingDir, gles.getName()));
        }
    }

    public static void postGlfwInit(){
        new File(lastWorkingDir, egl.getName()).delete();
        new File(lastWorkingDir, gles.getName()).delete();
    }
}