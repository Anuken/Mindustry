package mindustry.world.blocks.legacy;

import mindustry.world.*;

/** Any subclass of this will be removed upon world load. */
public class LegacyBlock extends Block{

    public LegacyBlock(String name){
        super(name);
        inEditor = false;
        generateIcons = false;
    }

    /** Removes this block from the world, or replaces it with something else. */
    public void removeSelf(Tile tile){
        tile.remove();
    }
}
