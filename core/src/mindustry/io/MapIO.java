package mindustry.io;

import arc.files.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.maps.*;
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
            StringMap tags = new StringMap();
            ver.region("meta", stream, counter, in -> tags.putAll(ver.readStringMap(in)));
            return new Map(file, tags.getInt("width"), tags.getInt("height"), tags, custom, version, Version.build);
        }
    }

    public static void writeMap(Fi file, Map map) throws IOException{
        try{
            SaveIO.write(file, map.tags);
        }catch(Exception e){
            throw new IOException(e);
        }
    }

    public static void loadMap(Map map){
        SaveIO.load(map.file);
    }

    public static void loadMap(Map map, WorldContext cons){
        SaveIO.load(map.file, cons);
    }

    public static Pixmap generatePreview(Map map) throws IOException{
        map.spawns = 0;
        map.teams.clear();

        try(InputStream is = new InflaterInputStream(map.file.read(bufferSize)); CounterInputStream counter = new CounterInputStream(is); DataInputStream stream = new DataInputStream(counter)){
            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            ver.region("meta", stream, counter, ver::readStringMap);

            Pixmap floors = new Pixmap(map.width, map.height);
            Pixmap walls = new Pixmap(map.width, map.height);
            int black = 255;
            int shade = Color.rgba8888(0f, 0f, 0f, 0.5f);
            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    super.setBlock(type);

                    int c = colorFor(block(), Blocks.air, Blocks.air, team());
                    if(c != black){
                        walls.setRaw(x, floors.height - 1 - y, c);
                        floors.set(x, floors.height - 1 - y + 1, shade);
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext(){
                @Override public void resize(int width, int height){}
                @Override public boolean isGenerating(){return false;}
                @Override public void begin(){
                    world.setGenerating(true);
                }
                @Override public void end(){
                    world.setGenerating(false);
                }

                @Override
                public void onReadBuilding(){
                    //read team colors
                    if(tile.build != null){
                        int c = tile.build.team.color.rgba8888();
                        int size = tile.block().size;
                        int offsetx = -(size - 1) / 2;
                        int offsety = -(size - 1) / 2;
                        for(int dx = 0; dx < size; dx++){
                            for(int dy = 0; dy < size; dy++){
                                int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                                walls.set(drawx, floors.height - 1 - drawy, c);
                            }
                        }

                        if(tile.build.block instanceof CoreBlock){
                            map.teams.add(tile.build.team.id);
                        }
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
                    if(overlayID != 0){
                        floors.set(x, floors.height - 1 - y, colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict));
                    }else{
                        floors.set(x, floors.height - 1 - y, colorFor(Blocks.air, content.block(floorID), Blocks.air, Team.derelict));
                    }
                    if(content.block(overlayID) == Blocks.spawn){
                        map.spawns ++;
                    }
                    return tile;
                }
            }));

            floors.draw(walls, true);
            walls.dispose();
            return floors;
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public static Pixmap generatePreview(Tiles tiles){
        Pixmap pixmap = new Pixmap(tiles.width, tiles.height);
        for(int x = 0; x < pixmap.width; x++){
            for(int y = 0; y < pixmap.height; y++){
                Tile tile = tiles.getn(x, y);
                pixmap.set(x, pixmap.height - 1 - y, colorFor(tile.block(), tile.floor(), tile.overlay(), tile.team()));
            }
        }
        return pixmap;
    }

    public static int colorFor(Block wall, Block floor, Block overlay, Team team){
        if(wall.synthetic()){
            return team.color.rgba();
        }
        return (wall.solid ? wall.mapColor : !overlay.useColor ? floor.mapColor : overlay.mapColor).rgba();
    }

    public static Pixmap writeImage(Tiles tiles){
        Pixmap pix = new Pixmap(tiles.width, tiles.height);
        for(Tile tile : tiles){
            //while synthetic blocks are possible, most of their data is lost, so in order to avoid questions like
            //"why is there air under my drill" and "why are all my conveyors facing right", they are disabled
            int color = tile.block().hasColor && !tile.block().synthetic() ? tile.block().mapColor.rgba() : tile.floor().mapColor.rgba();
            pix.set(tile.x, tiles.height - 1 - tile.y, color);
        }
        return pix;
    }

    public static void readImage(Pixmap pixmap, Tiles tiles){
        for(Tile tile : tiles){
            int color = pixmap.get(tile.x, pixmap.height - 1 - tile.y);
            Block block = ColorMapper.get(color);

            if(block.isFloor()){
                tile.setFloor(block.asFloor());
            }else if(block.isMultiblock()){
                tile.setBlock(block, Team.derelict, 0);
            }else{
                tile.setBlock(block);
            }
        }

        //guess at floors by grabbing a random adjacent floor
        for(Tile tile : tiles){
            if(tile.floor() == Blocks.air && tile.block() != Blocks.air){
                for(Point2 p : Geometry.d4){
                    Tile other = tiles.get(tile.x + p.x, tile.y + p.y);
                    if(other != null && other.floor() != Blocks.air){
                        tile.setFloorUnder(other.floor());
                        break;
                    }
                }
            }

            //default to stone floor
            if(tile.floor() == Blocks.air){
                tile.setFloorUnder((Floor)Blocks.stone);
            }
        }
    }
}
