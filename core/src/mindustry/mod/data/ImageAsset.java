package mindustry.mod.data;

import arc.files.*;
import mindustry.mod.*;

import java.io.*;

public class ImageAsset extends DataAsset implements Comparable<ImageAsset>{
    /** Size of encoded image. */
    public int width, height;
    /** Encoded PNG data. */
    public byte[] data;

    public ImageAsset(String path, int width, int height, byte[] data){
        Fi file = new Fi(path);
        this.name = file.nameWithoutExtension();
        this.path = file.pathWithoutExtension();
        this.width = width;
        this.height = height;
        this.data = data;
    }

    ImageAsset(){}

    public static ImageAsset fromFile(String relativePath, Fi file) throws IOException{
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
        return new ImageAsset(relativePath, width, height, data);
    }

    @Override
    void read(DataInput stream) throws IOException{
        width = stream.readShort();
        height = stream.readShort();
        data = new byte[stream.readInt()];
        stream.readFully(data);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        stream.writeShort(width);
        stream.writeShort(height);
        stream.writeInt(data.length);
        stream.write(data);
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.image;
    }

    @Override
    public int compareTo(ImageAsset imageAsset){
        return path.compareTo(imageAsset.path);
    }
}
