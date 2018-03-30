package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.SeedRandom;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Cultivator extends Drill {
    protected Color plantColor = Color.valueOf("648b55");
    protected Color plantColorLight = Color.valueOf("73a75f");
    protected Color bottomColor = Color.valueOf("474747");

    protected Item result;

    protected SeedRandom random = new SeedRandom(0);
    protected float recurrence = 6f;

    public Cultivator(String name) {
        super(name);
        drillEffect = Fx.none;
    }

    @Override
    public void update(Tile tile) {
        super.update(tile);

        CultivatorEntity entity = tile.entity();
        entity.warmup = Mathf.lerpDelta(entity.warmup,
                tile.entity.liquid.amount > liquidUse ? 1f : 0f, 0.015f);
    }

    @Override
    public void draw(Tile tile) {
        CultivatorEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());

        Draw.color(plantColor);
        Draw.alpha(entity.warmup);
        Draw.rect(name + "-middle", tile.drawx(), tile.drawy());

        Draw.color(bottomColor, plantColorLight, entity.warmup);

        random.setSeed(tile.packedPosition());
        for(int i = 0; i < 12; i ++){
            float offset = random.nextFloat() * 999999f;
            float x = random.range(4f), y = random.range(4f);
            float life = 1f - (((Timers.time() + offset) / 50f) % recurrence);

            if(life > 0){
                Lines.stroke(entity.warmup * (life*1f + 0.2f));
                Lines.poly(tile.drawx() + x, tile.drawy() + y, 8, (1f-life) * 3f);
            }
        }

        Draw.color();
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-top"), };
    }

    @Override
    public TileEntity getEntity() {
        return new CultivatorEntity();
    }

    @Override
    public boolean isValid(Tile tile){
        return tile.block().drops != null && tile.block().drops.item == result;
    }

    public static class CultivatorEntity extends DrillEntity{
        public float warmup;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            warmup = stream.readFloat();
        }
    }
}
