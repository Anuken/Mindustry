package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

public class ResupplyPoint extends Block{
    private static Rectangle rect = new Rectangle();

    protected int timerSupply = timers++;
    protected int timerTarget = timers++;

    protected float supplyRadius = 50f;
    protected float supplyInterval = 10f;

    public ResupplyPoint(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.resupplyPoint, BlockFlag.target);
        layer = Layer.laser;
        hasItems = true;
        hasPower = true;
        powerCapacity = 20f;

        consumes.power(0.02f);
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), supplyRadius);
        Draw.color();
    }

    @Override
    public void drawLayer(Tile tile){
        ResupplyPointEntity entity = tile.entity();

        if(entity.strength > 0f){
            float ang = entity.angleTo(entity.lastx, entity.lasty);
            float len = 5f;
            float x1 = tile.drawx() + Angles.trnsx(ang, len), y1 = tile.drawy() + Angles.trnsy(ang, len);
            float dstTo = Vector2.dst(x1, y1, entity.lastx, entity.lasty);
            float space = 4f;

            float xf = entity.lastx - x1, yf = entity.lasty - y1;

            Shapes.laser("transfer", "transfer-end",
                    x1, y1, entity.lastx, entity.lasty, entity.strength);

            Draw.color(Palette.accent);
            for(int i = 0; i < dstTo / space - 1; i++){
                float fract = (i * space) / dstTo + ((Timers.time() / 90f) % (space / dstTo));
                Draw.alpha(Mathf.clamp(fract * 1.5f));
                Draw.rect("transfer-arrow", x1 + fract * xf, y1 + fract * yf,
                        8, 8 * entity.strength, ang);
            }

            Draw.color();

        }
    }

    @Override
    public void update(Tile tile){
        ResupplyPointEntity entity = tile.entity();

        if(!validTarget(entity, entity.target) || entity.target.distanceTo(tile) > supplyRadius){
            entity.target = null;
        }else if(entity.target != null && entity.strength > 0.5f){

            if(entity.timer.get(timerSupply, supplyInterval)){
                for(int i = 0; i < Item.all().size; i++){
                    Item item = Item.getByID(i);
                    if(tile.entity.items.has(item) && entity.target.acceptsAmmo(item)){
                        tile.entity.items.remove(item, 1);
                        entity.target.addAmmo(item);
                        break;
                    }
                }
            }

            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
        }

        if(entity.target != null && entity.cons.valid()){
            entity.lastx = entity.target.x;
            entity.lasty = entity.target.y;
            entity.strength = Mathf.lerpDelta(entity.strength, 1f, 0.08f * Timers.delta());
        }else{
            entity.strength = Mathf.lerpDelta(entity.strength, 0f, 0.08f * Timers.delta());
        }

        if(entity.timer.get(timerTarget, 20)){
            rect.setSize(supplyRadius * 2).setCenter(tile.drawx(), tile.drawy());

            entity.target = Units.getClosest(tile.getTeam(), tile.drawx(), tile.drawy(), supplyRadius, unit -> validTarget(entity, unit));
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public TileEntity getEntity(){
        return new ResupplyPointEntity();
    }

    boolean validTarget(ResupplyPointEntity entity, Unit unit){
        if(unit == null || unit.inventory.totalAmmo() >= unit.inventory.ammoCapacity()
                || unit.isDead()) return false;

        for(int i = 0; i < Item.all().size; i++){
            Item item = Item.getByID(i);
            if(entity.items.has(item) && unit.acceptsAmmo(item)){
                return true;
            }
        }
        return false;
    }

    public class ResupplyPointEntity extends TileEntity{
        public Unit target;
        public float strength, rotation = 90, lastx, lasty;
    }
}
