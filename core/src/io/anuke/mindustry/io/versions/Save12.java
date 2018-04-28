package io.anuke.mindustry.io.versions;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.io.BlockLoader;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup.EntityContainer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class Save12 extends SaveFileVersion {

    public Save12(){
        super(12);
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        /*long loadTime = */stream.readLong();

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
        state.mode = GameMode.values()[mode];
        Core.camera.position.set(playerx, playery, 0);

        //weapons

        control.upgrades().getWeapons().clear();
        control.upgrades().getWeapons().add(Weapon.blaster);
        Vars.player.weaponLeft = Vars.player.weaponRight = Weapon.blaster;

        int weapons = stream.readByte();

        for(int i = 0; i < weapons; i ++){
            control.upgrades().addWeapon((Weapon)Upgrade.getByID(stream.readByte()));
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
            int health = stream.readInt();

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
        int tiles = stream.readInt();

        world.loadMap(world.maps().getMap(mapid), seed);
        renderer.clearTiles();

        for(Enemy enemy : enemiesToUpdate){
            enemy.node = -2;
        }

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                //remove breakables like rocks
                if(tile.breakable()){
                    world.tile(x, y).setBlock(Blocks.air);
                }
            }
        }

        for(int i = 0; i < tiles; i ++){
            int pos = stream.readInt();
            byte link = stream.readByte();
            boolean hasEntity = stream.readBoolean();
            int blockid = stream.readInt();

            Tile tile = world.tile(pos % world.width(), pos / world.width());
            tile.setBlock(BlockLoader.getByOldID(blockid));
            tile.link = link;

            if(hasEntity){
                byte rotation = stream.readByte();
                int health = stream.readInt();
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
            stream.writeInt((int)enemy.health); //health
        }

        //--MAP DATA--

        //seed
        stream.writeInt(world.getSeed());

        int totalblocks = 0;

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                if(tile.breakable()){
                    totalblocks ++;
                }
            }
        }

        //tile amount
        stream.writeInt(totalblocks);

        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                Tile tile = world.tile(x, y);

                if(tile.breakable()){

                    stream.writeInt(x + y*world.width()); //tile pos
                    stream.writeByte(tile.link);
                    stream.writeBoolean(tile.entity != null); //whether it has a tile entity
                    stream.writeInt(tile.block().id); //block ID

                    if(tile.entity != null){
                        stream.writeByte(tile.getRotation()); //rotation
                        stream.writeInt((int)tile.entity.health); //health
                        int amount = 0;
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
