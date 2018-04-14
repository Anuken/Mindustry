package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class ItemAnimationEffect extends TimedEntity {
    private static final float size = 5f;

    private final Vector2 vec = new Vector2();
    private final Vector2 from = new Vector2();
    private final Vector2 to = new Vector2();
    private final Item item;

    public ItemAnimationEffect(Item item, float x, float y, float tox, float toy) {
        this.x = x;
        this.y = y;
        this.item = item;
        from.set(x, y);
        to.set(tox, toy);
        lifetime = 40f;
    }

    @Override
    public void update() {
        super.update();

        vec.set(from).interpolate(to, fin(), Interpolation.fade);
        x = vec.x;
        y = vec.y;
    }

    @Override
    public void draw() {
        float s = size * Mathf.curve(fout(), 0.1f);
        Draw.rect(item.region, x, y, s, s);
    }

    @Override
    public <T extends Entity> T add() {
        return super.add(Vars.effectGroup);
    }
}
