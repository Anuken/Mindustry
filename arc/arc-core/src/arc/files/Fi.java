package arc.files;

import arc.*;
import arc.Files.*;
import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.FileChannel.*;
import java.security.*;
import java.util.zip.*;

/**
 * Represents a file or directory on the filesystem, classpath, Android SD card, or Android assets directory. FileHandles are
 * created via a {@link Files} instance.
 * <p>
 * Because some of the file types are backed by composite files and may be compressed (for example, if they are in an Android .apk
 * or are found via the classpath), the methods for extracting a {@link #path()} or {@link #file()} may not be appropriate for all
 * types. Use the Reader or Stream methods here to hide these dependencies from your platform independent code.
 * @author mzechner
 * @author Nathan Sweet
 */
public class Fi implements Comparable<Fi>{
    protected File file;
    protected FileType type;

    protected Fi(){
    }

    /**
     * Creates a new absolute FileHandle for the file name. Use this for tools on the desktop that don't need any of the backends.
     * Do not use this constructor in case you write something cross-platform. Use the {@link Files} interface instead.
     * @param fileName the filename.
     */
    public Fi(String fileName){
        this.file = new File(fileName);
        this.type = FileType.absolute;
    }

    /**
     * Creates a new absolute FileHandle for the {@link File}. Use this for tools on the desktop that don't need any of the
     * backends. Do not use this constructor in case you write something cross-platform. Use the {@link Files} interface instead.
     * @param file the file.
     */
    public Fi(File file){
        this.file = file;
        this.type = FileType.absolute;
    }

    public Fi(String fileName, FileType type){
        this.type = type;
        file = new File(fileName);
    }

    protected Fi(File file, FileType type){
        this.file = file;
        this.type = type;
    }

    public static Fi get(String path){
        return new Fi(path);
    }

    public static Fi tempFile(String prefix){
        try{
            return new Fi(File.createTempFile(prefix, null));
        }catch(IOException ex){
            throw new ArcRuntimeException("Unable to create temp file.", ex);
        }
    }

    public static Fi tempDirectory(String prefix){
        try{
            File file = File.createTempFile(prefix, null);
            if(!file.delete()) throw new IOException("Unable to delete temp file: " + file);
            if(!file.mkdir()) throw new IOException("Unable to create temp directory: " + file);
            return new Fi(file);
        }catch(IOException ex){
            throw new ArcRuntimeException("Unable to create temp file.", ex);
        }
    }

    private static void emptyDirectory(File file, boolean preserveTree){
        if(file.exists()){
            File[] files = file.listFiles();
            if(files != null){
                for(File value : files){
                    if(!value.isDirectory())
                        value.delete();
                    else if(preserveTree)
                        emptyDirectory(value, true);
                    else
                        deleteDirectory(value);
                }
            }
        }
    }

    private static boolean deleteDirectory(File file){
        emptyDirectory(file, false);
        return file.delete();
    }

    private static void copyFile(Fi source, Fi dest){
        try{
            dest.write(source.read(), false);
        }catch(Exception ex){
            throw new ArcRuntimeException("Error copying source file: " + source.file + " (" + source.type + ")\n" //
            + "To destination: " + dest.file + " (" + dest.type + ")", ex);
        }
    }

    private static void copyDirectory(Fi sourceDir, Fi destDir){
        destDir.mkdirs();
        Fi[] files = sourceDir.list();
        for(Fi srcFile : files){
            Fi destFile = destDir.child(srcFile.name());
            if(srcFile.isDirectory())
                copyDirectory(srcFile, destFile);
            else
                copyFile(srcFile, destFile);
        }
    }

    /**
     * @return the path of the file as specified on construction. Backward slashes will be replaced by forward slashes.
     */
    public String path(){
        return file.getPath().replace('\\', '/');
    }

    /** @return the absolute path to this file without backslashes.*/
    public String absolutePath(){
        return file.getAbsolutePath().replace('\\', '/');
    }

    /** @return the name of the file, without any parent paths. */
    public String name(){
        return file.getName().isEmpty() ? file.getPath() : file.getName();
    }

    /** @return whether this file's extension is equal to the specified string. */
    public boolean extEquals(String ext){
        return extension().equalsIgnoreCase(ext);
    }

    /** Returns the file extension (without the dot) or an empty string if the file name doesn't contain a dot. */
    public String extension(){
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if(dotIndex == -1) return "";
        return name.substring(dotIndex + 1);
    }

