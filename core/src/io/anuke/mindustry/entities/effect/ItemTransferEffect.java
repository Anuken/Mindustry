package io.anuke.mindustry.entities.effect;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class ItemTransferEffect extends Entity {
    private static final float size = 5f;
    private static final float alpha = 0.1f;

    private final Item item;
    private final Entity target;

    public ItemTransferEffect(Item item, float x, float y, Entity target) {
        this.x = x;
        this.y = y;
        this.item = item;
        this.target = target;
    }

    @Override
    public void update() {

        x = Mathf.lerpDelta(x, target.x, alpha);
        y = Mathf.lerpDelta(y, target.y, alpha);

        if(distanceTo(target) <= 2f){
            remove();
        }
    }

    @Override
    public void draw() {
        float s = size;
        Draw.rect(item.region, x, y, s, s);
    }

    @Override
    public <T extends Entity> T add() {
        return super.add(Vars.effectGroup);
    }
}
