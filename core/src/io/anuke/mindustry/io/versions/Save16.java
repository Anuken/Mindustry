package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.util.Bits;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

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

        int wave = stream.readInt();
        byte difficulty = stream.readByte();
        float wavetime = stream.readFloat();

        state.difficulty = Difficulty.values()[difficulty];

        //block header

        int blocksize = stream.readInt();

        IntMap<Block> map = new IntMap<>();

        for(int i = 0; i < blocksize; i ++){
            String name = stream.readUTF();
            int id = stream.readShort();

            map.put(id, Block.getByName(name));
        }

        float playerx = stream.readFloat();
        float playery = stream.readFloat();

        int playerhealth = stream.readInt();

        if(!headless) {
            player.x = playerx;
            player.y = playery;
            player.health = playerhealth;
            state.mode = GameMode.values()[mode];
            Core.camera.position.set(playerx, playery, 0);

            //weapons
            control.upgrades().getWeapons().clear();
            control.upgrades().getWeapons().add(Weapon.blaster);
            player.weaponLeft = player.weaponRight = Weapon.blaster;

            int weapons = stream.readByte();

            for (int i = 0; i < weapons; i++) {
                control.upgrades().addWeapon((Weapon) Upgrade.getByID(stream.readByte()));
            }

            ui.hudfrag.updateWeapons();
        }else{
            byte b = stream.readByte();
            for(int i = 0; i < b; i ++) stream.readByte();
        }

        //inventory

        int totalItems = stream.readByte();

        Arrays.fill(state.inventory.getItems(), 0);

        for(int i = 0; i < totalItems; i ++){
            Item item = Item.getByID(stream.readByte());
            int amount = stream.readInt();
            state.inventory.getItems()[item.id] = amount;
        }

        if(!headless) ui.hudfrag.updateItems();

        //enemies

        byte teams = stream.readByte();

        for(int i = 0; i < teams; i ++){
            Team team = Team.values()[i];
            EntityGroup<BaseUnit> group = unitGroups[i];

            int amount = stream.readInt();

            for(int j = 0; j < amount; j ++){
                byte type = stream.readByte();
                float x = stream.readFloat();
                float y = stream.readFloat();
                int health = stream.readShort();

                BaseUnit enemy = new BaseUnit(UnitType.getByID(type), team);
                enemy.health = health;
                enemy.x = x;
                enemy.y = y;
                enemy.add(group);
            }
        }

        state.enemies = 0; //TODO display enemies correctly!
        state.wave = wave;
        state.wavetime = wavetime;

        if(!android && !headless)
            player.add();

        //map

        short width = stream.readShort();
        short height = stream.readShort();

        Tile[][] tiles = world.createTiles(width, height);

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++) {
                byte floorid = stream.readByte();
                byte wallid = stream.readByte();

                Tile tile = new Tile(x, y, floorid, wallid);

                if (wallid == Blocks.blockpart.id) {
                    tile.link = stream.readByte();
                }

                if (tile.entity != null) {
                    byte tr = stream.readByte();
                    byte team = Bits.getLeftByte(tr);
                    byte rotation = Bits.getRightByte(tr);
                    short health = stream.readShort();

                    tile.setTeam(Team.values()[team]);
                    tile.entity.health = health;
                    tile.setRotation(rotation);

                    if (tile.entity.inventory != null) tile.entity.inventory.read(stream);
                    if (tile.entity.power != null) tile.entity.power.read(stream);
                    if (tile.entity.liquid != null) tile.entity.liquid.read(stream);

                    tile.entity.read(stream);
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

        if(!headless) {
            stream.writeFloat(player.x); //player x/y
            stream.writeFloat(player.y);

            stream.writeInt(player.health); //player health

            stream.writeByte(control.upgrades().getWeapons().size - 1); //amount of weapons

            //start at 1, because the first weapon is always the starter - ignore that
            for (int i = 1; i < control.upgrades().getWeapons().size; i++) {
                stream.writeByte(control.upgrades().getWeapons().get(i).id); //weapon ordinal
            }
        }else{
            stream.writeFloat(world.getSpawnX());
            stream.writeFloat(world.getSpawnY());
            stream.writeInt(150);
            stream.writeByte(0);
        }

        //--INVENTORY--

        int l = state.inventory.getItems().length;
        int itemsize = 0;

        for(int i = 0; i < l; i ++){
            if(state.inventory.getItems()[i] > 0){
                itemsize ++;
            }
        }

        stream.writeByte(itemsize); //amount of items

        for(int i = 0; i < l; i ++){
            if(state.inventory.getItems()[i] > 0){
                stream.writeByte(i); //item ID
                stream.writeInt(state.inventory.getItems()[i]); //item amount
            }
        }

        //--ENEMIES--
        stream.writeByte(Team.values().length); //amount of total teams (backwards compatibility)

        for(Team team : Team.values()){
            EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];
            stream.writeInt(group.size()); //amount of units in the team group

            for(BaseUnit unit : group.all()){
                stream.writeByte(unit.type.id); //type
                stream.writeFloat(unit.x); //x
                stream.writeFloat(unit.y); //y
                stream.writeShort(unit.health); //health
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

                    if(tile.entity.inventory != null) tile.entity.inventory.write(stream);
                    if(tile.entity.power != null) tile.entity.power.write(stream);
                    if(tile.entity.liquid != null) tile.entity.liquid.write(stream);

                    tile.entity.write(stream);
                }
            }
        }
    }
}
