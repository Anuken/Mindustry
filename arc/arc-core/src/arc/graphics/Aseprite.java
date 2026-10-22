package arc.graphics;

import arc.struct.*;

import java.io.*;
import java.nio.charset.*;
import java.util.zip.*;

/** Reads Aseprite files - https://github.com/aseprite/aseprite/blob/main/docs/ase-file-specs.md. Supports RGBA and indexed (8bpp, palette-based) color modes. No grayscale or tilemaps. */
public class Aseprite{

    /** Reads an Aseprite file from a stream. Does not close the stream. The stream is wrapped in a buffered stream. */
    public static AseImage read(InputStream stream) throws IOException{
        AseInput in = new AseInput(new BufferedInputStream(stream));

        in.u32(); //file size

        if(in.u16() != 0xA5E0) throw new IOException("Invalid header, not an ASE file?");

        int frameCount = in.u16(), width = in.u16(), height = in.u16(), colorDepth = in.u16();
        boolean validOpacity = in.u32() == 1;

        if(colorDepth != 32 && colorDepth != 8) throw new IOException("Only RGBA (32-bit) and indexed (8-bit) aseprite files are supported.");

        boolean indexed = colorDepth == 8;
        int bytesPerPixel = indexed ? 1 : 4;

        in.u16(); //speed, deprecated
        in.u32(); //0
        in.u32(); //0

        int transparentIndex = in.u8(); //palette entry representing transparent color, only meaningful for indexed sprites
        in.skipFully(3); //ignore

        in.u16(); //color number
        in.u8(); //width of a pixel
        in.u8(); //height of a pixel
        in.i16(); //grid X
        in.i16(); //grid Y
        in.u16(); //grid width
        in.u16(); //grid height

        in.skipFully(84); //header padding

        Seq<AseLayer> layers = new Seq<>(), rootLayers = new Seq<>(), groupStack = new Seq<>();
        Seq<AseTag> tags = new Seq<>();
        Seq<AseFrame> frames = new Seq<>();
        boolean justReadTags = false;
        int readTagIndex = 0, lastDepth = 0;

        //palette for indexed sprites. Capped at 256 entries, since pixel indices are a single byte.
        //Packed as RGBA8888, i.e. (r << 24) | (g << 16) | (b << 8) | a.
        int[] palette = new int[256];
        boolean hasNewPalette = false;

        for(int frameId = 0; frameId < frameCount; frameId++){
            in.u32(); //frame total bytes

            if(in.u16() != 0xF1FA) throw new IOException("Invalid frame magic (corrupt file?)");

            int chunksOld = in.u16(), durationMs = in.u16();

            in.u16(); //unused

            int chunksNew = in.u32(), chunks = chunksNew == 0 ? chunksOld : chunksNew;

            boolean readImage = false;
            AseFrame frame = new AseFrame();
            frame.duration = durationMs;

            for(int chunkId = 0; chunkId < chunks; chunkId++){
                int chunkSize = in.u32(), chunkType = in.u16();

                if(chunkType == 0x2004){ //layer
                    justReadTags = false;

                    int flags = in.u16(), layerType = in.u16(), childLevel = in.u16();

                    in.u32(); //width/height, ignored
                    in.u16(); //blend mode, ignored

                    int opacity = in.u8();

                    in.skipFully(3); //skip 3 bytes, unused

                    String name = in.string();

                    if(layerType == 2){
                        in.u32(); //tileset index, don't care
                    }

                    AseLayer layer = new AseLayer();
                    layer.index = layers.size;
                    layer.opacity = validOpacity ? opacity : 255;
                    layer.name = name;
                    layer.flags = flags;
                    layer.kind = AseLayerType.all[layerType];

                    layers.add(layer);

                    if(childLevel < lastDepth){
                        for(int i = 0; i < lastDepth - childLevel; i++){
                            if(groupStack.size > 0) groupStack.pop();
                        }
                    }

                    lastDepth = childLevel;

                    if(childLevel > 0 && groupStack.size > 0){
                        groupStack.peek().children.add(layer);
                    }else{
                        rootLayers.add(layer);
                    }

                    if(layer.kind == AseLayerType.group){
                        groupStack.add(layer);
                    }

                }else if(chunkType == 0x2018){ //tags
                    justReadTags = true;

                    int count = in.u16();
                    in.skipFully(8);
                    for(int i = 0; i < count; i++){
                        AseTag tag = new AseTag();
                        tag.frameFrom = in.u16();
                        tag.frameTo = in.u16();
                        tag.direction = AseLoopDirection.all[in.u8()];
                        tag.repeat = in.u16();

                        in.skipFully(10); //unused

                        tag.name = in.string();

                        tags.add(tag);
                    }
                }else if(chunkType == 0x2020 && (layers.size > 0 || justReadTags)){ //user data (horrible implementation)
                    int flags = in.u32();

                    if((flags & 1) == 1){ //text
                        String text = in.string();
                        if(!justReadTags){
                            layers.peek().userData = text;
                        }else{
                            tags.get(readTagIndex).userData = text;
                        }
                    }

                    if((flags & 2) == 2){ //color
                        int color = in.u32();
                        if(!justReadTags){
                            layers.peek().userColor = color;
                        }else{
                            tags.get(readTagIndex).userColor = color;
                        }
                    }

                    //other flags not supported! will crash!

                    if(justReadTags){
                        readTagIndex++;
                    }
                }else if(chunkType == 0x2019){ //new palette chunk
                    justReadTags = false;

                    int newSize = in.u32(), from = in.u32(), to = in.u32();

                    in.skipFully(8);

                    if(!hasNewPalette){
                        java.util.Arrays.fill(palette, 0);
                        hasNewPalette = true;
                    }

                    for(int i = from; i <= to; i++){
                        int entryFlags = in.u16();
                        int r = in.u8(), g = in.u8(), b = in.u8(), a = in.u8();

                        if(i >= 0 && i < palette.length){
                            palette[i] = (r << 24) | (g << 16) | (b << 8) | a;
                        }

                        if((entryFlags & 1) == 1) in.string(); //color name, unused
                    }
                }else if((chunkType == 0x0004 || chunkType == 0x0011) && !hasNewPalette){ //old palette chunks, only used if no new palette chunk is present
                    justReadTags = false;

                    boolean sixBit = chunkType == 0x0011;
                    int packets = in.u16();
                    int index = 0;

                    for(int p = 0; p < packets; p++){
                        int skip = in.u8();
                        int count = in.u8();
                        int num = count == 0 ? 256 : count;

                        index += skip;

                        for(int i = 0; i < num; i++){
                            int r = in.u8(), g = in.u8(), b = in.u8();

                            if(sixBit){
                                r = r * 255 / 63;
                                g = g * 255 / 63;
                                b = b * 255 / 63;
                            }

                            if(index >= 0 && index < palette.length){
                                palette[index] = (r << 24) | (g << 16) | (b << 8) | 255;
                            }

                            index++;
                        }
                    }
                }else if(chunkType == 0x2005){ //cel (image data)
                    justReadTags = false;
                    readImage = true;

                    int layerIndex = in.u16(), x = in.i16(), y = in.i16(), opacity = in.u8(), celType = in.u16();

                    in.skipFully(7); //reserved

                    //TODO figure out links later
                    if(celType != 2) throw new IOException("Only compressed image data is allowed - tilemaps, links and raw images are not supported.");

                    int pixWidth = in.u16(), pixHeight = in.u16();
                    //base size - 6 byte header - 2 byte index - 4 bytes xy - 1 byte opacity - 2 bytes type - 4 bytes size - 7 bytes padding
                    byte[] compressed = new byte[chunkSize - 6 - 2 - 4 - 1 - 2 - 4 - 7];

                    in.readFully(compressed);

                    byte[] data = inflate(compressed, pixWidth * pixHeight * bytesPerPixel);

                    if(indexed){
                        AseLayer layer = layers.get(layerIndex);
                        boolean background = (layer.flags & AseLayer.background) != 0;
                        data = indexedToRgba(data, palette, transparentIndex, background);
                    }

                    AseCel cel = new AseCel();
                    cel.layerIndex = layerIndex;
                    cel.data = data;
                    cel.x = x;
                    cel.y = y;
                    cel.width = pixWidth;
                    cel.height = pixHeight;
                    cel.opacity = opacity;

                    frame.layers.add(cel);
                }else{ //unknown chunk, skipping - I don't support tilemaps, so they do not matter
                    in.skipFully(chunkSize - 6);
                }
            }

            if(!readImage){
                //add dummy empty 0x0 cel for every layer
                for(int i = 0; i < layers.size; i++){
                    AseCel cel = new AseCel();
                    cel.layerIndex = i;
                    cel.opacity = 255;

                    frame.layers.add(cel);
                }
            }

            frames.add(frame);
        }

        AseImage image = new AseImage();
        image.layers = layers;
        image.rootLayers = rootLayers;
        image.tags = tags;
        image.frames = frames;
        image.width = width;
        image.height = height;
        image.colorDepth = colorDepth;
        image.palette = palette;
        image.transparentIndex = transparentIndex;
        return image;
    }

