package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.util.Position;

public class ItemTransfer extends TimedEntity{
    private Vector2 from = new Vector2();
    private Vector2 current = new Vector2();
    private Vector2 tovec = new Vector2();
    private Item item;
    private Position to;
    private Callable done;

    public static void create(Item item, float fromx, float fromy, Position to, Callable done){
        ItemTransfer tr = Pools.obtain(ItemTransfer.class);
        tr.item = item;
        tr.from.set(fromx, fromy);
        tr.to = to;
        tr.done = done;
        tr.lifetime = 60f;
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
        Pools.free(this);
    }

    @Override
    public void update() {
        super.update();
        current.set(from).lerp(tovec.set(to.getX(), to.getY()), fin());
        set(current.x, current.y);
    }

    @Override
    public void draw() {

    }

    @Override
    public <T extends Entity> T add() {
        return super.add(Vars.effectGroup);
    }
}
