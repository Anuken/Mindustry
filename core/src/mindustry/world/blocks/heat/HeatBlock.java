package mindustry.world.blocks.heat;

/** Basic interface for any block that produces heat.*/
public interface HeatBlock{
    float heat();
    /** @return heat as a fraction of max heat */
    float heatFrac();
}
