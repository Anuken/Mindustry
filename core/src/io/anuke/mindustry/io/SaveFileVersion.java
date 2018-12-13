package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.util.Bits;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public abstract class SaveFileVersion{
    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong();
        long playtime = stream.readLong();
        int build = stream.readInt();
        int sector = stream.readInt();
        byte mode = stream.readByte();
        String map = stream.readUTF();
        int wave = stream.readInt();
        byte difficulty = stream.readByte();
        return new SaveMeta(version, time, playtime, build, sector, mode, map, wave, Difficulty.values()[difficulty]);
    }

    public void writeMap(DataOutputStream stream) throws IOException{

        //write world size
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i % world.width(), i / world.width());

            stream.writeByte(tile.getFloorID());
            stream.writeByte(tile.getBlockID());
            stream.writeByte(tile.getElevation());

            if(tile.block() instanceof BlockPart){
                stream.writeByte(tile.link);
            }else if(tile.entity != null){
                stream.writeByte(Bits.packByte(tile.getTeamID(), tile.getRotation())); //team + rotation
                stream.writeShort((short) tile.entity.health); //health

                if(tile.entity.items != null) tile.entity.items.write(stream);
                if(tile.entity.power != null) tile.entity.power.write(stream);
                if(tile.entity.liquids != null) tile.entity.liquids.write(stream);
                if(tile.entity.cons != null) tile.entity.cons.write(stream);

                tile.entity.writeConfig(stream);
                tile.entity.write(stream);
            }else if(tile.block() == Blocks.air){
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.tile(j % world.width(), j / world.width());

                    if(nextTile.getFloorID() != tile.getFloorID() || nextTile.block() != Blocks.air || nextTile.getElevation() != tile.getElevation()){
                        break;
                    }

                    consecutives++;
                }

                stream.writeByte(consecutives);
                i += consecutives;
            }
        }

        //write visibility, length-run encoded
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i % world.width(), i / world.width());
            boolean discovered = tile.discovered();

            int consecutives = 0;

            for(int j = i + 1; j < world.width() * world.height() && consecutives < 32767*2-1; j++){
                Tile nextTile = world.tile(j % world.width(), j / world.width());

                if(nextTile.discovered() != discovered){
                    break;
                }

                consecutives++;
            }

            stream.writeBoolean(discovered);
            stream.writeShort(consecutives);
            i += consecutives;
        }
    }

    public void readMap(DataInputStream stream) throws IOException{
        short width = stream.readShort();
        short height = stream.readShort();

        if(world.getSector() != null){
            world.setMap(new Map("Sector " + world.getSector().x + ", " + world.getSector().y, width, height));
        }else if(world.getMap() == null){
            world.setMap(new Map("unknown", width, height));
        }

        world.beginMapLoad();

        Tile[][] tiles = world.createTiles(width, height);

        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            byte floorid = stream.readByte();
            byte wallid = stream.readByte();
            byte elevation = stream.readByte();

            Tile tile = new Tile(x, y, floorid, wallid);
            tile.setElevation(elevation);

            if(wallid == Blocks.blockpart.id){
                tile.link = stream.readByte();
            }else if(tile.entity != null){
                byte tr = stream.readByte();
                short health = stream.readShort();

                byte team = Bits.getLeftByte(tr);
                byte rotation = Bits.getRightByte(tr);

                Team t = Team.all[team];

                tile.setTeam(Team.all[team]);
                tile.entity.health = health;
                tile.setRotation(rotation);

                if(tile.entity.items != null) tile.entity.items.read(stream);
                if(tile.entity.power != null) tile.entity.power.read(stream);
                if(tile.entity.liquids != null) tile.entity.liquids.read(stream);
                if(tile.entity.cons != null) tile.entity.cons.read(stream);

                tile.entity.readConfig(stream);
                tile.entity.read(stream);

                if(tile.block() == StorageBlocks.core){
                    state.teams.get(t).cores.add(tile);
                }
            }else if(wallid == 0){
                int consecutives = stream.readUnsignedByte();

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    Tile newTile = new Tile(newx, newy, floorid, wallid);
                    newTile.setElevation(elevation);
                    tiles[newx][newy] = newTile;
                }

                i += consecutives;
            }

            tiles[x][y] = tile;
        }

        for(int i = 0; i < width * height; i++){
            boolean discovered = stream.readBoolean();
            int consecutives = stream.readUnsignedShort();
            if(discovered){
                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    tiles[newx][newy].setVisibility((byte) 1);
                }
            }
            i += consecutives;
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
                    stream.writeByte(((SaveTrait) entity).getTypeID());
                    ((SaveTrait) entity).writeSave(stream);
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
                SaveTrait trait = (SaveTrait) TypeTrait.getTypeByID(typeid).get();
                trait.readSave(stream);
            }
        }
    }

    public MappableContent[][] readContentHeader(DataInputStream stream) throws IOException{

        byte mapped = stream.readByte();

        MappableContent[][] map = new MappableContent[ContentType.values().length][0];

        for (int i = 0; i < mapped; i++) {
            ContentType type = ContentType.values()[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];

            for (int j = 0; j < total; j++) {
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
                    stream.writeUTF(((MappableContent) c).getContentName());
                }
            }
        }
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
