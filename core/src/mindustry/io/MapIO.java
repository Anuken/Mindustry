package mindustry.io;

import arc.struct.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.maps.*;
import mindustry.world.*;
import mindustry.world.LegacyColorMapper.*;
import mindustry.world.blocks.storage.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

/** Reads and writes map files. */
//TODO does this class even need to exist??? move to Maps?
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

            Pixmap floors = new Pixmap(map.width, map.height, Format.RGBA8888);
            Pixmap walls = new Pixmap(map.width, map.height, Format.RGBA8888);
            int black = 255;
            int shade = Color.rgba8888(0f, 0f, 0f, 0.5f);
            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    super.setBlock(type);
                    int c = colorFor(Blocks.air, block(), Blocks.air, getTeam());
                    if(c != black){
                        walls.draw(x, floors.getHeight() - 1 - y, c);
                        floors.draw(x, floors.getHeight() - 1 - y + 1, shade);
                    }
                }

                @Override
                public void setTeam(Team team){
                    super.setTeam(team);
                    if(block instanceof CoreBlock){
                        map.teams.add(team.id);
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext(){
                @Override public void resize(int width, int height){}
                @Override public boolean isGenerating(){return false;}
                @Override public void begin(){}
                @Override public void end(){}

                @Override
                public Tile tile(int x, int y){
                    tile.x = (short)x;
                    tile.y = (short)y;
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID){
                    if(overlayID != 0){
                        floors.draw(x, floors.getHeight() - 1 - y, colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict));
                    }else{
                        floors.draw(x, floors.getHeight() - 1 - y, colorFor(content.block(floorID), Blocks.air, Blocks.air, Team.derelict));
                    }
                    if(content.block(overlayID) == Blocks.spawn){
                        map.spawns ++;
                    }
                    return tile;
                }
            }));

            floors.drawPixmap(walls, 0, 0);
            walls.dispose();
            return floors;
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public static Pixmap generatePreview(Tile[][] tiles){
        Pixmap pixmap = new Pixmap(tiles.length, tiles[0].length, Format.RGBA8888);
        for(int x = 0; x < pixmap.getWidth(); x++){
            for(int y = 0; y < pixmap.getHeight(); y++){
                Tile tile = tiles[x][y];
                pixmap.draw(x, pixmap.getHeight() - 1 - y, colorFor(tile.floor(), tile.block(), tile.overlay(), tile.getTeam()));
            }
        }
        return pixmap;
    }

    public static int colorFor(Block floor, Block wall, Block ore, Team team){
        if(wall.synthetic()){
            return team.color.rgba();
        }
        return (wall.solid ? wall.color : ore == Blocks.air ? floor.color : ore.color).rgba();
    }

    /** Reads a pixmap in the 3.5 pixmap format. */
    public static void readPixmap(Pixmap pixmap, Tile[][] tiles){
        for(int x = 0; x < pixmap.getWidth(); x++){
            for(int y = 0; y < pixmap.getHeight(); y++){
                int color = pixmap.getPixel(x, pixmap.getHeight() - 1 - y);
                LegacyBlock block = LegacyColorMapper.get(color);
                Tile tile = tiles[x][y];

                tile.setFloor(block.floor);
                tile.setBlock(block.wall);
                if(block.ore != null) tile.setOverlay(block.ore);

                //place core
                if(color == Color.green.rgba()){
                    //actual core parts
                    tile.setBlock(Blocks.coreShard);
                    tile.setTeam(Team.sharded);
                }
            }
        }
    }

    interface TileProvider{
        Tile get(int x, int y);
    }
}
