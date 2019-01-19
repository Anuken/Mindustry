package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.RandomXS128;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Cultivator extends Drill{
    protected Color plantColor = Color.valueOf("648b55");
    protected Color plantColorLight = Color.valueOf("73a75f");
    protected Color bottomColor = Color.valueOf("474747");
    protected TextureRegion middleRegion, topRegion;

    protected Item result;

    protected RandomXS128 random = new RandomXS128(0);
    protected float recurrence = 6f;

    public Cultivator(String name){
        super(name);
        drillEffect = Fx.none;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.drillTier);
        stats.add(BlockStat.drillTier, table -> {
            table.addImage("grass1").size(8 * 3).padBottom(3).padTop(3);
            table.add(Blocks.grass.formalName).padLeft(3);
        });
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

    @Override
    public boolean isValid(Tile tile){
        return tile != null && tile.floor() == Blocks.grass;
    }

    @Override
    public Item getDrop(Tile tile){
        return Items.biomatter;
    }

    public static class CultivatorEntity extends DrillEntity{
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
