package mindustry.io;

import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.storage.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

/** Reads and writes map files. */
public class MapIO{
    private static final int[] pngHeader = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static boolean isImage(Fi file){
        try(InputStream stream = file.read(32)){
            for(int i1 : pngHeader){
                if(stream.read() != i1){
                    return false;
                }
            }
            return true;
        }catch(IOException e){
            return false;
        }
    }

    public static Map createMap(Fi file, boolean custom) throws IOException{
        try(InputStream is = new InflaterInputStream(file.read(bufferSize)); CounterInputStream counter = new CounterInputStream(is); DataInputStream stream = new DataInputStream(counter)){
            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            if(ver == null) throw new IOException("Unknown save version: " + version + ". Are you trying to load a save from a newer version?");
            StringMap tags = new StringMap();
            ver.readRegion("meta", stream, counter, in -> tags.putAll(ver.readStringMap(in)));
            return new Map(file, tags.getInt("width"), tags.getInt("height"), tags, custom, version, Version.build);
        }
    }

    public static void writeMap(Fi file, Map map) throws Throwable{
        writeMap(file, map, true);
    }

    /** @param embed if true, assets will be embedded in the map - this is needed for external export. */
    public static void writeMap(Fi file, Map map, boolean embed) throws Throwable{
        SaveIO.write(file, new SaveOptions(){{
            extraTags = map.tags;
            embedAssets = embed;
        }});
    }

    public static void loadMap(Map map) throws SaveLoadException{
        SaveIO.load(map.file, new DefaultWorldContext());
    }

    public static void loadMap(Map map, SaveLoadContext cons) throws SaveLoadException{
        SaveIO.load(map.file, cons);
    }

    /** Per-tile flags retained while reading a map for preview; used by the shading pass. */
    private static final byte flagSolid = 1, flagLiquid = 2, flagDark = 4, flagAir = 8;

