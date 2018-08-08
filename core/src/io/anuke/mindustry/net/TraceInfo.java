package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.IntIntMap;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.world.Block;

public class TraceInfo{
    public int playerid;
    public String ip;
    public boolean modclient;
    public boolean android;

    public IntIntMap fastShots = new IntIntMap();

    public int totalBlocksBroken;
    public int structureBlocksBroken;
    public Block lastBlockBroken = Blocks.air;

    public int totalBlocksPlaced;
    public Block lastBlockPlaced = Blocks.air;

    public String uuid;

    public TraceInfo(String uuid){
        this.uuid = uuid;
    }
}
