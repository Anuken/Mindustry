package mindustry.entities.effect;

import mindustry.annotations.Annotations.Loc;
import mindustry.annotations.Annotations.Remote;
import arc.graphics.g2d.*;
import arc.math.Interpolation;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.pooling.Pools;
import mindustry.entities.*;
import mindustry.entities.type.TimedEntity;
import mindustry.entities.traits.DrawTrait;
import mindustry.entities.type.Unit;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class ItemTransfer extends TimedEntity implements DrawTrait{
    private Vec2 from = new Vec2();
    private Vec2 current = new Vec2();
    private Vec2 tovec = new Vec2();
    private Item item;
    private float seed;
    private Position to;
    private Runnable done;

    public ItemTransfer(){}

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

    @Remote(called = Loc.server, unreliable = true)
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
