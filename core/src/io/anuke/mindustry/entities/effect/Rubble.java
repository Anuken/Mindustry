package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.groundEffectGroup;

public class Rubble extends TimedEntity{
    private static final Color color = Color.valueOf("52504e");
    private int size;

    public static void create(float x, float y, int size){
        Rubble rubble = new Rubble();
        rubble.size = size;
        rubble.set(x + Mathf.range(1), y + Mathf.range(1)).add();
    }

    private Rubble(){
        lifetime = 7000f;
    }

    @Override
    public void draw(){
        Draw.color(color.r, color.g, color.b, 1f-Mathf.curve(fin(), 0.98f));
        Draw.rect("rubble-" + size + "-" + Mathf.randomSeed(id, 0, 1), x, y, Mathf.randomSeed(id, 0, 4) * 90);
        Draw.color();
    }

    @Override
    public Rubble add() {
        return add(groundEffectGroup);
    }
}