    /**
     * Generates a preview of a map. This is mostly thread safe, but writes to map spawns/teams in the same method, which is a bit risky, but I don't have any better ideas.
     * There's no way (that I can see) of checking teams/spawns in a map without reading every tile.
     * After I wrote this comment, I added both to meta, but it's too late now; all older saves don't have team/spawn info, so it has to be recalculated every time and cached in a dat file next to the preview.
     * Removing spawns/teams isn't an option either, as people would become unable to filter maps by supported gamemode.
     * */
    public static Pixmap generatePreview(Map map) throws IOException{
        map.spawns = 0;
        map.teams.clear();

        try(InputStream is = new InflaterInputStream(map.file.read(bufferSize)); CounterInputStream counter = new CounterInputStream(is); DataInputStream stream = new DataInputStream(counter)){
            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            if(ver == null) throw new IOException("Unknown save version: " + version + ". Are you trying to load a save from a newer version?");
            ver.readRegion("meta", stream, counter, ver::readStringMap);

            final int width = map.width, height = map.height;
            final int len = width*height;
            final short[] floorIds = new short[len], overlayIds = new short[len];
            //unshaded color of each tile (minimap logic: wall/building/floor/overlay)
            final int[] colors = new int[len];
            final byte[] flags = new byte[len];

            var tile = new CachedTile(){
                /** Index of the last tile that had setBlock called on it, i.e. whose block is actually loaded in this shared instance. */
                int blockIndex = -1;

                /** Computes color + flags for the tile currently loaded in this shared instance. Safe to call multiple times as more data is read. */
                void record(){
                    int idx = x + y * width;
                    if(idx != blockIndex) return;

                    int color = previewColor(this);
                    byte flag = flagsFor(block, floor, isDarkened());
                    colors[idx] = color;
                    flags[idx] = flag;
                }

                @Override
                public void setBlock(Block type){
                    //do not super.setBlock as that affects the current world; previews never create buildings
                    this.block = type;
                    this.build = null;

                    this.data = 0;
                    this.floorData = 0;
                    this.overlayData = 0;
                    this.extraData = 0;
                    //floor/overlay are not stored in this instance when reading, so look them up
                    int idx = x + y * width;
                    this.blockIndex = idx;
                    this.floor = (Floor)content.block(floorIds[idx]);
                    this.overlay = (Floor)content.block(overlayIds[idx]);

                    record();
                }
            };

            //version 12 has content patches here, version 11 has them after the content header
            var context = new SaveLoadContext(){
                {
                    preview = true;
                }

                @Override public void resize(int width, int height){}
                @Override public boolean isGenerating(){return false;}
                //must not touch the global state, as previews can run off the main thread
                @Override public void begin(){}
                @Override public void end(){}

                @Override
                public void onReadPreviewBuilding(Team team){
                    //multiblocks only call setBlock on their center tile, so every tile of the footprint gets the team color
                    //and the block's flags here; shadows/darkness then see the multiblock as a whole
                    int c = team.color.rgba();
                    Block block = tile.block();
                    boolean dark = tile.isDarkened();
                    int size = block.size;
                    int offset = -(size - 1) / 2;

                    for(int dx = 0; dx < size; dx++){
                        for(int dy = 0; dy < size; dy++){
                            int px = tile.x + dx + offset, py = tile.y + dy + offset;
                            if(px < 0 || py < 0 || px >= width || py >= height) continue;

                            int idx = px + py * width;
                            colors[idx] = c;
                            flags[idx] = flagsFor(block, (Floor)content.block(floorIds[idx]), dark);
                        }
                    }

                    if(tile.block() instanceof CoreBlock){
                        map.teams.add(team.id);
                    }
                }

                @Override
                public Tile tile(int index){
                    tile.x = (short)(index % width);
                    tile.y = (short)(index / width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID){
                    int idx = x + y * width;
                    floorIds[idx] = (short)floorID;
                    overlayIds[idx] = (short)overlayID;

                    if(content.block(overlayID) == Blocks.spawn){
                        map.spawns ++;
                    }
                    //default to air over this floor. Older save versions never call setBlock for runs of air,
                    //and multiblock parts never get it either, so this is the only place those tiles get a color
                    tile.x = (short)x;
                    tile.y = (short)y;
                    tile.setBlock(Blocks.air);
                    return tile;
                }

                @Override
                public void onReadTileData(){
                    //data (extraData for colored walls, etc.) is now available, so recompute
                    tile.record();
                }
            };

            if(ver.version >= 12) ver.skipChunk(stream);
            ver.readRegion("content", stream, counter, in -> readPreviewContentHeader(in, context));
            if(ver.version == 11) ver.skipChunk(stream);
            ver.readRegion("preview_map", stream, counter, in -> ver.readMap(in, context));

            return shadePreview(map, width, height, colors, flags);
        }
    }

    private static byte flagsFor(Block block, Floor floor, boolean dark){
        int f = 0;
        if(block == Blocks.air) f |= flagAir;
        if(block.solid) f |= flagSolid;
        if(floor.isLiquid) f |= flagLiquid;
        if(dark) f |= flagDark;
        return (byte)f;
    }

    /** Second pass: applies darkness, shadows and shore shading exactly like MinimapRenderer.colorFor. */
    private static Pixmap shadePreview(Map map, int width, int height, int[] colors, byte[] flags){
        byte[] darkness = computeDarkness(flags, width, height);
        Pixmap pixmap = new Pixmap(width, height);
        Color color = new Color();

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int idx = x + y * width;
                int flag = flags[idx];

                //map limit doesn't apply (it would look bad in previews)
                //assume borderDarkness is true in rules (parsing rules just for this rarely-changed flag is a waste)
                float dark = borderDarkness(x, y, width, height, false, 0, 0, 0, 0);
                if((flag & flagDark) != 0){
                    dark = Math.max(dark, darkness[idx]);
                }

                boolean hasAbove = y < height - 1;
                int aboveFlag = hasAbove ? flags[idx + width] : 0;

                boolean shadow = (flag & flagAir) != 0 && (aboveFlag & flagSolid) != 0;
                boolean shore = (flag & flagLiquid) != 0 && (!hasAbove || (aboveFlag & flagLiquid) == 0);

                pixmap.setRaw(x, height - 1 - y, shadeColor(color, colors[idx], dark, shadow, shore));
            }
        }

