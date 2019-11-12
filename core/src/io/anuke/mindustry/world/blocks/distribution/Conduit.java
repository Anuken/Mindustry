package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.modules.*;

public class Conduit extends LiquidBlock implements Autotiler{
    protected final int timerFlow = timers++;

    protected TextureRegion[] topRegions = new TextureRegion[7];
    protected TextureRegion[] botRegions = new TextureRegion[7];

    public Conduit(String name){
        super(name);
        rotate = true;
        solid = false;
        floating = true;
        conveyorPlacement = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find("conduit-liquid");
        for(int i = 0; i < topRegions.length; i++){
            topRegions[i] = Core.atlas.find(name + "-top-" + i);
            botRegions[i] = Core.atlas.find("conduit-bottom-" + i);
        }
    }

    @Override
    public void onProximityUpdate(Tile tile){
        super.onProximityUpdate(tile);

        ConduitEntity entity = tile.entity();
        int[] bits = buildBlending(tile, tile.rotation(), null, true);
        entity.blendbits = bits[0];
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        Draw.colorl(0.34f);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], req.drawx(), req.drawy(),
            botRegions[bits[0]].getWidth() * Draw.scl * req.animScale, botRegions[bits[0]].getHeight() * Draw.scl * req.animScale,
            req.rotation * 90);
        Draw.color();


        Draw.rect(topRegions[bits[0]], req.drawx(), req.drawy(), topRegions[bits[0]].getWidth() * Draw.scl * req.animScale, topRegions[bits[0]].getHeight() * Draw.scl * req.animScale, req.rotation * 90);
    }

    @Override
    public Block getReplacement(BuildRequest req, Array<BuildRequest> requests){
        Boolf<Point2> cont = p -> requests.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof Conduit || req.block instanceof LiquidJunction));
        return cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof Conduit &&
            Mathf.mod(req.tile().rotation() - req.rotation, 2) == 1 ? Blocks.liquidJunction : this;
    }

    @Override
    public void transformCase(int num, int[] bits){
        bits[0] = num == 0 ? 3 : num == 1 ? 6 : num == 2 ? 2 : num == 3 ? 4 : num == 4 ? 5 : num == 5 ? 1 : 0;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.hasLiquids && otherblock.outputsLiquid && lookingAt(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public void draw(Tile tile){
        ConduitEntity entity = tile.entity();
        LiquidModule mod = tile.entity.liquids;
        int rotation = tile.rotation() * 90;

        Draw.colorl(0.34f);
        Draw.rect(botRegions[entity.blendbits], tile.drawx(), tile.drawy(), rotation);

        Draw.color(mod.current().color);
        Draw.alpha(entity.smoothLiquid);
        Draw.rect(botRegions[entity.blendbits], tile.drawx(), tile.drawy(), rotation);
        Draw.color();

        Draw.rect(topRegions[entity.blendbits], tile.drawx(), tile.drawy(), rotation);
    }

    @Override
    public void update(Tile tile){
        ConduitEntity entity = tile.entity();
        entity.smoothLiquid = Mathf.lerpDelta(entity.smoothLiquid, entity.liquids.total() / liquidCapacity, 0.05f);

        if(tile.entity.liquids.total() > 0.001f && tile.entity.timer.get(timerFlow, 1)){
            tryMoveLiquid(tile, tile.getNearby(tile.rotation()), true, tile.entity.liquids.current());
            entity.noSleep();
        }else{
            entity.sleep();
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("conduit-bottom"), Core.atlas.find(name + "-top-0")};
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        tile.entity.noSleep();
        return tile.entity.liquids.get(liquid) + amount < liquidCapacity && (tile.entity.liquids.current() == liquid || tile.entity.liquids.get(tile.entity.liquids.current()) < 0.2f)
            && ((source.absoluteRelativeTo(tile.x, tile.y) + 2) % 4 != tile.rotation());
    }

    @Override
    public TileEntity newEntity(){
        return new ConduitEntity();
    }

    public static class ConduitEntity extends TileEntity{
        public float smoothLiquid;

        int blendbits;
    }
}
