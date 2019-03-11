package io.anuke.mindustry.io;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.Pack;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.world;

/** Reads and writes map files.*/
public class MapIO{
    private static final int version = 0;

    //TODO implement
    public static Pixmap generatePixmap(Map map){
        return null;
    }

    //TODO implement
    /**Reads a pixmap in the 3.5 pixmap format.*/
    public static Tile[][] readLegacyPixmap(Pixmap pixmap){
        return null;
    }

    //TODO implement
    /**Reads a pixmap in the 4.0 .mmap format.*/
    public static Tile[][] readLegacyMmap(DataInputStream stream) throws IOException{
        return null;
    }

    public static void writeMap(Map map, Tile[][] tiles, DataOutputStream stream) throws IOException{
        stream.writeInt(version);
        stream.writeInt(Version.build);
        stream.writeByte((byte) map.tags.size);

        for(Entry<String, String> entry : map.tags.entries()){
            stream.writeUTF(entry.key);
            stream.writeUTF(entry.value);
        }

        SaveIO.getSaveWriter().writeContentHeader(stream);

        stream.writeShort(tiles.length);
        stream.writeShort(tiles[0].length);

        for(int i = 0; i < tiles.length * tiles[0].length; i++){
            Tile tile = world.tile(i % world.width(), i / world.width());

            stream.writeByte(tile.getFloorID());
            stream.writeByte(tile.getBlockID());

            if(tile.block() instanceof BlockPart){
                stream.writeByte(tile.link);
            }else if(tile.entity != null){
                stream.writeByte(Pack.byteByte(tile.getTeamID(), tile.getRotation())); //team + rotation
                stream.writeShort((short) tile.entity.health); //health
                tile.entity.writeConfig(stream);
            }else if(tile.block() == Blocks.air){
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.tile(j % world.width(), j / world.width());

                    if(nextTile.getFloorID() != tile.getFloorID() || nextTile.block() != Blocks.air){
                        break;
                    }

                    consecutives++;
                }

                stream.writeByte(consecutives);
                i += consecutives;
            }
        }
    }

    public static Map readMap(String useName, DataInputStream stream) throws IOException{
        ObjectMap<String, String> tags = new ObjectMap<>();

        int version = stream.readInt(); //version
        int build = stream.readInt();
        byte tagAmount = stream.readByte();

        for(int i = 0; i < tagAmount; i++){
            String name = stream.readUTF();
            String value = stream.readUTF();
            tags.put(name, value);
        }

        return new Map(useName);
    }

    public static Tile[][] readTiles(DataInputStream stream) throws IOException{
        readMap("this map name is utterly irrelevant", stream);

        MappableContent[][] c = SaveIO.getSaveWriter().readContentHeader(stream);

        int width = stream.readShort();
        int height = stream.readShort();

        try{

            content.setTemporaryMapper(c);

            Tile[][] tiles = new Tile[width][height];

            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                byte floorid = stream.readByte();
                byte wallid = stream.readByte();

                Tile tile = new Tile(x, y, floorid, wallid);

                if(wallid == Blocks.part.id){
                    tile.link = stream.readByte();
                }else if(tile.entity != null){
                    byte tr = stream.readByte();
                    short health = stream.readShort();

                    byte team = Pack.leftByte(tr);
                    byte rotation = Pack.rightByte(tr);

                    Team t = Team.all[team];

                    tile.setTeam(Team.all[team]);
                    tile.entity.health = health;
                    tile.setRotation(rotation);

                    tile.entity.readConfig(stream);
                }else if(wallid == 0){
                    int consecutives = stream.readUnsignedByte();

                    for(int j = i + 1; j < i + 1 + consecutives; j++){
                        int newx = j % width, newy = j / width;
                        Tile newTile = new Tile(newx, newy, floorid, wallid);
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
