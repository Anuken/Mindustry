package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Position;

public class ItemTransfer extends TimedEntity{
    private Vector2 from = new Vector2();
    private Vector2 current = new Vector2();
    private Vector2 tovec = new Vector2();
    private Item item;
    private float seed;
    private Position to;
    private Callable done;

    public static void create(Item item, float fromx, float fromy, Position to, Callable done){
        ItemTransfer tr = Pools.obtain(ItemTransfer.class);
        tr.item = item;
        tr.from.set(fromx, fromy);
        tr.to = to;
        tr.done = done;
        tr.lifetime = 60f;
        tr.seed = Mathf.range(1f);
        tr.add();
    }

    private ItemTransfer(){}

    @Override
    public void reset() {
        super.reset();
        item = null;
        to = null;
        done = null;
        from.setZero();
        current.setZero();
        tovec.setZero();
    }

    @Override
    public void removed() {
        done.run();
        Pools.free(this);
    }

    @Override
    public void update() {
        super.update();
        current.set(from).interpolate(tovec.set(to.getX(), to.getY()), fin(), Interpolation.pow3);
        current.add(tovec.set(to.getX(), to.getY()).sub(from).nor().rotate90(1).scl(seed * fslope() * 10f));
        set(current.x, current.y);
    }

    @Override
    public void draw() {
        float length = fslope()*6f;
        float angle = current.set(x, y).sub(from).angle();
        Draw.color(Palette.accent);
        Lines.stroke(fslope()*2f);

        Lines.circle(x, y, fslope()*2f);
        Lines.lineAngleCenter(x, y, angle, length);
        Lines.lineAngle(x, y, angle, fout()*6f);

        Draw.color(item.color);
        Fill.circle(x, y, fslope()*1.5f);

        Draw.reset();
    }

    @Override
    public <T extends Entity> T add() {
        return super.add(Vars.effectGroup);
    }
}
