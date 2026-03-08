package mindustry.game;

import arc.struct.*;
import mindustry.type.*;
import mindustry.world.*;

/** Statistics for a specific planet's campaign. */
public class CampaignStats{
    /** Enemy units destroyed by type. */
    public ObjectIntMap<UnitType> enemyUnitsDestroyed = new ObjectIntMap<>();
    /** Record of enemy blocks that have been destroyed (from any source) by count. */
    public ObjectIntMap<Block> enemyBuildingsDestroyed = new ObjectIntMap<>();
    /** Player team units produced by type. */
    public ObjectIntMap<UnitType> unitsProduced = new ObjectIntMap<>();
    /** Record of blocks that have been placed by count. */
    public ObjectIntMap<Block> buildingsBuilt = new ObjectIntMap<>();
    /** Record of blocks that have been placed by count. */
    public ObjectIntMap<Block> buildingsDeconstructed = new ObjectIntMap<>();
    /** Record of blocks that have been placed by count. */
    public ObjectIntMap<Block> buildingsDestroyed = new ObjectIntMap<>();
    /** Total campaign playtime in milliseconds. */
    public long playtime;
    /** Total game-overs. */
    public int sectorsLost;
    /** Total times a sector has been captured. If you lose (or get invaded) and re-capture something, this still counts. */
    public int sectorsCaptured;
    /** Total waves lasted. */
    public int wavesLasted;
}
