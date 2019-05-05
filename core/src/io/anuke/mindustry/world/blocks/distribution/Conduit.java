package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.modules.LiquidModule;

public class Conduit extends LiquidBlock{
    protected final int timerFlow = timers++;

    protected TextureRegion[] topRegions = new TextureRegion[7];
    protected TextureRegion[] botRegions = new TextureRegion[7];

    public Conduit(String name){
        super(name);
        rotate = true;
        solid = false;
        floating = true;
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
        entity.blendbits = 0;
        entity.blendshadowrot = -1;

        if(blends(tile, 2) && blends(tile, 1) && blends(tile, 3)){
            entity.blendbits = 3;
        }else if(blends(tile, 1) && blends(tile, 3)){
            entity.blendbits = 6;
        }else if(blends(tile, 1) && blends(tile, 2)){
            entity.blendbits = 2;
        }else if(blends(tile, 3) && blends(tile, 2)){
            entity.blendbits = 4;
        }else if(blends(tile, 1)){
            entity.blendbits = 5;
            entity.blendshadowrot = 0;
        }else if(blends(tile, 3)){
            entity.blendbits = 1;
            entity.blendshadowrot = 1;
        }
    }

    private boolean blends(Tile tile, int direction){
        Tile other = tile.getNearby(Mathf.mod(tile.rotation() - direction, 4));
        if(other != null) other = other.link();

        return other != null && other.block().hasLiquids && other.block().outputsLiquid && ((tile.getNearby(tile.rotation()) == other) || (!other.block().rotate || other.getNearby(other.rotation()) == tile));
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
        return tile.entity.liquids.get(liquid) + amount < liquidCapacity && (tile.entity.liquids.current() == liquid || tile.entity.liquids.get(tile.entity.liquids.current()) < 0.2f) && ((2 + source.relativeTo(tile.x, tile.y)) % 4 != tile.rotation());
    }

    @Override
    public TileEntity newEntity(){
        return new ConduitEntity();
    }

    public static class ConduitEntity extends TileEntity{
        public float smoothLiquid;

        byte blendbits;
        int blendshadowrot;
    }
}
