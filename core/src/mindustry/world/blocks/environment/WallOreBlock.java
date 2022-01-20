package mindustry.world.blocks.environment;

import arc.*;
import mindustry.type.*;

/**An overlay ore that draws on top of walls. */
public class WallOreBlock extends OreBlock{

    public WallOreBlock(Item ore){
        super("ore-wall-" + ore.name, ore);
    }

    //mods only
    public WallOreBlock(String name){
        super(name);
    }

    @Override
    public void init(){
        super.init();

        this.localizedName = this.localizedName + " " + Core.bundle.get("wallore");
    }
}
