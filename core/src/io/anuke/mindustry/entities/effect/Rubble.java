package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.groundEffectGroup;

/**Class for creating block rubble on the ground.*/
public class Rubble extends TimedEntity implements BelowLiquidEffect{
    private static final Color color = Color.valueOf("52504e");
    private int size;

    /**Creates a rubble effect at a position. Provide a block size to use.*/
    public static void create(float x, float y, int size){
        Rubble rubble = new Rubble();
        rubble.size = size;
        rubble.set(x, y);
        rubble.add();
    }

    public Rubble(){
        lifetime = 7000f;
    }

    @Override
    public void draw(){
        String region = "rubble-" + size + "-" + Mathf.randomSeed(id, 0, 1);

        if(!Draw.hasRegion(region)){
            remove();
            return;
        }

        Draw.color(color.r, color.g, color.b, 1f-Mathf.curve(fin(), 0.98f));
        Draw.rect(region, x, y, Mathf.randomSeed(id, 0, 4) * 90);
        Draw.color();
    }

    @Override
    public EntityGroup targetGroup() {
        return groundEffectGroup;
    }
}
