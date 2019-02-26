package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.RandomXS128;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.Tile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Cultivator extends GenericCrafter{
    protected static final Color plantColor = Color.valueOf("648b55");
    protected static final Color plantColorLight = Color.valueOf("73a75f");
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

    public static class CultivatorEntity extends GenericCrafterEntity{
        public float warmup;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            warmup = stream.readFloat();
        }
    }
}
