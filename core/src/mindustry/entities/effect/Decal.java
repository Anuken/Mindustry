package mindustry.entities.effect;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.entities.EntityGroup;
import mindustry.entities.type.TimedEntity;
import mindustry.entities.traits.BelowLiquidTrait;
import mindustry.entities.traits.DrawTrait;
import mindustry.graphics.Pal;

import static mindustry.Vars.groundEffectGroup;

/**
 * Class for creating block rubble on the ground.
 */
public abstract class Decal extends TimedEntity implements BelowLiquidTrait, DrawTrait{

    @Override
    public float lifetime(){
        return 3600;
    }

    @Override
    public void draw(){
        Draw.color(Pal.rubble.r, Pal.rubble.g, Pal.rubble.b, 1f - Mathf.curve(fin(), 0.98f));
        drawDecal();
        Draw.color();
    }

    @Override
    public EntityGroup targetGroup(){
        return groundEffectGroup;
    }

    abstract void drawDecal();
}
