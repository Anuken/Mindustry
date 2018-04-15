package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class ItemAnimationEffect extends TimedEntity {
    private static final float size = 5f;

    private final Vector2 vec = new Vector2();
    private final Vector2 from = new Vector2();
    private final Vector2 to = new Vector2();
    private final Item item;
    private final Callable removed;

    public Interpolation interp = Interpolation.fade;
    public float endSize = 0.9f;

    public ItemAnimationEffect(Item item, float x, float y, float tox, float toy, Callable removed) {
        this.x = x;
        this.y = y;
        this.item = item;
        this.removed = removed;
        from.set(x, y);
        to.set(tox, toy);
        lifetime = 40f;
    }

    @Override
    public void removed() {
        super.removed();
        removed.run();
    }

    @Override
    public void update() {
        super.update();

        vec.set(from).interpolate(to, fin(), interp);
        x = vec.x;
        y = vec.y;
    }

    @Override
    public void draw() {
        float s = size * (1f-Mathf.curve(fin(), endSize));
        Draw.rect(item.region, x, y, s, s);
    }

    @Override
    public <T extends Entity> T add() {
        return super.add(Vars.effectGroup);
    }
}
