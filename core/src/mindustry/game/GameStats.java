package mindustry.game;

import arc.struct.*;
import mindustry.type.*;
import mindustry.world.*;

public class GameStats{
    /** Enemy (red team) units destroyed. */
    public int enemyUnitsDestroyed;
    /** Total waves lasted. */
    public int wavesLasted;
    /** Friendly buildings fully built. */
    public int buildingsBuilt;
    /** Friendly buildings fully deconstructed. */
    public int buildingsDeconstructed;
    /** Friendly buildings destroyed. */
    public int buildingsDestroyed;
    /** Total units created by any means. */
    public int unitsCreated;
    /** Record of blocks that have been placed by count. Used for objectives only. */
    public ObjectIntMap<Block> placedBlockCount = new ObjectIntMap<>();
    /** Record of enemy blocks that have been destroyed (from any source) by count. */
    public ObjectIntMap<Block> destroyedBlockCount = new ObjectIntMap<>();
    /**
     * Record of items that have entered the core through transport blocks. Used for objectives only.
     * This can easily be ""spoofed"" with unloaders, so don't use it for anything remotely important.
     * */
    public ObjectIntMap<Item> coreItemCount = new ObjectIntMap<>();

    public int getPlaced(Block block){
        return placedBlockCount.get(block, 0);
    }

    public int getDestroyed(Block block){
        return destroyedBlockCount.get(block, 0);
    }

    /**
     * Helper method to calculate the efficiency ratio of the current session.
     * This compares total resources produced against total units lost.
     * * @param itemsProduced Total amount of raw materials gathered.
     * @param unitsDestroyed Total count of friendly units lost in battle.
     * @return A performance score as a float.
     */
    public float calculateEfficiency(long itemsProduced, int unitsDestroyed) {
        if (unitsDestroyed == 0) return itemsProduced;
        return (float) itemsProduced / unitsDestroyed;
    }
}
