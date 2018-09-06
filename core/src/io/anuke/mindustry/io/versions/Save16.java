package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.util.Bits;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Save16 extends SaveFileVersion{

    public Save16(){
        super(16);
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        stream.readLong(); //time
        stream.readLong(); //total playtime
        stream.readInt(); //build
        int sector = stream.readInt(); //sector ID

        //general state

        byte mode = stream.readByte();
        String mapname = stream.readUTF();
        Map map = world.maps().getByName(mapname);
        world.setMap(map);

        world.setSector(world.sectors().get(sector));

        int wave = stream.readInt();
        byte difficulty = stream.readByte();
        float wavetime = stream.readFloat();

        state.difficulty = Difficulty.values()[difficulty];
        state.mode = GameMode.values()[mode];
        state.wave = wave;
        state.wavetime = wavetime;

        content.setTemporaryMapper(readContentHeader(stream));

        state.spawner.read(stream);

        //entities

        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                byte typeid = stream.readByte();
                SaveTrait trait = (SaveTrait) TypeTrait.getTypeByID(typeid).get();
                trait.readSave(stream);
            }
        }

        //map

        short width = stream.readShort();
        short height = stream.readShort();

        if(world.getSector() != null){
            world.setMap(new Map("Sector " + world.getSector().x + ", " + world.getSector().y, width, height));
        }else if(map == null){
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

    @Override
    public void write(DataOutputStream stream) throws IOException{
        //--META--
        stream.writeInt(version); //version id
        stream.writeLong(TimeUtils.millis()); //last saved
        stream.writeLong(headless ? 0 : control.getSaves().getTotalPlaytime()); //playtime
        stream.writeInt(Version.build); //build
        stream.writeInt(world.getSector() == null ? invalidSector : world.getSector().packedPosition()); //sector ID

        //--GENERAL STATE--
        stream.writeByte(state.mode.ordinal()); //gamemode
        stream.writeUTF(world.getMap().name); //map ID

        stream.writeInt(state.wave); //wave
        stream.writeByte(state.difficulty.ordinal()); //difficulty ordinal
        stream.writeFloat(state.wavetime); //wave countdown

        writeContentHeader(stream);

        state.spawner.write(stream); //spawnes

        //--ENTITIES--

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

        //--MAP DATA--

        Timers.mark();

        //write world size
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i);

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

                tile.entity.write(stream);
            }else if(tile.block() == Blocks.air){
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.tile(j);

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
            Tile tile = world.tile(i);
            boolean discovered = tile.discovered();

            int consecutives = 0;

            for(int j = i + 1; j < world.width() * world.height() && consecutives < 32767*2-1; j++){
                Tile nextTile = world.tile(j);

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
}
