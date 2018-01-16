package io.anuke.mindustry.net;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ByteArray;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.WorldGenerator;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.mindustry.world.blocks.types.Rock;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;

import java.io.*;

public class NetworkIO {
    private static final int fileVersionID = 16;

    public static void write(int playerID, ByteArray upgrades, OutputStream os){

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
            stream.writeInt(Vars.control.enemyGroup.amount()); //enemy amount

            stream.writeBoolean(Vars.control.isFriendlyFire()); //friendly fire state
            stream.writeInt(playerID); //player remap ID

            //--INVENTORY--

            for(int i = 0; i < Vars.control.getItems().length; i ++){ //items
                stream.writeInt(Vars.control.getItems()[i]);
            }

            stream.writeByte(upgrades.size); //upgrade data

            for(int i = 0; i < upgrades.size; i ++){
                stream.writeByte(upgrades.get(i));
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
                            stream.writeShort(tile.getPackedData());
                            stream.writeShort(tile.entity.health); //health

                            //items
                            for(int i = 0; i < tile.entity.items.length; i ++){
                                stream.writeInt(tile.entity.items[i]);
                            }

                            //timer data

                            //amount of active timers
                            byte times = 0;

                            for(; times < tile.entity.timer.getTimes().length; times ++){
                                if(tile.entity.timer.getTimes()[times] <= 1){
                                    break;
                                }
                            }

                            stream.writeByte(times);

                            for(int i = 0; i < times; i ++){
                                stream.writeFloat(tile.entity.timer.getTimes()[i]);
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


    public static void load(InputStream is){

        try(DataInputStream stream = new DataInputStream(is)){

            int version = stream.readInt();
            float timerTime = stream.readFloat();
            long timestamp = stream.readLong();

            Timers.resetTime(timerTime + (TimeUtils.timeSinceMillis(timestamp) / 1000f) * 60f);

            if(version != fileVersionID){
                throw new RuntimeException("Netcode version mismatch!");
            }

            //general state
            byte mode = stream.readByte();
            byte mapid = stream.readByte();

            int wave = stream.readInt();
            float wavetime = stream.readFloat();
            int enemies = stream.readInt();
            boolean friendlyfire = stream.readBoolean();

            Vars.control.setWaveData(enemies, wave, wavetime);
            Vars.control.setMode(GameMode.values()[mode]);
            Vars.control.setFriendlyFire(friendlyfire);

            int pid = stream.readInt();

            //inventory
            for(int i = 0; i < Vars.control.getItems().length; i ++){
                Vars.control.getItems()[i] = stream.readInt();
            }

            Vars.ui.hudfrag.updateItems();

            Vars.control.getWeapons().clear();
            Vars.control.getWeapons().add(Weapon.blaster);

            byte weapons = stream.readByte();

            for(int i = 0; i < weapons; i ++){
                Vars.control.getWeapons().add((Weapon) Upgrade.getByID(stream.readByte()));
            }

            Vars.player.weaponLeft = Vars.player.weaponRight = Vars.control.getWeapons().peek();
            Vars.ui.hudfrag.updateWeapons();

            Entities.clear();
            Vars.player.id = pid;
            Vars.player.add();

            //map

            int seed = stream.readInt();

            Vars.world.loadMap(Vars.world.maps().getMap(mapid), seed);
            Vars.renderer.clearTiles();

            Vars.player.set(Vars.control.getCore().worldx(), Vars.control.getCore().worldy());

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
                Block result = WorldGenerator.rocks.get(tile.floor());
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
                    short data = stream.readShort();
                    short health = stream.readShort();

                    tile.entity.health = health;
                    tile.setPackedData(data);

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