        return pixmap;
    }

    /** Same shading as MinimapRenderer.colorFor, after the base color has been determined. */
    private static int shadeColor(Color color, int base, float darkness, boolean shadow, boolean shore){
        color.set(base);
        color.mul(1f - Mathf.clamp(darkness / 4f));

        if(shadow){
            color.mul(0.7f);
        }else if(shore){
            color.mul(0.84f, 0.84f, 0.9f, 1f);
        }

        return color.rgba();
    }

    /** Map-edge darkness; copy of the first part of World.getDarkness. */
    private static float borderDarkness(int x, int y, int width, int height, boolean limitMapArea, int limitX, int limitY, int limitWidth, int limitHeight){
        int edgeBlend = 2;
        int edgeDst;

        if(!limitMapArea){
            edgeDst = Math.min(x, Math.min(y, Math.min(-(x - (width - 1)), -(y - (height - 1)))));
        }else{
            edgeDst =
            Math.min(x - limitX,
            Math.min(y - limitY,
            Math.min(-(x - (limitX + limitWidth - 1)), -(y - (limitY + limitHeight - 1)))));
        }

        return edgeDst <= edgeBlend ? (edgeBlend - edgeDst) * (4f / edgeBlend) : 0f;
    }

    /** Array-based copy of World.applyDarkness. Returns the per-tile "data" value, only meaningful for tiles with flagDark. */
    private static byte[] computeDarkness(byte[] flags, int width, int height){
        int len = width * height;
        byte[] dark = new byte[len], buffer = new byte[len], out = new byte[len];

        for(int i = 0; i < len; i++){
            if((flags[i] & flagDark) != 0){
                dark[i] = darkRadius;
            }
        }

        for(int i = 0; i < darkRadius; i++){
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    int idx = x + y * width;
                    boolean min = false;
                    for(Point2 point : Geometry.d4){
                        int nx = x + point.x, ny = y + point.y;
                        if(nx >= 0 && ny >= 0 && nx < width && ny < height && dark[nx + ny * width] < dark[idx]){
                            min = true;
                            break;
                        }
                    }
                    buffer[idx] = (byte)Math.max(0, dark[idx] - (min ? 1 : 0));
                }
            }

            System.arraycopy(buffer, 0, dark, 0, len);
        }

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int idx = x + y * width;
                boolean darkened = (flags[idx] & flagDark) != 0;

                if(darkened){
                    out[idx] = dark[idx];
                }

                if(dark[idx] == darkRadius){
                    boolean full = true;
                    for(Point2 p : Geometry.d4){
                        int px = p.x + x, py = p.y + y;
                        if(px >= 0 && py >= 0 && px < width && py < height && !(darkened && dark[px + py * width] == darkRadius)){
                            full = false;
                            break;
                        }
                    }

                    if(full) out[idx] = (byte)(darkRadius + 1);
                }
            }
        }

        return out;
    }

    /** Base (unshaded) color of a tile; same logic as MinimapRenderer.colorFor before shading. */
    private static int previewColor(Tile tile){
        Block real = tile.block();
        int bc = real.minimapColor(tile);
        if(bc == 0 && real == Blocks.air && tile.overlay() == Blocks.air) bc = tile.floor().minimapColor(tile);
        return bc == 0 ? colorFor(real, tile.floor(), tile.overlay(), tile.team()) : bc;
    }

    private static void readPreviewContentHeader(DataInput stream, SaveLoadContext context) throws IOException{
        //reads content header while refusing to fire patch loaded event
        int mapped = stream.readUnsignedByte();

        MappableContent[][] map = new MappableContent[ContentType.all.length][0];

        for(int i = 0; i < mapped; i++){
            ContentType type = ContentType.all[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];

            for(int j = 0; j < total; j++){
                String name = stream.readUTF();
                //fallback only for blocks
                map[type.ordinal()][j] = content.getByName(type, type == ContentType.block ? SaveFileReader.fallback.get(name, name) : name);
            }
        }

        context.reads = new MappedReads(null, map);
    }

    /** Generates a preview from a loaded world. Shading matches the minimap and the file-based generatePreview(Map). */
    public static Pixmap generatePreview(World world){
        Pixmap pixmap = new Pixmap(world.width, world.height);
        Color color = new Color();

        for(int x = 0; x < pixmap.width; x++){
            for(int y = 0; y < pixmap.height; y++){
                Tile tile = world.getn(x, y);
                Tile above = y < world.height - 1 ? world.rawTile(x, y + 1) : null;

                boolean shadow = tile.block() == Blocks.air && above != null && above.block().solid;
                boolean shore = tile.floor().isLiquid && (above == null || !above.floor().isLiquid);

                pixmap.setRaw(x, pixmap.height - 1 - y, shadeColor(color, previewColor(tile), world.getDarkness(x, y), shadow, shore));
            }
        }
        return pixmap;
    }

    public static int colorFor(Block wall, Block floor, Block overlay, Team team){
        if(wall.synthetic()){
            return team.color.rgba();
        }
        return (((Floor)overlay).wallOre ? overlay.mapColor.rgba() : wall.solid ? wall.mapColor.rgba() : !overlay.useColor ? floor.mapColor.rgba() :
            (!(overlay instanceof OverlayFloor) ? Pixmap.blend((overlay.mapColor.rgba() & ~0xff) | 128, floor.mapColor.rgba()) : overlay.mapColor.rgba()));
    }

    public static Pixmap writeImage(World tiles){
        Pixmap pix = new Pixmap(tiles.width, tiles.height);
        for(Tile tile : tiles){
            //while synthetic blocks are possible, most of their data is lost, so in order to avoid questions like
            //"why is there air under my drill" and "why are all my conveyors facing right", they are disabled
            int color = tile.block().hasColor && !tile.block().hasBuilding() ? tile.block().mapColor.rgba() : tile.floor().mapColor.rgba();
            pix.set(tile.x, tiles.height - 1 - tile.y, color);
        }
        return pix;
    }

    public static void readImage(Pixmap pixmap, World tiles){
        for(Tile tile : tiles){
            int color = pixmap.get(tile.x, pixmap.height - 1 - tile.y);
            Block block = ColorMapper.get(color);

            //ignore buildings; reading images is only intended for environment tiles
            if(block.hasBuilding()) continue;

            if(block.isOverlay()){
                tile.setOverlay(block.asFloor());
            }else if(block.isFloor()){
                tile.setFloor(block.asFloor());
            }else if(block.isMultiblock()){
                tile.setBlock(block, Team.derelict, 0);
            }else{
                tile.setBlock(block);
            }
        }

        for(Tile tile : tiles){
            //default to stone floor
            if(tile.floor() == Blocks.air){
                tile.setFloor((Floor)Blocks.stone);
            }
        }
    }
}
