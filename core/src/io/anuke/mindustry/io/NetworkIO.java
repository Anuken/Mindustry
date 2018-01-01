package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.GameMode;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.mindustry.world.blocks.types.Rock;
import io.anuke.mindustry.world.blocks.types.production.Generator;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;

import java.io.*;

import static io.anuke.mindustry.Vars.android;
import static io.anuke.mindustry.io.SaveIO.enemyIDs;
import static io.anuke.mindustry.io.SaveIO.idEnemies;

public class NetworkIO {
    private static final int fileVersionID = 13;

    public static void write(OutputStream os){

        try(DataOutputStream stream = new DataOutputStream(os)){

            //--META--
            stream.writeInt(fileVersionID); //version id
            stream.writeFloat(Timers.time()); //timer time
            stream.writeLong(TimeUtils.millis()); //timestamp

            //--GENERAL STATE--
            stream.writeByte(Vars.control.getMode().ordinal()); //gamemode
            stream.writeByte(Vars.world.getMap().id); //map ID

            stream.writeInt(Vars.control.getWave()); //wave
            stream.writeFloat(Vars.control.getWaveCountdown()); //wave countdown

            //--INVENTORY--

            for(int i = 0; i < Vars.control.getItems().length; i ++){
                stream.writeInt(Vars.control.getItems()[i]);
            }

            //--ENEMIES--

            int totalEnemies = 0;

            for(Enemy entity : Vars.control.enemyGroup.all()){
                if(idEnemies.containsKey(entity.getClass())){
                    totalEnemies ++;
                }
            }

            stream.writeInt(totalEnemies); //enemy amount

            for(Enemy enemy : Vars.control.enemyGroup.all()){
                if(idEnemies.containsKey(enemy.getClass())){
                    stream.writeInt(enemy.id);
                    stream.writeByte(idEnemies.get(enemy.getClass())); //type
                    stream.writeByte(enemy.lane); //lane
                    stream.writeFloat(enemy.x); //x
                    stream.writeFloat(enemy.y); //y
                    stream.writeByte(enemy.tier); //tier
                    stream.writeShort(enemy.health); //health
                    stream.writeShort(enemy.node); //current node
                }
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

            //tile amount
            stream.writeInt(totalblocks);

            for(int x = 0; x < Vars.world.width(); x ++){
                for(int y = 0; y < Vars.world.height(); y ++){
                    Tile tile = Vars.world.tile(x, y);

                    if(tile.breakable() && !(tile.block() instanceof Rock)){

                        stream.writeInt(x + y*Vars.world.width()); //tile pos
                        //TODO will break if block number gets over BYTE_MAX
                        stream.writeByte(tile.block().id); //block ID

                        if(tile.block() instanceof BlockPart){
                            stream.writeByte(tile.link);
                        }

                        if(tile.entity != null){
                            stream.writeByte(tile.getRotation()); //placerot
                            stream.writeShort(tile.entity.health); //health

                            //items
                            for(int i = 0; i < tile.entity.items.length; i ++){
                                stream.writeInt(tile.entity.items[i]);
                            }

                            //timer data

                            //amount of active timers
                            byte times = 0;

                            for(; times < tile.entity.timer.getTimes().length; times ++){
                                if(tile.entity.timer.getTimes()[times] > 0){
                                    break;
                                }
                            }

                            stream.writeByte(times);

                            for(int i = 0; i < times; i ++){
                                stream.writeFloat(tile.entity.timer.getTimes()[times]);
                            }

                            tile.entity.write(stream);
                        }
                    }
                }
            }

        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static void load(FileHandle file){
        load(file.read());
    }

    //TODO GWT support
    public static void load(InputStream is){

        try(DataInputStream stream = new DataInputStream(is)){

            int version = stream.readInt();
            float timerTime = stream.readFloat();
            long timestamp = stream.readLong();

            Timers.resetTime(timerTime + (TimeUtils.timeSinceMillis(timestamp) / 1000f) * 60f);

            if(version != fileVersionID){
                throw new RuntimeException("Save file version mismatch!");
            }

            //general state
            byte mode = stream.readByte();
            byte mapid = stream.readByte();

            int wave = stream.readInt();
            float wavetime = stream.readFloat();

            Vars.control.setMode(GameMode.values()[mode]);

            //inventory
            for(int i = 0; i < Vars.control.getItems().length; i ++){
                Vars.control.getItems()[i] = stream.readInt();
            }

            Vars.ui.updateItems();

            //enemies

            Entities.clear();

            int enemies = stream.readInt();

            Array<Enemy> enemiesToUpdate = new Array<>();

            for(int i = 0; i < enemies; i ++){
                int id = stream.readInt();
                byte type = stream.readByte();
                int lane = stream.readByte();
                float x = stream.readFloat();
                float y = stream.readFloat();
                byte tier = stream.readByte();
                short health = stream.readShort();
                short node = stream.readShort();

                try{
                    Enemy enemy = ClassReflection.newInstance(enemyIDs.get(type));
                    enemy.id = id;
                    enemy.lane = lane;
                    enemy.health = health;
                    enemy.x = x;
                    enemy.y = y;
                    enemy.tier = tier;
                    enemy.node = node;
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

            for(int x = 0; x < Vars.world.width(); x ++){
                for(int y = 0; y < Vars.world.height(); y ++){
                    Tile tile = Vars.world.tile(x, y);

                    //remove breakables like rocks
                    if(tile.breakable()){
                        Vars.world.tile(x, y).setBlock(Blocks.air);
                    }
                }
            }

            int rocks = stream.readInt();

            for(int i = 0; i < rocks; i ++){
                int pos = stream.readInt();
                Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());
                Block result = io.anuke.mindustry.world.Generator.rocks.get(tile.floor());
                if(result != null) tile.setBlock(result);
            }

            int tiles = stream.readInt();

            for(int i = 0; i < tiles; i ++){
                int pos = stream.readInt();
                byte blockid = stream.readByte();

                Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());
                tile.setBlock(Block.getByID(blockid));

                if(tile.block() == Blocks.blockpart){
                    tile.link = stream.readByte();
                }

                if(tile.entity != null){
                    byte rotation = stream.readByte();
                    short health = stream.readShort();

                    tile.entity.health = health;
                    tile.setRotation(rotation);

                    for(int j = 0; j < tile.entity.items.length; j ++){
                        tile.entity.items[j] = stream.readInt();
                    }

                    byte timers = stream.readByte();
                    for(int time = 0; time < timers; time ++){
                        tile.entity.timer.getTimes()[time] = stream.readFloat();
                    }

                    tile.entity.read(stream);
                }
            }

        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
