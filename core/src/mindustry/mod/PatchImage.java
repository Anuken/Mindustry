package mindustry.mod;

import arc.files.*;

import java.io.*;

public class PatchImage implements Comparable<PatchImage>{
    /** Image name without extension; does not contain packing prefix. */
    public String name;
    /** Image path, excluding extension. */
    public String path;
    /** Size of encoded image. */
    public int width, height;
    /** Encoded PNG data. */
    public byte[] data;

    public PatchImage(String path, int width, int height, byte[] data){
        Fi file = new Fi(path);
        this.name = file.nameWithoutExtension();
        this.path = file.pathWithoutExtension();
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public static PatchImage fromFile(String relativePath, Fi file) throws IOException{
        byte[] data = file.readBytes();
        int width, height;
        //perform basic validation and fetch size from IHDR chunk
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))){
            long header = in.readLong();
            if(header != 0x89504e470d0a1a0aL){
                throw new IOException("File is not a PNG.");
            }
            in.readInt(); //length
            int type = in.readInt(); //chunk type
            if(type != 0x49484452) throw new IOException("PNG files must begin with a IHDR chunk.");
            width = in.readInt();
            height = in.readInt();
            if(width <= 0 || height <= 0) throw new IOException("PNG size must be positive.");
            if(width > DataPatcher.maxImageSize || height > DataPatcher.maxImageSize) throw new IOException("PNG is larger than maximum image size (" + DataPatcher.maxImageSize + "x" + DataPatcher.maxImageSize + ")");
        }
        return new PatchImage(relativePath, width, height, data);
    }

    @Override
    public int compareTo(PatchImage patchImage){
        return path.compareTo(patchImage.path);
    }
}
