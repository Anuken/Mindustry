package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.RandomXS128;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.Attribute;

import java.io.*;

public class Cultivator extends GenericCrafter{
    protected static final Color plantColor = Color.valueOf("5541b1");
    protected static final Color plantColorLight = Color.valueOf("7457ce");
    protected static final Color bottomColor = Color.valueOf("474747");

    protected TextureRegion middleRegion, topRegion;
    protected RandomXS128 random = new RandomXS128(0);
    protected float recurrence = 6f;

    public Cultivator(String name){
        super(name);
        craftEffect = Fx.none;
    }

    @Override
    public void load(){
        super.load();

        middleRegion = Core.atlas.find(name + "-middle");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        CultivatorEntity entity = tile.entity();
        entity.warmup = Mathf.lerpDelta(entity.warmup, entity.cons.valid() ? 1f : 0f, 0.015f);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("multiplier", entity -> new Bar(() ->
        Core.bundle.formatFloat("bar.efficiency",
        ((((CultivatorEntity)entity).boost + 1f) * ((CultivatorEntity)entity).warmup) * 100f, 1),
        () -> Pal.ammo,
        () -> ((CultivatorEntity)entity).warmup));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", (1 + sumAttribute(Attribute.spores, x, y)) * 100, 1), x, y, valid);
    }

    @Override
    public void draw(Tile tile){
        CultivatorEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());

        Draw.color(plantColor);
        Draw.alpha(entity.warmup);
        Draw.rect(middleRegion, tile.drawx(), tile.drawy());

        Draw.color(bottomColor, plantColorLight, entity.warmup);

        random.setSeed(tile.pos());
        for(int i = 0; i < 12; i++){
            float offset = random.nextFloat() * 999999f;
            float x = random.range(4f), y = random.range(4f);
            float life = 1f - (((Time.time() + offset) / 50f) % recurrence);

            if(life > 0){
                Lines.stroke(entity.warmup * (life * 1f + 0.2f));
                Lines.poly(tile.drawx() + x, tile.drawy() + y, 8, (1f - life) * 3f);
            }
        }

        Draw.color();
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-top"),};
    }

    @Override
    public TileEntity newEntity(){
        return new CultivatorEntity();
    }

    @Override
    public void onProximityAdded(Tile tile){
        super.onProximityAdded(tile);

        CultivatorEntity entity = tile.entity();
        entity.boost = sumAttribute(Attribute.spores, tile.x, tile.y);
    }

    @Override
    protected float getProgressIncrease(TileEntity entity, float baseTime){
        CultivatorEntity c = (CultivatorEntity)entity;
        return super.getProgressIncrease(entity, baseTime) * (1f + c.boost);
    }

    public static class CultivatorEntity extends GenericCrafterEntity{
        public float warmup;
        public float boost;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            warmup = stream.readFloat();
        }
    }
}
