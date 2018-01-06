package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.mindustry.world.blocks.types.Rock;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.Entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.android;

public class Save13 extends SaveFileVersion {

    public Save13(){
        super(13);
    }

    @Override
    public void read(DataInputStream stream) throws IOException {

        int version = stream.readInt();
        /*long loadTime = */stream.readLong();

        if(version != this.version){
            throw new RuntimeException("Save file version mismatch!");
        }

        //general state
        byte mode = stream.readByte();
        byte mapid = stream.readByte();

        int wave = stream.readInt();
        float wavetime = stream.readFloat();

        float playerx = stream.readFloat();
        float playery = stream.readFloat();

        int playerhealth = stream.readInt();

        Vars.player.x = playerx;
        Vars.player.y = playery;
        Vars.player.health = playerhealth;
        Vars.control.setMode(GameMode.values()[mode]);
        Core.camera.position.set(playerx, playery, 0);

        //weapons

        Vars.control.getWeapons().clear();
        Vars.control.getWeapons().add(Weapon.blaster);
        Vars.player.weaponLeft = Vars.player.weaponRight = Weapon.blaster;

        int weapons = stream.readByte();

        for(int i = 0; i < weapons; i ++){
            Vars.control.addWeapon((Weapon) Upgrade.getByID(stream.readByte()));
        }

        Vars.ui.weaponfrag.update();

        //inventory

        int totalItems = stream.readByte();

        Arrays.fill(Vars.control.getItems(), 0);

        for(int i = 0; i < totalItems; i ++){
            Item item = Item.getByID(stream.readByte());
            int amount = stream.readInt();
            Vars.control.getItems()[item.id] = amount;
        }

        Vars.ui.hudfrag.updateItems();

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
                enemy.add(Vars.control.enemyGroup);
                enemiesToUpdate.add(enemy);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }

        Vars.control.setWaveData(enemies, wave, wavetime);

        if(!android)
            Vars.player.add();

        //map

        int seed = stream.readInt();

        Vars.world.loadMap(Vars.world.maps().getMap(mapid), seed);
        Vars.renderer.clearTiles();

        for(Enemy enemy : enemiesToUpdate){
            enemy.node = -2;
        }

        int rocks = stream.readInt();

        for(int x = 0; x < Vars.world.width(); x ++){
            for(int y = 0; y < Vars.world.height(); y ++){
                Tile tile = Vars.world.tile(x, y);

                //remove breakables like rocks
                if(tile.breakable()){
                    Vars.world.tile(x, y).setBlock(Blocks.air);
                }
            }
        }

        for(int i = 0; i < rocks; i ++){
            int pos = stream.readInt();
            Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());
            Block result = Generator.rocks.get(tile.floor());
            if(result != null) tile.setBlock(result);
        }

        int tiles = stream.readInt();

        for(int i = 0; i < tiles; i ++){
            int pos = stream.readInt();
            int blockid = stream.readInt();

            Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());
            tile.setBlock(BlockLoader.getByOldID(blockid));

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
        stream.writeByte(Vars.control.getMode().ordinal()); //gamemode
        stream.writeByte(Vars.world.getMap().id); //map ID

        stream.writeInt(Vars.control.getWave()); //wave
        stream.writeFloat(Vars.control.getWaveCountdown()); //wave countdown

        stream.writeFloat(Vars.player.x); //player x/y
        stream.writeFloat(Vars.player.y);

        stream.writeInt(Vars.player.health); //player health

        stream.writeByte(Vars.control.getWeapons().size - 1); //amount of weapons

        //start at 1, because the first weapon is always the starter - ignore that
        for(int i = 1; i < Vars.control.getWeapons().size; i ++){
            stream.writeByte(Vars.control.getWeapons().get(i).id); //weapon ordinal
        }

        //--INVENTORY--

        int l = Vars.control.getItems().length;
        int itemsize = 0;

        for(int i = 0; i < l; i ++){
            if(Vars.control.getItems()[i] > 0){
                itemsize ++;
            }
        }

        stream.writeByte(itemsize); //amount of items

        for(int i = 0; i < l; i ++){
            if(Vars.control.getItems()[i] > 0){
                stream.writeByte(i); //item ID
                stream.writeInt(Vars.control.getItems()[i]); //item amount
            }
        }

        //--ENEMIES--
        Array<Enemy> enemies = Vars.control.enemyGroup.all();

        stream.writeInt(enemies.size); //enemy amount

        for(int i = 0; i < enemies.size; i ++){
            Enemy enemy = enemies.get(i);
            stream.writeByte(enemy.type.id); //type
            stream.writeByte(enemy.lane); //lane
            stream.writeFloat(enemy.x); //x
            stream.writeFloat(enemy.y); //y
            stream.writeByte(enemy.tier); //tier
            stream.writeShort(enemy.health); //health
        }

        //--MAP DATA--

        //seed
        stream.writeInt(Vars.world.getSeed());

        int totalblocks = 0;
        int totalrocks = 0;

        for(int x = 0; x < Vars.world.width(); x ++){
            for(int y = 0; y < Vars.world.height(); y ++){
                Tile tile = Vars.world.tile(x, y);

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
        for(int x = 0; x < Vars.world.width(); x ++) {
            for (int y = 0; y < Vars.world.height(); y++) {
                Tile tile = Vars.world.tile(x, y);

                if (tile.block() instanceof Rock) {
                    stream.writeInt(tile.packedPosition());
                }
            }
        }

        //write all blocks
        stream.writeInt(totalblocks);

        for(int x = 0; x < Vars.world.width(); x ++){
            for(int y = 0; y < Vars.world.height(); y ++){
                Tile tile = Vars.world.tile(x, y);

                if(tile.breakable() && !(tile.block() instanceof Rock)){

                    stream.writeInt(x + y*Vars.world.width()); //tile pos
                    stream.writeInt(tile.block().id); //block ID

                    if(tile.block() instanceof BlockPart) stream.writeByte(tile.link);

                    if(tile.entity != null){
                        stream.writeByte(tile.getRotation()); //rotation
                        stream.writeShort(tile.entity.health); //health
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
