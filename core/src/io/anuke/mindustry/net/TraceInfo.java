package io.anuke.mindustry.net;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;

public class TraceInfo {
    public int playerid;
    public String ip;
    public boolean modclient;
    public boolean android;

    public int fastShots;
    public long lastFastShot;

    public int totalBlocksBroken;
    public int structureBlocksBroken;
    public Block lastBlockBroken = Blocks.air;

    public int totalBlocksPlaced;
    public Block lastBlockPlaced = Blocks.air;

    public String uuid;

    public TraceInfo(String ip){
        this.ip = ip;
    }
}
