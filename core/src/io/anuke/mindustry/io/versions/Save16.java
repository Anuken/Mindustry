package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.entities.SerializableEntity;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
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
            byte gid = stream.readByte();
            EntityGroup<?> group = Entities.getGroup(gid);
            for (int j = 0; j < amount; j++) {
                Entity entity = construct(group.getType());
                ((SerializableEntity)entity).readSave(stream);
            }
        }

        //map

        short width = stream.readShort();
        short height = stream.readShort();

        Entities.resizeTree(0, 0, width * tilesize, height * tilesize);

        Tile[][] tiles = world.createTiles(width, height);

        for(int x = 0; x < width; x ++){
            for(int y = 0; y < height; y ++) {
                byte floorid = stream.readByte();
                byte wallid = stream.readByte();

                Tile tile = new Tile(x, y, floorid, wallid);


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

        if(!headless) renderer.clearTiles();
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

        stream.writeInt(Block.getAllBlocks().size);

        for(int i = 0; i < Block.getAllBlocks().size; i ++){
            Block block = Block.getAllBlocks().get(i);
            stream.writeUTF(block.name);
            stream.writeShort(block.id);
        }

        //--ENTITIES--
        //TODO synchronized block here

        int groups = 0;

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SerializableEntity){
                groups ++;
            }
        }

        stream.writeByte(groups);

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SerializableEntity){
                stream.writeInt(group.size());
                stream.writeByte(group.getID());
                for(Entity entity : group.all()){
                    ((SerializableEntity)entity).writeSave(stream);
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

                stream.writeByte(tile.floor().id); //floor ID
                stream.writeByte(tile.block().id); //wall ID

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
