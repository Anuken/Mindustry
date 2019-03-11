package io.anuke.mindustry.io;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.Pack;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.world;

/** Reads and writes map files.*/
public class MapIO{
    private static final int version = 1;
    private static final int[] pngHeader = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static boolean isImage(FileHandle file){
        try(InputStream stream = file.read()){
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

    //TODO implement
    public static Pixmap unfinished_generatePreview(Map map){
        return null;
    }

    //TODO implement
    /**Reads a pixmap in the 3.5 pixmap format.*/
    public static Tile[][] unfinished_readLegacyPixmap(Pixmap pixmap){
        return null;
    }

    //TODO implement
    /**Reads a pixmap in the old 4.0 .mmap format.*/
    private static Tile[][] unfinished_readLegacyMmap(InputStream stream) throws IOException{
        return null;
    }

    public static int colorFor(Block floor, Block wall, Team team){
        if(wall.synthetic()){
            return team.intColor;
        }
        return Color.rgba8888(wall.solid ? wall.color : floor.color);
    }

    public static void writeMap(Map map, Tile[][] tiles, OutputStream output) throws IOException{
        try(DataOutputStream stream = new DataOutputStream(output)){
            stream.writeInt(version);
            stream.writeInt(Version.build);
            stream.writeShort(tiles.length);
            stream.writeShort(tiles[0].length);
            stream.writeByte((byte)map.tags.size);

            for(Entry<String, String> entry : map.tags.entries()){
                stream.writeUTF(entry.key);
                stream.writeUTF(entry.value);
            }
        }

        try(DataOutputStream stream = new DataOutputStream(new DeflaterOutputStream(output))){

            SaveIO.getSaveWriter().writeContentHeader(stream);

            //floor first
            for(int i = 0; i < tiles.length * tiles[0].length; i++){
                Tile tile = world.tile(i % world.width(), i / world.width());
                stream.writeByte(tile.getFloorID());
                stream.writeByte(tile.getOre());
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.tile(j % world.width(), j / world.width());

                    if(nextTile.getFloorID() != tile.getFloorID() || nextTile.block() != Blocks.air || nextTile.getOre() != tile.getOre()){
                        break;
                    }

                    consecutives++;
                }

                stream.writeByte(consecutives);
                i += consecutives;
            }

            //blocks
            for(int i = 0; i < tiles.length * tiles[0].length; i++){
                Tile tile = world.tile(i % world.width(), i / world.width());
                stream.writeByte(tile.getBlockID());

                if(tile.block() instanceof BlockPart){
                    stream.writeByte(tile.link);
                }else if(tile.entity != null){
                    stream.writeByte(Pack.byteByte(tile.getTeamID(), tile.getRotation())); //team + rotation
                    stream.writeShort((short)tile.entity.health); //health
                    tile.entity.writeConfig(stream);
                }else{
                    //write consecutive non-entity blocks
                    int consecutives = 0;

                    for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                        Tile nextTile = world.tile(j % world.width(), j / world.width());

                        if(nextTile.block() != tile.block()){
                            break;
                        }

                        consecutives++;
                    }

                    stream.writeByte(consecutives);
                    i += consecutives;
                }
            }
        }
    }

    public static Map readMap(String useName, InputStream input) throws IOException{
        try(DataInputStream stream = new DataInputStream(input)){
            ObjectMap<String, String> tags = new ObjectMap<>();

            //meta is uncompressed
            int version = stream.readInt();
            int build = stream.readInt();
            short width = stream.readShort(), height = stream.readShort();
            byte tagAmount = stream.readByte();

            for(int i = 0; i < tagAmount; i++){
                String name = stream.readUTF();
                String value = stream.readUTF();
                tags.put(name, value);
            }

            return new Map(useName, width, height);
        }
    }

    public static Tile[][] readTiles(Map map, Tile[][] tiles) throws IOException{
        return readTiles(map.stream.get(), map.width, map.height, tiles);
    }

    public static Tile[][] readTiles(InputStream input, int width, int height, Tile[][] tiles) throws IOException{
        readMap("this map name is utterly irrelevant", input);

        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(input))){

            MappableContent[][] c = SaveIO.getSaveWriter().readContentHeader(stream);

            try{
                content.setTemporaryMapper(c);
                //TODO 2-phase rle

                for(int i = 0; i < width * height; i++){
                    int x = i % width, y = i / width;
                    byte floorid = stream.readByte();
                    byte wallid = stream.readByte();
                    byte oreid = stream.readByte();

                    Tile tile = new Tile(x, y, floorid, wallid);

                    if(wallid == Blocks.part.id){
                        tile.link = stream.readByte();
                    }else if(tile.entity != null){
                        byte tr = stream.readByte();
                        short health = stream.readShort();

                        byte team = Pack.leftByte(tr);
                        byte rotation = Pack.rightByte(tr);

                        tile.setTeam(Team.all[team]);
                        tile.entity.health = health;
                        tile.setRotation(rotation);

                        tile.entity.readConfig(stream);
                    }else if(wallid == 0){
                        int consecutives = stream.readUnsignedByte();

                        for(int j = i + 1; j < i + 1 + consecutives; j++){
                            int newx = j % width, newy = j / width;
                            Tile newTile = new Tile(newx, newy, floorid, wallid);
                            newTile.setOre(oreid);
                            tiles[newx][newy] = newTile;
                        }

                        i += consecutives;
                    }

                    tiles[x][y] = tile;
                }

                return tiles;

            }finally{
                content.setTemporaryMapper(null);
            }
        }
    }
}