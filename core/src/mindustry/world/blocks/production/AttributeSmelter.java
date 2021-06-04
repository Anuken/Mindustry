package mindustry.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;

/** @deprecated use AttributeCrafter instead, this is only a transition class. No flame effects are drawn, to encourage transition! */
@Deprecated
public class AttributeSmelter extends AttributeCrafter{
    //parameters are kept for compatibility but deliberately unused
    public Color flameColor = Color.valueOf("ffc999");
    public @Load("@-top") TextureRegion topRegion;
    //compat
    public float maxHeatBoost = 1f;

    public AttributeSmelter(String name){
        super(name);
    }

    //unused, kept for compatibility
    @Deprecated
    public class AttributeSmelterBuild extends AttributeCrafterBuild{

    }
}
