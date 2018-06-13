package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.util.Bits;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Save16 extends SaveFileVersion {

    public Save16(){
        super(16);
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        /*long loadTime = */
        stream.readLong();

        //general state
        byte mode = stream.readByte();
        String mapname = stream.readUTF();
        world.setMap(world.maps().getByName(mapname));

        int wave = stream.readInt();
        byte difficulty = stream.readByte();
        float wavetime = stream.readFloat();

        state.difficulty = Difficulty.values()[difficulty];
        state.mode = GameMode.values()[mode];
        state.enemies = 0; //TODO display enemies correctly!
        state.wave = wave;
        state.wavetime = wavetime;

        //block header

        int blocksize = stream.readInt();

        IntMap<Block> map = new IntMap<>();

        for(int i = 0; i < blocksize; i ++){
            String name = stream.readUTF();
            int id = stream.readShort();

            map.put(id, Block.getByName(name));
        }

        //entities

        byte groups = stream.readByte();

        for (int i = 0; i < groups; i++) {
            int amount = stream.readInt();
            for (int j = 0; j < amount; j++) {
                byte typeid = stream.readByte();
                SaveTrait trait = (SaveTrait) TypeTrait.getTypeByID(typeid).get();
                trait.readSave(stream);
            }
        }

        //map

        short width = stream.readShort();
        short height = stream.readShort();

        EntityPhysics.resizeTree(0, 0, width * tilesize, height * tilesize);

        world.beginMapLoad();

        Tile[][] tiles = world.createTiles(width, height);

        for(int x = 0; x < width; x ++){
            for(int y = 0; y < height; y ++) {
                byte floorid = stream.readByte();
                byte wallid = stream.readByte();
                byte elevation = stream.readByte();

                Tile tile = new Tile(x, y, floorid, wallid);

                tile.elevation = elevation;

                if (wallid == Blocks.blockpart.id) {
                    tile.link = stream.readByte();
                }

                if (tile.entity != null) {
                    byte tr = stream.readByte();
                    short health = stream.readShort();

                    byte team = Bits.getLeftByte(tr);
                    byte rotation = Bits.getRightByte(tr);

                    Team t = Team.values()[team];

                    tile.setTeam(Team.values()[team]);
                    tile.entity.health = health;
                    tile.setRotation(rotation);

                    if (tile.entity.items != null) tile.entity.items.read(stream);
                    if (tile.entity.power != null) tile.entity.power.read(stream);
                    if (tile.entity.liquids != null) tile.entity.liquids.read(stream);

                    tile.entity.read(stream);

                    if(tile.block() == StorageBlocks.core &&
                            state.teams.has(t)){
                        state.teams.get(t).cores.add(tile);
                    }
                }

                tiles[x][y] = tile;
            }
        }

        world.endMapLoad();
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        //--META--
        stream.writeInt(version); //version id
        stream.writeLong(TimeUtils.millis()); //last saved

        //--GENERAL STATE--
        stream.writeByte(state.mode.ordinal()); //gamemode
        stream.writeUTF(world.getMap().name); //map ID

        stream.writeInt(state.wave); //wave
        stream.writeByte(state.difficulty.ordinal()); //difficulty ordinal
        stream.writeFloat(state.wavetime); //wave countdown

        //--BLOCK HEADER--

        stream.writeInt(Block.all().size);

        for(int i = 0; i < Block.all().size; i ++){
            Block block = Block.all().get(i);
            stream.writeUTF(block.name);
            stream.writeShort(block.id);
        }

        //--ENTITIES--
        //TODO synchronized block here

        int groups = 0;

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                groups ++;
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

        //--MAP DATA--

        //write world size
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        //now write all blocks
        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                stream.writeByte(tile.floor().id);
                stream.writeByte(tile.block().id);
                stream.writeByte(tile.elevation);

                if(tile.block() instanceof BlockPart){
                    stream.writeByte(tile.link);
                }

                if(tile.entity != null){
                    stream.writeByte(Bits.packByte((byte)tile.getTeam().ordinal(), tile.getRotation())); //team + rotation
                    stream.writeShort((short)tile.entity.health); //health

                    if(tile.entity.items != null) tile.entity.items.write(stream);
                    if(tile.entity.power != null) tile.entity.power.write(stream);
                    if(tile.entity.liquids != null) tile.entity.liquids.write(stream);

                    tile.entity.write(stream);
                }
            }
        }
    }
}
