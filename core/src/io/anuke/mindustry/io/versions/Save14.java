package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.WorldGenerator;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.mindustry.world.blocks.types.Rock;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup.EntityContainer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class Save14 extends SaveFileVersion{

    public Save14(){
        super(14);
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        /*long loadTime = */
        stream.readLong();

        //general state
        byte mode = stream.readByte();
        byte mapid = stream.readByte();

        int wave = stream.readInt();
        float wavetime = stream.readFloat();

        //block header

        int blocksize = stream.readInt();

        IntMap<Block> map = new IntMap<>();

        for(int i = 0; i < blocksize; i ++){
            String name = readString(stream);
            int id = stream.readShort();

            map.put(id, Block.getByName(name));
        }

        float playerx = stream.readFloat();
        float playery = stream.readFloat();

        int playerhealth = stream.readInt();

        Vars.player.x = playerx;
        Vars.player.y = playery;
        Vars.player.health = playerhealth;
        state.mode = GameMode.values()[mode];
        Core.camera.position.set(playerx, playery, 0);

        //weapons

        control.upgrades().getWeapons().clear();
        control.upgrades().getWeapons().add(Weapon.blaster);
        Vars.player.weaponLeft = Vars.player.weaponRight = Weapon.blaster;

        int weapons = stream.readByte();

        for(int i = 0; i < weapons; i ++){
            control.upgrades().addWeapon((Weapon) Upgrade.getByID(stream.readByte()));
        }

        ui.hudfrag.updateWeapons();

        //inventory

        int totalItems = stream.readByte();

        Arrays.fill(state.inventory.getItems(), 0);

        for(int i = 0; i < totalItems; i ++){
            Item item = Item.getByID(stream.readByte());
            int amount = stream.readInt();
            state.inventory.getItems()[item.id] = amount;
        }

        ui.hudfrag.updateItems();

        //enemies

        Entities.clear();

        int enemies = stream.readInt();

        Array<Enemy> enemiesToUpdate = new Array<>();

        for(int i = 0; i < enemies; i ++){
            byte type = stream.readByte();
            int lane = stream.readByte();
            float x = stream.readFloat();
            float y = stream.readFloat();
            byte tier = stream.readByte();
            int health = stream.readShort();

            try{
                Enemy enemy = new Enemy(EnemyType.getByID(type));
                enemy.lane = lane;
                enemy.health = health;
                enemy.x = x;
                enemy.y = y;
                enemy.tier = tier;
                enemy.add(enemyGroup);
                enemiesToUpdate.add(enemy);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }

        state.enemies = enemies;
        state.wave = wave;
        state.wavetime = wavetime;

        if(!mobile)
            Vars.player.add();

        //map

        int seed = stream.readInt();

        world.loadMap(world.maps().getMap(mapid), seed);
        renderer.clearTiles();

        for(Enemy enemy : enemiesToUpdate){
            enemy.node = -2;
        }

        int rocks = stream.readInt();

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                //remove breakables like rocks
                if(tile.breakable()){
                    world.tile(x, y).setBlock(Blocks.air);
                }
            }
        }

        for(int i = 0; i < rocks; i ++){
            int pos = stream.readInt();
            Tile tile = world.tile(pos % world.width(), pos / world.width());
            if(tile == null) continue;
            Block result = WorldGenerator.rocks.get(tile.floor());
            if(result != null) tile.setBlock(result);
        }

        int tiles = stream.readInt();

        for(int i = 0; i < tiles; i ++){
            int pos = stream.readInt();
            int blockid = stream.readInt();

            Tile tile = world.tile(pos % world.width(), pos / world.width());
            tile.setBlock(map.get(blockid));

            if(blockid == Blocks.blockpart.id){
                tile.link = stream.readByte();
            }

            if(tile.entity != null){
                byte rotation = stream.readByte();
                short health = stream.readShort();
                int items = stream.readByte();

                tile.entity.health = health;
                tile.setRotation(rotation);

                for(int j = 0; j < items; j ++){
                    int itemid = stream.readByte();
                    int itemamount = stream.readInt();
                    tile.entity.items[itemid] = itemamount;
                }

                tile.entity.read(stream);
            }
        }
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        //--META--
        stream.writeInt(version); //version id
        stream.writeLong(TimeUtils.millis()); //last saved

        //--GENERAL STATE--
        stream.writeByte(state.mode.ordinal()); //gamemode
        stream.writeByte(world.getMap().id); //map ID

        stream.writeInt(state.wave); //wave
        stream.writeFloat(state.wavetime); //wave countdown

        //--BLOCK HEADER--

        stream.writeInt(Block.getAllBlocks().size);

        for(int i = 0; i < Block.getAllBlocks().size; i ++){
            Block block = Block.getAllBlocks().get(i);
            writeString(stream, block.name);
            stream.writeShort(block.id);
        }

        stream.writeFloat(Vars.player.x); //player x/y
        stream.writeFloat(Vars.player.y);

        stream.writeInt((int)Vars.player.health); //player health

        stream.writeByte(control.upgrades().getWeapons().size - 1); //amount of weapons

        //start at 1, because the first weapon is always the starter - ignore that
        for(int i = 1; i < control.upgrades().getWeapons().size; i ++){
            stream.writeByte(control.upgrades().getWeapons().get(i).id); //weapon ordinal
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

        EntityContainer<Enemy> enemies = enemyGroup.all();

        stream.writeInt(enemies.size()); //enemy amount

        for(int i = 0; i < enemies.size(); i ++){
            Enemy enemy = enemies.get(i);
            stream.writeByte(enemy.type.id); //type
            stream.writeByte(enemy.lane); //lane
            stream.writeFloat(enemy.x); //x
            stream.writeFloat(enemy.y); //y
            stream.writeByte(enemy.tier); //tier
            stream.writeShort((short)enemy.health); //health
        }

        //--MAP DATA--

        //seed
        stream.writeInt(world.getSeed());

        int totalblocks = 0;
        int totalrocks = 0;

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                if(tile.breakable()){
                    if(tile.block() instanceof Rock){
                        totalrocks ++;
                    }else{
                        totalblocks ++;
                    }
                }
            }
        }

        //amount of rocks
        stream.writeInt(totalrocks);

        //write all rocks
        for(int x = 0; x < world.width(); x ++) {
            for (int y = 0; y < world.height(); y++) {
                Tile tile = world.tile(x, y);

                if (tile.block() instanceof Rock) {
                    stream.writeInt(tile.packedPosition());
                }
            }
        }

        //write all blocks
        stream.writeInt(totalblocks);

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                if(tile.breakable() && !(tile.block() instanceof Rock)){

                    stream.writeInt(x + y*world.width()); //tile pos
                    stream.writeInt(tile.block().id); //block ID

                    if(tile.block() instanceof BlockPart) stream.writeByte(tile.link);

                    if(tile.entity != null){
                        stream.writeByte(tile.getRotation()); //rotation
                        stream.writeShort((short)tile.entity.health); //health
                        byte amount = 0;
                        for(int i = 0; i < tile.entity.items.length; i ++){
                            if(tile.entity.items[i] > 0) amount ++;
                        }
                        stream.writeByte(amount); //amount of items

                        for(int i = 0; i < tile.entity.items.length; i ++){
                            if(tile.entity.items[i] > 0){
                                stream.writeByte(i); //item ID
                                stream.writeInt(tile.entity.items[i]); //item amount
                            }
                        }

                        tile.entity.write(stream);
                    }
                }
            }
        }
    }
}
