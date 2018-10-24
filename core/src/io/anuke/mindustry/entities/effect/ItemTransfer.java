package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.TimedEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.PosTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;

import static io.anuke.mindustry.Vars.effectGroup;
import static io.anuke.mindustry.Vars.threads;

public class ItemTransfer extends TimedEntity implements DrawTrait{
    private Vector2 from = new Vector2();
    private Vector2 current = new Vector2();
    private Vector2 tovec = new Vector2();
    private Item item;
    private float seed;
    private PosTrait to;
    private Runnable done;

    public ItemTransfer(){
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemEffect(Item item, float x, float y, Unit to){
        if(to == null) return;
        create(item, x, y, to, () -> {
        });
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemToUnit(Item item, float x, float y, Unit to){
        if(to == null) return;
        create(item, x, y, to, () -> to.inventory.addItem(item, 1));
    }

    @Remote(called = Loc.server)
    public static void transferItemTo(Item item, int amount, float x, float y, Tile tile){
        if(tile == null || tile.entity == null || tile.entity.items == null) return;
        for(int i = 0; i < Mathf.clamp(amount / 3, 1, 8); i++){
            Timers.run(i * 3, () -> create(item, x, y, tile, () -> {
            }));
        }
        tile.entity.items.add(item, amount);
    }

    public static void create(Item item, float fromx, float fromy, PosTrait to, Runnable done){
        ItemTransfer tr = Pooling.obtain(ItemTransfer.class, ItemTransfer::new);
        tr.item = item;
        tr.from.set(fromx, fromy);
        tr.to = to;
        tr.done = done;
        tr.seed = Mathf.range(1f);
        tr.add();
    }

    @Override
    public float lifetime(){
        return 60;
    }

    @Override
    public void reset(){
        super.reset();
        item = null;
        to = null;
        done = null;
        from.setZero();
        current.setZero();
        tovec.setZero();
    }

    @Override
    public void removed(){
        if(done != null){
            threads.run(done);
        }
        Pooling.free(this);
    }

    @Override
    public void update(){
        if(to == null){
            remove();
            return;
        }

        super.update();
        current.set(from).interpolate(tovec.set(to.getX(), to.getY()), fin(), Interpolation.pow3);
        current.add(tovec.set(to.getX(), to.getY()).sub(from).nor().rotate90(1).scl(seed * fslope() * 10f));
        set(current.x, current.y);
    }

    @Override
    public void draw(){
        float length = fslope() * 6f;
        float angle = current.set(x, y).sub(from).angle();
        Draw.color(Palette.accent);
        Lines.stroke(fslope() * 2f);

        Lines.circle(x, y, fslope() * 2f);
        Lines.lineAngleCenter(x, y, angle, length);
        Lines.lineAngle(x, y, angle, fout() * 6f);

        Draw.color(item.color);
        Fill.circle(x, y, fslope() * 1.5f);

        Draw.reset();
    }

    @Override
    public EntityGroup targetGroup(){
        return effectGroup;
    }
}