    private static byte[] inflate(byte[] input, int size) throws IOException{
        Inflater inflater = new Inflater();
        try{
            inflater.setInput(input);

            byte[] result = new byte[size];
            int offset = 0;
            while(offset < size){
                int read = inflater.inflate(result, offset, size - offset);
                if(read <= 0) break;
                offset += read;
            }
            return result;
        }catch(DataFormatException e){
            throw new IOException(e);
        }finally{
            inflater.end();
        }
    }

    /** Converts a buffer of single-byte palette indices into RGBA8888 pixel data (4 bytes per pixel: R, G, B, A).
     * Pixels whose index matches transparentIndex are forced fully transparent, unless the layer is a background layer,
     * matching the aseprite spec's handling of the transparent color index. */
    private static byte[] indexedToRgba(byte[] indices, int[] palette, int transparentIndex, boolean background){
        byte[] out = new byte[indices.length * 4];

        for(int i = 0; i < indices.length; i++){
            int index = indices[i] & 0xFF;
            int color = index < palette.length ? palette[index] : 0;
            int o = i * 4;

            out[o] = (byte)(color >>> 24);
            out[o + 1] = (byte)(color >>> 16);
            out[o + 2] = (byte)(color >>> 8);
            out[o + 3] = (index == transparentIndex && !background) ? 0 : (byte)color;
        }

        return out;
    }

