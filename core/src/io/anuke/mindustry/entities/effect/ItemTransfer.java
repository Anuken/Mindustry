package io.anuke.mindustry.entities.effect;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Position;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.TimedEntity;
import io.anuke.mindustry.entities.traits.DrawTrait;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public class ItemTransfer extends TimedEntity implements DrawTrait{
    private Vector2 from = new Vector2();
    private Vector2 current = new Vector2();
    private Vector2 tovec = new Vector2();
    private Item item;
    private float seed;
    private Position to;
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
        create(item, x, y, to, () -> to.addItem(item));
    }

    @Remote(called = Loc.server)
    public static void transferItemTo(Item item, int amount, float x, float y, Tile tile){
        if(tile == null || tile.entity == null || tile.entity.items == null) return;
        for(int i = 0; i < Mathf.clamp(amount / 3, 1, 8); i++){
            Time.run(i * 3, () -> create(item, x, y, tile, () -> {}));
        }
        tile.entity.items.add(item, amount);
    }

    public static void create(Item item, float fromx, float fromy, Position to, Runnable done){
        ItemTransfer tr = Pools.obtain(ItemTransfer.class, ItemTransfer::new);
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
            done.run();
        }
        Pools.free(this);
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
        Lines.stroke(fslope() * 2f, Pal.accent);

        Lines.circle(x, y, fslope() * 2f);

        Draw.color(item.color);
        Fill.circle(x, y, fslope() * 1.5f);

        Draw.reset();
    }

    @Override
    public EntityGroup targetGroup(){
        return effectGroup;
    }
}
