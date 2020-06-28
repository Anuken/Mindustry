package mindustry.world.meta;

import arc.*;
import arc.func.*;
import mindustry.*;

import java.util.*;

/**
 * Like BuildVisiblity, but defines whether a block can be *placed*, with an extra message.
 * This is like defining a conditionally banned block.
 * */
public enum BuildPlaceability{
    always(() -> true),
    sectorCaptured(() -> Vars.state.rules.sector != null && Vars.state.rules.sector.isCaptured());

    private final Boolp placeability;

    BuildPlaceability(Boolp placeability){
        this.placeability = placeability;
    }

    public boolean placeable(){
        return placeability.get();
    }

    /** @return why this block is banned. */
    public String message(){
        return Core.bundle.get("unplaceable." + name().toLowerCase(Locale.ROOT));
    }

}
