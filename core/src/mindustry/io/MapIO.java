package mindustry.io;

import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.io.*;
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

    /**
     * Generates a preview of a map. This is mostly safe, but writes to map spawns/teams in the same method, which is a bit risky, but I don't have any better ideas.
     * There's no way (that I can see) of checking teams/spawns in a map without reading every time.
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

            Pixmap floors = new Pixmap(map.width, map.height);
            Pixmap walls = new Pixmap(map.width, map.height);
            int black = 255;
            int shade = Color.rgba8888(0f, 0f, 0.1f, 0.5f);

            int width = map.width, height = map.height;
            int len = width*height;
            short[] floorIds = new short[len];
            boolean[] overlays = new boolean[len];

            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    //do not super.setBlock as that affects the current world; previews never create buildings
                    this.block = type;
                    this.build = null;
                    int c = colorFor(type, Blocks.air, Blocks.air, team());
                    if(c != black){
                        walls.setRaw(x, floors.height - 1 - y, c);
                        int offset = -(type.size - 1) / 2;
                        for(int dx = 0; dx < type.size; dx++){
                            int px = x + dx + offset, py = floors.height - 1 - (y + offset) + 1;
                            floors.set(px, py, Pixmap.blend(shade, floors.get(px, py)));
                        }
                    }
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
                    //read team colors
                    int c = team.color.rgba8888();
                    int size = tile.block().size;
                    int offsetx = -(size - 1) / 2;
                    int offsety = -(size - 1) / 2;
                    for(int dx = 0; dx < size; dx++){
                        for(int dy = 0; dy < size; dy++){
                            int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                            walls.set(drawx, floors.height - 1 - drawy, c);
                        }
                    }

                    if(tile.block() instanceof CoreBlock){
                        map.teams.add(team.id);
                    }
                }

                @Override
                public Tile tile(int index){
                    tile.x = (short)(index % map.width);
                    tile.y = (short)(index / map.width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID){
                    floors.set(x, floors.height - 1 - y, colorFor(Blocks.air, content.block(floorID), content.block(overlayID), Team.derelict));

                    if(content.block(overlayID) == Blocks.spawn){
                        map.spawns ++;
                    }
                    floorIds[x + y * width] = (short)floorID;
                    overlays[x + y * width] = overlayID != 0;
                    return tile;
                }

                @Override
                public void onReadTileData(){
                    Block block = tile.block();
                    Block floor = content.block(floorIds[tile.x + tile.y*width]);

                    if(!block.synthetic() && block != Blocks.air){
                        int color = block.minimapColor(tile);
                        if(color != 0){
                            walls.set(tile.x, walls.height - 1 - tile.y, color);
                        }
                    }else if(!overlays[tile.x + tile.y * width] && block == Blocks.air){
                        int color = floor.minimapColor(tile);
                        if(color != 0){
                            floors.set(tile.x, floors.height - 1 - tile.y, color);
                        }
                    }
                }
            };

            if(ver.version >= 12) ver.skipChunk(stream);
            ver.readRegion("content", stream, counter, in -> readPreviewContentHeader(in, context));
            if(ver.version == 11) ver.skipChunk(stream);
            ver.readRegion("preview_map", stream, counter, in -> ver.readMap(in, context));

            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    if(!((Floor)content.block(floorIds[x + y * width])).isLiquid) continue;
                    if(y < height - 1 && ((Floor)content.block(floorIds[x + (y + 1) * width])).isLiquid) continue;

                    int row = height - 1 - y;
                    floors.set(x, row, new Color(floors.get(x, row)).mul(0.84f, 0.84f, 0.9f, 1f));
                }
            }

            floors.draw(walls, true);
            walls.dispose();
            return floors;
        }
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

    public static Pixmap generatePreview(World tiles){
        Pixmap pixmap = new Pixmap(tiles.width, tiles.height);
        for(int x = 0; x < pixmap.width; x++){
            for(int y = 0; y < pixmap.height; y++){
                Tile tile = tiles.getn(x, y);
                int color = 0;
                if(!tile.block().synthetic() && tile.block() != Blocks.air){
                    color = tile.block().minimapColor(tile);
                }else if(tile.overlay() == Blocks.air && tile.block() == Blocks.air){
                    color = tile.floor().minimapColor(tile);
                }
                if(color == 0) color = colorFor(tile.block(), tile.floor(), tile.overlay(), tile.team());
                pixmap.set(x, pixmap.height - 1 - y, color);
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