    /** Little-endian reads, as used by the ASE format. */
    static class AseInput extends DataInputStream{

        AseInput(InputStream in){
            super(in);
        }

        int u8() throws IOException{
            return readUnsignedByte();
        }

        int u16() throws IOException{
            return readUnsignedByte() | readUnsignedByte() << 8;
        }

        int i16() throws IOException{
            return (short)u16();
        }

        int u32() throws IOException{
            return u16() | u16() << 16;
        }

        void skipFully(int len) throws IOException{
            while(len > 0){
                int skipped = skipBytes(len);
                if(skipped <= 0){
                    readByte(); //throws at EOF
                    skipped = 1;
                }
                len -= skipped;
            }
        }

        String string() throws IOException{
            byte[] bytes = new byte[u16()];
            readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public enum AseLayerType{
        image, group, tilemap;

        public static final AseLayerType[] all = values();
    }

    public enum AseLoopDirection{
        forward, reverse, pingPong, pingPongReverse;

        public static final AseLoopDirection[] all = values();
    }

    public static class AseTag{
        public int frameFrom, frameTo;
        public AseLoopDirection direction;
        public int repeat;
        public String name = "";
        public String userData = "";
        /** RGBA8888 */
        public int userColor;
    }

    /** One animation frame, containing the cel (image) data for every layer that has one in this frame. */
    public static class AseFrame{
        public int duration;
        public Seq<AseCel> layers = new Seq<>();
    }

    /** A single layer's image data within one frame. */
    public static class AseCel{
        /** Index into AseImage#layers identifying which layer this cel belongs to. */
        public int layerIndex;
        public byte[] data;
        public int width, height;
        public int x, y;
        public int opacity;
    }

    public static class AseLayer{
        public static final int
        visible = 1, editable = 2, lockMovement = 4, background = 8,
        preferLinkedCels = 16, collapsed = 32, reference = 64;

        public int flags;
        public String name = "";
        public int index;
        public int opacity;
        public AseLayerType kind;
        public String userData = "";
        /** RGBA8888 */
        public int userColor;
        public Seq<AseLayer> children = new Seq<>();
    }

    public static class AseImage{
        public Seq<AseLayer> layers, rootLayers;
        public Seq<AseTag> tags;
        public Seq<AseFrame> frames;
        public int width, height;
        public int colorDepth;
        /** Only populated for indexed (8bpp) sprites; packed as RGBA8888. Unused for RGBA sprites. */
        public int[] palette;
        /** Palette entry index representing transparent color in non-background layers. Only meaningful for indexed sprites. */
        public int transparentIndex;
    }
}