    /** @return the name of the file, without parent paths or the extension. */
    public String nameWithoutExtension(){
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if(dotIndex == -1) return name;
        return name.substring(0, dotIndex);
    }

    /**
     * @return the path and filename without the extension, e.g. dir/dir2/file.png -> dir/dir2/file. backward slashes will be
     * returned as forward slashes.
     */
    public String pathWithoutExtension(){
        String path = file.getPath().replace('\\', '/');
        int dotIndex = path.lastIndexOf('.');
        if(dotIndex == -1) return path;
        return path.substring(0, dotIndex);
    }

    public FileType type(){
        return type;
    }

    /**
     * Returns a java.io.File that represents this file handle. Note the returned file will only be usable for
     * {@link FileType#absolute} and {@link FileType#external} file handles.
     */
    public File file(){
        if(type == FileType.external) return new File(Core.files.getExternalStoragePath(), file.getPath());
        return file;
    }

    /** @return the sha256 hash of this file. */
    public byte[] sha256(){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try(InputStream fis = read(); DigestInputStream dis = new DigestInputStream(fis, digest)){
                byte[] buffer = new byte[8192];
                while(dis.read(buffer) != -1) ;
            }

            return digest.digest();
        }catch(IOException | NoSuchAlgorithmException e){
            throw new ArcRuntimeException(e);
        }
    }

    /**
     * Returns a stream for reading this file as bytes.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public InputStream read(){
        if(type == FileType.classpath || (type == FileType.internal && !file().exists())
        || (type == FileType.local && !file().exists())){
            InputStream input = Fi.class.getResourceAsStream("/" + file.getPath().replace('\\', '/'));
            if(input == null) throw new ArcRuntimeException("File not found: " + file + " (" + type + ")");
            return input;
        }
        try{
            return new FileInputStream(file());
        }catch(Exception ex){
            if(file().isDirectory())
                throw new ArcRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", ex);
            throw new ArcRuntimeException("Error reading file: " + file + " (" + type + ")", ex);
        }
    }

    /**
     * Returns a buffered stream for reading this file as bytes.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public BufferedInputStream read(int bufferSize){
        return new BufferedInputStream(read(), bufferSize);
    }

    /**
     * Returns a reader for reading this file as characters.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public Reader reader(){
        return reader("UTF-8");
    }

    /**
     * Returns a reader for reading this file as characters.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public Reader reader(String charset){
        InputStream stream = read();
        try{
            return new InputStreamReader(stream, charset);
        }catch(UnsupportedEncodingException ex){
            Streams.close(stream);
            throw new ArcRuntimeException("Error reading file: " + this, ex);
        }
    }

    /**
     * Returns a buffered reader for reading this file as characters.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public BufferedReader reader(int bufferSize){
        return reader(bufferSize, "UTF-8");
    }

    /**
     * Returns a buffered reader for reading this file as characters.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public BufferedReader reader(int bufferSize, String charset){
        try{
            return new BufferedReader(new InputStreamReader(read(), charset), bufferSize);
        }catch(UnsupportedEncodingException ex){
            throw new ArcRuntimeException("Error reading file: " + this, ex);
        }
    }

    /**
     * Reads the entire file into a string using the platform's default charset.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public String readString(){
        return readString("UTF-8");
    }

    /**
     * Reads the entire file into a string using the specified charset.
     * @param charset If null the default charset is used.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public String readString(String charset){
        StringBuilder output = new StringBuilder(estimateLength());
        InputStreamReader reader = null;
        try{
            if(charset == null)
                reader = new InputStreamReader(read());
            else
                reader = new InputStreamReader(read(), charset);
            char[] buffer = new char[256];
            while(true){
                int length = reader.read(buffer);
                if(length == -1) break;
                output.append(buffer, 0, length);
            }
        }catch(IOException ex){
            throw new ArcRuntimeException("Error reading layout file: " + this, ex);
        }finally{
            Streams.close(reader);
        }
        return output.toString();
    }

    /**
     * Reads the entire file into a byte array.
     * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    public byte[] readBytes(){
        InputStream input = read();
        try{
            return Streams.copyBytes(input, estimateLength());
        }catch(IOException ex){
            throw new ArcRuntimeException("Error reading file: " + this, ex);
        }finally{
            Streams.close(input);
        }
    }

    /** @return a new ByteArrayInputStream containing all the bytes in this file. */
    public ByteArrayInputStream readByteStream(){
        return new ByteArrayInputStream(readBytes());
    }

    private int estimateLength(){
        int length = (int)length();
        return length != 0 ? length : 512;
    }

    /**
     * Reads the entire file into the byte array. The byte array must be big enough to hold the file's data.
     * @param bytes the array to load the file into
     * @param offset the offset to start writing bytes
     * @param size the number of bytes to read, see {@link #length()}
     * @return the number of read bytes
     */
    public int readBytes(byte[] bytes, int offset, int size){
        InputStream input = read();
        int position = 0;
        try{
            while(true){
                int count = input.read(bytes, offset + position, size - position);
                if(count <= 0) break;
                position += count;
            }
        }catch(IOException ex){
            throw new ArcRuntimeException("Error reading file: " + this, ex);
        }finally{
            Streams.close(input);
        }
        return position - offset;
    }

    /**
     * Attempts to memory map this file in READ_ONLY mode. Android files must not be compressed.
     * @throws ArcRuntimeException if this file handle represents a directory, doesn't exist, or could not be read, or memory mapping fails, or is a {@link FileType#classpath} file.
     */
    public ByteBuffer map(){
        return map(MapMode.READ_ONLY);
    }

    /**
     * Attempts to memory map this file. Android files must not be compressed.
     * @throws ArcRuntimeException if this file handle represents a directory, doesn't exist, or could not be read, or memory mapping fails, or is a {@link FileType#classpath} file.
     */
    public ByteBuffer map(FileChannel.MapMode mode){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot map a classpath file: " + this);
        RandomAccessFile raf = null;
        try{
            raf = new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw");
            FileChannel fileChannel = raf.getChannel();
            ByteBuffer map = fileChannel.map(mode, 0, file.length());
            map.order(ByteOrder.nativeOrder());
            return map;
        }catch(Exception ex){
            throw new ArcRuntimeException("Error memory mapping file: " + this + " (" + type + ")", ex);
        }finally{
            Streams.close(raf);
        }
    }

    public Writes writes(boolean append){
        return new Writes(new DataOutputStream(write(append, Streams.defaultBufferSize)));
    }

    public Writes writes(){
        return writes(false);
    }

    public Reads reads(){
        return new Reads(new DataInputStream(read(Streams.defaultBufferSize)));
    }

    public Writes writesDeflate(){
        return new Writes(new DataOutputStream(new DeflaterOutputStream(write(false, Streams.defaultBufferSize))));
    }

    public Reads readsDeflate(){
        return new Reads(new DataInputStream(new InflaterInputStream(read(Streams.defaultBufferSize))));
    }

    public OutputStream write(){
        return write(false);
    }

    /**
     * Returns a stream for writing to this file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public OutputStream write(boolean append){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot write to a classpath file: " + file);
        if(type == FileType.internal) throw new ArcRuntimeException("Cannot write to an internal file: " + file);
        parent().mkdirs();
        try{
            return new FileOutputStream(file(), append);
        }catch(Exception ex){
            if(file().isDirectory())
                throw new ArcRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", ex);
            throw new ArcRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
        }
    }

    /**
     * Returns a buffered stream for writing to this file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @param bufferSize The size of the buffer.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public OutputStream write(boolean append, int bufferSize){
        return new BufferedOutputStream(write(append), bufferSize);
    }

    /**
     * Reads the remaining bytes from the specified stream and writes them to this file. The stream is closed. Parent directories
     * will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public void write(InputStream input, boolean append){
        OutputStream output = null;
        try{
            output = write(append);
            Streams.copy(input, output);
        }catch(Exception ex){
            throw new ArcRuntimeException("Error stream writing to file: " + file + " (" + type + ")", ex);
        }finally{
            Streams.close(input);
            Streams.close(output);
        }

    }

    /**
     * Returns a writer for writing to this file using the default charset. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public Writer writer(boolean append){
        return writer(append, "UTF-8");
    }

    /**
     * Returns a writer for writing to this file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @param charset May be null to use the default charset.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public Writer writer(boolean append, String charset){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot write to a classpath file: " + file);
        if(type == FileType.internal) throw new ArcRuntimeException("Cannot write to an internal file: " + file);
        parent().mkdirs();
        try{
            FileOutputStream output = new FileOutputStream(file(), append);
            if(charset == null)
                return new OutputStreamWriter(output);
            else
                return new OutputStreamWriter(output, charset);
        }catch(IOException ex){
            if(file().isDirectory())
                throw new ArcRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", ex);
            throw new ArcRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
        }
    }

    /** {@see PixmapIO#writePNG(FileHandle, Pixmap)}*/
    public void writePng(Pixmap pixmap){
        PixmapIO.writePng(this, pixmap);
    }

    /**
     * Writes a string without appending it.
     * @see #writeString(String, boolean)
     */
    public void writeString(String string){
        writeString(string, false);
    }

    /**
     * Writes the specified string to the file using the default charset. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public void writeString(String string, boolean append){
        writeString(string, append, "UTF-8");
    }

    /**
     * Writes the specified string to the file using the specified charset. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @param charset May be null to use the default charset.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public void writeString(String string, boolean append, String charset){
        Writer writer = null;
        try{
            writer = writer(append, charset);
            writer.write(string);
        }catch(Exception ex){
            throw new ArcRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
        }finally{
            Streams.close(writer);
        }
    }

    public void writeBytes(byte[] bytes){
        writeBytes(bytes, false);
    }

    /**
     * Writes the specified bytes to the file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public void writeBytes(byte[] bytes, boolean append){
        OutputStream output = write(append);
        try{
            output.write(bytes);
        }catch(IOException ex){
            throw new ArcRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
        }finally{
            Streams.close(output);
        }
    }

    /**
     * Writes the specified bytes to the file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link FileType#classpath} or
     * {@link FileType#internal} file, or if it could not be written.
     */
    public void writeBytes(byte[] bytes, int offset, int length, boolean append){
        OutputStream output = write(append);
        try{
            output.write(bytes, offset, length);
        }catch(IOException ex){
            throw new ArcRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
        }finally{
            Streams.close(output);
        }
    }

    /** Recursively iterates through all files in this directory.
     * Directories are not handled.*/
    public void walk(Cons<Fi> cons){
        if(isDirectory()){
            for(Fi file : list()){
                file.walk(cons);
            }
        }else{
            cons.get(this);
        }
    }

    /** Recursively iterates through all files in this directory and adds them to an array.
     * Directories are not handled. */
    public Seq<Fi> findAll(Boolf<Fi> test){
        Seq<Fi> out = new Seq<>();
        walk(f -> {
            if(test.get(f)){
                out.add(f);
            }
        });
        return out;
    }

    /** Recursively iterates through all files in this directory and adds them to a newly allocated array.*/
    public Seq<Fi> findAll(){
        Seq<Fi> out = new Seq<>();
        walk(out::add);
        return out;
    }

    /** Equivalent to {@link #list()}, but returns a Seq. */
    public Seq<Fi> seq(){
        return Seq.with(list());
    }

    /**
     * Returns the paths to the children of this directory. Returns an empty list if this file handle represents a file and not a
     * directory. On the desktop, an {@link FileType#internal} handle to a directory on the classpath will return a zero length
     * array.
     * @throws ArcRuntimeException if this file is an {@link FileType#classpath} file.
     */
    public Fi[] list(){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot list a classpath directory: " + file);
        String[] relativePaths = file().list();
        if(relativePaths == null) return new Fi[0];
        Fi[] handles = new Fi[relativePaths.length];
        for(int i = 0, n = relativePaths.length; i < n; i++)
            handles[i] = child(relativePaths[i]);
        return handles;
    }

    /**
     * Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file
     * handle represents a file and not a directory. On the desktop, an {@link FileType#internal} handle to a directory on the
     * classpath will return a zero length array.
     * @param filter the {@link FileFilter} to filter files
     * @throws ArcRuntimeException if this file is an {@link FileType#classpath} file.
     */
    public Fi[] list(FileFilter filter){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot list a classpath directory: " + file);
        File file = file();
        String[] relativePaths = file.list();
        if(relativePaths == null) return new Fi[0];
        Fi[] handles = new Fi[relativePaths.length];
        int count = 0;
        for(String path : relativePaths){
            Fi child = child(path);
            if(!filter.accept(child.file())) continue;
            handles[count] = child;
            count++;
        }
        if(count < relativePaths.length){
            Fi[] newHandles = new Fi[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    /**
     * Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file
     * handle represents a file and not a directory. On the desktop, an {@link FileType#internal} handle to a directory on the
     * classpath will return a zero length array.
     * @param filter the {@link FilenameFilter} to filter files
     * @throws ArcRuntimeException if this file is an {@link FileType#classpath} file.
     */
    public Fi[] list(FilenameFilter filter){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot list a classpath directory: " + file);
        File file = file();
        String[] relativePaths = file.list();
        if(relativePaths == null) return new Fi[0];
        Fi[] handles = new Fi[relativePaths.length];
        int count = 0;
        for(String path : relativePaths){
            if(!filter.accept(file, path)) continue;
            handles[count] = child(path);
            count++;
        }
        if(count < relativePaths.length){
            Fi[] newHandles = new Fi[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    /**
     * Returns the paths to the children of this directory with the specified suffix. Returns an empty list if this file handle
     * represents a file and not a directory. On the desktop, an {@link FileType#internal} handle to a directory on the classpath
     * will return a zero length array.
     * @throws ArcRuntimeException if this file is an {@link FileType#classpath} file.
     */
    public Fi[] list(String suffix){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot list a classpath directory: " + file);
        String[] relativePaths = file().list();
        if(relativePaths == null) return new Fi[0];
        Fi[] handles = new Fi[relativePaths.length];
        int count = 0;
        for(String path : relativePaths){
            if(!path.endsWith(suffix)) continue;
            handles[count] = child(path);
            count++;
        }
        if(count < relativePaths.length){
            Fi[] newHandles = new Fi[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    /**
     * Returns true if this file is a directory. Always returns false for classpath files. On Android, an
     * {@link FileType#internal} handle to an empty directory will return false. On the desktop, an {@link FileType#internal}
     * handle to a directory on the classpath will return false.
     */
    public boolean isDirectory(){
        if(type == FileType.classpath) return false;
        return file().isDirectory();
    }

    /** Returns a handle to the child with the specified name. */
    public Fi child(String name){
        if(file.getPath().length() == 0) return new Fi(new File(name), type);
        return new Fi(new File(file, name), type);
    }

    /**
     * Returns a handle to the sibling with the specified name.
     * @throws ArcRuntimeException if this file is the root.
     */
    public Fi sibling(String name){
        if(file.getPath().length() == 0) throw new ArcRuntimeException("Cannot get the sibling of the root.");
        return new Fi(new File(file.getParent(), name), type);
    }

    public Fi parent(){
        File parent = file.getParentFile();
        if(parent == null){
            if(OS.isWindows){
                return new Fi("", type){
                    Fi[] children = Seq.with(File.listRoots()).map(Fi::new).toArray(Fi.class);

                    @Override
                    public Fi parent(){
                        return this;
                    }

                    @Override
                    public boolean isDirectory(){
                        return true;
                    }

                    @Override
                    public boolean exists(){
                        return true;
                    }

                    @Override
                    public Fi child(String name){
                        return new Fi(new File(name));
                    }

                    @Override
                    public Fi[] list(){
                        return children;
                    }

                    @Override
                    public Fi[] list(FileFilter filter){
                        return Seq.select(list(), f -> filter.accept(f.file)).toArray(Fi.class);
                    }
                };
            }else{
                if(type == FileType.absolute){
                    parent = new File("/");
                }else{
                    parent = new File("");
                }
            }
        }
        return new Fi(parent, type);
    }

    /** @throws ArcRuntimeException if this file handle is a {@link FileType#classpath} or {@link FileType#internal} file. */
    public boolean mkdirs(){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot mkdirs with a classpath file: " + file);
        if(type == FileType.internal) throw new ArcRuntimeException("Cannot mkdirs with an internal file: " + file);
        return file().mkdirs();
    }

    /**
     * Returns true if the file exists. On Android, a {@link FileType#classpath} or {@link FileType#internal} handle to a
     * directory will always return false. Note that this can be very slow for internal files on Android!
     */
    public boolean exists(){
        switch(type){
            case internal:
                if(file().exists()) return true;
                // Fall through.
            case classpath:
                return Fi.class.getResource("/" + file.getPath().replace('\\', '/')) != null;
        }
        return file().exists();
    }

    /**
     * Deletes this file or empty directory and returns success. Will not delete a directory that has children.
     * @throws ArcRuntimeException if this file handle is a {@link FileType#classpath} or {@link FileType#internal} file.
     */
    public boolean delete(){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot delete a classpath file: " + file);
        if(type == FileType.internal) throw new ArcRuntimeException("Cannot delete an internal file: " + file);
        return file().delete();
    }

    /**
     * Deletes this file or directory and all children, recursively.
     * @throws ArcRuntimeException if this file handle is a {@link FileType#classpath} or {@link FileType#internal} file.
     */
    public boolean deleteDirectory(){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot delete a classpath file: " + file);
        if(type == FileType.internal) throw new ArcRuntimeException("Cannot delete an internal file: " + file);
        return deleteDirectory(file());
    }

    /**
     * Deletes all children of this directory, recursively.
     * @throws ArcRuntimeException if this file handle is a {@link FileType#classpath} or {@link FileType#internal} file.
     */
    public void emptyDirectory(){
        emptyDirectory(false);
    }

    /**
     * Deletes all children of this directory, recursively. Optionally preserving the folder structure.
     * @throws ArcRuntimeException if this file handle is a {@link FileType#classpath} or {@link FileType#internal} file.
     */
    public void emptyDirectory(boolean preserveTree){
        if(type == FileType.classpath) throw new ArcRuntimeException("Cannot delete a classpath file: " + file);
        if(type == FileType.internal) throw new ArcRuntimeException("Cannot delete an internal file: " + file);
        emptyDirectory(file(), preserveTree);
    }

    /**
     * Copies this file or directory to the specified file or directory. If this handle is a file, then 1) if the destination is a
     * file, it is overwritten, or 2) if the destination is a directory, this file is copied into it, or 3) if the destination
     * doesn't exist, {@link #mkdirs()} is called on the destination's parent and this file is copied into it with a new name. If
     * this handle is a directory, then 1) if the destination is a file, ArcRuntimeException is thrown, or 2) if the destination is
     * a directory, this directory is copied into it recursively, overwriting existing files, or 3) if the destination doesn't
     * exist, {@link #mkdirs()} is called on the destination and this directory is copied into it recursively.
     * @throws ArcRuntimeException if the destination file handle is a {@link FileType#classpath} or {@link FileType#internal}
     * file, or copying failed.
     */
    public void copyTo(Fi dest){
        if(!isDirectory()){
            if(dest.isDirectory()) dest = dest.child(name());
            copyFile(this, dest);
            return;
        }
        if(dest.exists()){
            if(!dest.isDirectory()) throw new ArcRuntimeException("Destination exists but is not a directory: " + dest);
        }else{
            dest.mkdirs();
            if(!dest.isDirectory()) throw new ArcRuntimeException("Destination directory cannot be created: " + dest);
        }
        copyDirectory(this, dest.child(name()));
    }

    /**
     * Copies the contents of this folder into another folder. Unlike copyTo, this only copies the *contents*, not this folder itself.
     * @throws ArcRuntimeException if this or {@param dest} is not a valid directory, or copying fails.
     * */
    public void copyFilesTo(Fi dest){
        if(!isDirectory()) throw new ArcRuntimeException("Source folder must be a directory: " + this);
        if(dest.exists() && !dest.isDirectory()) throw new ArcRuntimeException("Destination folder must be a directory: " + dest);

        dest.mkdirs();

        copyDirectory(this, dest);
    }

    /**
     * Moves this file to the specified file, overwriting the file if it already exists.
     * @throws ArcRuntimeException if the source or destination file handle is a {@link FileType#classpath} or
     * {@link FileType#internal} file.
     */
    public void moveTo(Fi dest){
        switch(type){
            case classpath:
                throw new ArcRuntimeException("Cannot move a classpath file: " + file);
            case internal:
                throw new ArcRuntimeException("Cannot move an internal file: " + file);
            case absolute:
            case external:
                // Try rename for efficiency and to change case on case-insensitive file systems.
                if(file().renameTo(dest.file())) return;
        }
        copyTo(dest);
        delete();
        if(exists() && isDirectory()) deleteDirectory();
    }

    /**
     * Returns the length in bytes of this file, or 0 if this file is a directory, does not exist, or the size cannot otherwise be
     * determined.
     */
    public long length(){
        if(type == FileType.classpath || (type == FileType.internal && !file.exists())){
            InputStream input = read();
            try{
                return input.available();
            }catch(Exception ignored){
            }finally{
                Streams.close(input);
            }
            return 0;
        }
        return file().length();
    }

    /**
     * Returns the last modified time in milliseconds for this file. Zero is returned if the file doesn't exist. Zero is returned
     * for {@link FileType#classpath} files. On Android, zero is returned for {@link FileType#internal} files. On the desktop, zero
     * is returned for {@link FileType#internal} files on the classpath.
     */
    public long lastModified(){
        return file().lastModified();
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Fi)) return false;
        Fi other = (Fi)obj;
        return type == other.type && path().equals(other.path());
    }

    @Override
    public int hashCode(){
        int hash = 1;
        hash = hash * 37 + type.hashCode();
        hash = hash * 67 + path().hashCode();
        return hash;
    }

    @Override
    public int compareTo(Fi fi){
        return file.compareTo(fi.file);
    }

    @Override
    public String toString(){
        return file.getPath().replace('\\', '/');
    }
}
