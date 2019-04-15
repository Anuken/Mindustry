package io.anuke.mindustry.io;

import io.anuke.arc.collection.Array;
import io.anuke.arc.util.Pack;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.Serialization;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import java.io.*;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.world;

public abstract class SaveFileVersion{
    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong();
        long playtime = stream.readLong();
        int build = stream.readInt();

        Rules rules = Serialization.readRulesStreamJson(stream);
        String map = stream.readUTF();
        int wave = stream.readInt();
        return new SaveMeta(version, time, playtime, build, map, wave, rules);
    }

    public void writeMap(DataOutputStream stream) throws IOException{
        //write world size
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        //floor first
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i % world.width(), i / world.width());
            stream.writeByte(tile.getFloorID());
            stream.writeByte(tile.getOverlayID());
            int consecutives = 0;

            for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                Tile nextTile = world.tile(j % world.width(), j / world.width());

                if(nextTile.getFloorID() != tile.getFloorID() || nextTile.getOverlayID() != tile.getOverlayID()){
                    break;
                }

                consecutives++;
            }

            stream.writeByte(consecutives);
            i += consecutives;
        }

        //blocks
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i % world.width(), i / world.width());
            stream.writeByte(tile.getBlockID());

            if(tile.block() == Blocks.part){
                stream.writeByte(tile.getLinkByte());
            }else if(tile.entity != null){
                stream.writeByte(Pack.byteByte(tile.getTeamID(), tile.getRotation())); //team + rotation
                stream.writeShort((short)tile.entity.health); //health

                if(tile.entity.items != null) tile.entity.items.write(stream);
                if(tile.entity.power != null) tile.entity.power.write(stream);
                if(tile.entity.liquids != null) tile.entity.liquids.write(stream);
                if(tile.entity.cons != null) tile.entity.cons.write(stream);

                tile.entity.writeConfig(stream);
                tile.entity.write(stream);
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

    public void readMap(DataInputStream stream) throws IOException{
        short width = stream.readShort();
        short height = stream.readShort();

        world.beginMapLoad();

        Tile[][] tiles = world.createTiles(width, height);

        //read floor and create tiles first
        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            byte floorid = stream.readByte();
            byte oreid = stream.readByte();
            int consecutives = stream.readUnsignedByte();
            Block ore = content.block(oreid);

            tiles[x][y] = new Tile(x, y, floorid, (byte)0);
            tiles[x][y].setOverlay(ore);

            for(int j = i + 1; j < i + 1 + consecutives; j++){
                int newx = j % width, newy = j / width;
                Tile newTile = new Tile(newx, newy, floorid, (byte)0);
                newTile.setOverlay(ore);
                tiles[newx][newy] = newTile;
            }

            i += consecutives;
        }

        //read blocks
        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            Block block = content.block(stream.readByte());
            Tile tile = tiles[x][y];
            tile.setBlock(block);

            if(block == Blocks.part){
                tile.setLinkByte(stream.readByte());
            }else if(tile.entity != null){
                byte tr = stream.readByte();
                short health = stream.readShort();

                byte team = Pack.leftByte(tr);
                byte rotation = Pack.rightByte(tr);

                tile.setTeam(Team.all[team]);
                tile.entity.health = health;
                tile.setRotation(rotation);

                if(tile.entity.items != null) tile.entity.items.read(stream);
                if(tile.entity.power != null) tile.entity.power.read(stream);
                if(tile.entity.liquids != null) tile.entity.liquids.read(stream);
                if(tile.entity.cons != null) tile.entity.cons.read(stream);

                tile.entity.readConfig(stream);
                tile.entity.read(stream);
            }else{
                int consecutives = stream.readUnsignedByte();

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    tiles[newx][newy].setBlock(block);
                }

                i += consecutives;
            }
        }

        content.setTemporaryMapper(null);
        world.endMapLoad();
    }

    public void writeEntities(DataOutputStream stream) throws IOException{
        int groups = 0;

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                groups++;
            }
        }

        stream.writeByte(groups);

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                stream.writeInt(group.size());
                for(Entity entity : group.all()){
                    stream.writeByte(((SaveTrait)entity).getTypeID());
                    ((SaveTrait)entity).writeSave(stream);
                }
            }
        }
    }

    public void readEntities(DataInputStream stream) throws IOException{
        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                byte typeid = stream.readByte();
                SaveTrait trait = (SaveTrait)TypeTrait.getTypeByID(typeid).get();
                trait.readSave(stream);
            }
        }
    }

    public MappableContent[][] readContentHeader(DataInputStream stream) throws IOException{

        byte mapped = stream.readByte();

        MappableContent[][] map = new MappableContent[ContentType.values().length][0];

        for(int i = 0; i < mapped; i++){
            ContentType type = ContentType.values()[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];

            for(int j = 0; j < total; j++){
                String name = stream.readUTF();
                map[type.ordinal()][j] = content.getByName(type, name);
            }
        }

        return map;
    }

    public void writeContentHeader(DataOutputStream stream) throws IOException{
        Array<Content>[] map = content.getContentMap();

        int mappable = 0;
        for(Array<Content> arr : map){
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                mappable++;
            }
        }

        stream.writeByte(mappable);
        for(Array<Content> arr : map){
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                stream.writeByte(arr.first().getContentType().ordinal());
                stream.writeShort(arr.size);
                for(Content c : arr){
                    stream.writeUTF(((MappableContent)c).name);
                }
            }
        }
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
