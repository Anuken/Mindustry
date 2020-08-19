package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class Cultivator extends GenericCrafter{
    public Color plantColor = Color.valueOf("5541b1");
    public Color plantColorLight = Color.valueOf("7457ce");
    public Color bottomColor = Color.valueOf("474747");

    public @Load("@-middle") TextureRegion middleRegion;
    public @Load("@-top") TextureRegion topRegion;
    public Rand random = new Rand(0);
    public float recurrence = 6f;
    public Attribute attribute = Attribute.spores;

    public Cultivator(String name){
        super(name);
        craftEffect = Fx.none;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("multiplier", (CultivatorBuild entity) -> new Bar(() ->
        Core.bundle.formatFloat("bar.efficiency",
        ((entity.boost + 1f + attribute.env()) * entity.warmup) * 100f, 1),
        () -> Pal.ammo,
        () -> entity.warmup));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.affinities, attribute);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", (1 + sumAttribute(attribute, x, y)) * 100, 1), x, y, valid);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    public class CultivatorBuild extends GenericCrafterBuild{
        public float warmup;
        public float boost;

        @Override
        public void updateTile(){
            super.updateTile();

            warmup = Mathf.lerpDelta(warmup, consValid() ? 1f : 0f, 0.015f);
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            Draw.color(plantColor);
            Draw.alpha(warmup);
            Draw.rect(middleRegion, x, y);

            Draw.color(bottomColor, plantColorLight, warmup);

            random.setSeed(tile.pos());
            for(int i = 0; i < 12; i++){
                float offset = random.nextFloat() * 999999f;
                float x = random.range(4f), y = random.range(4f);
                float life = 1f - (((Time.time() + offset) / 50f) % recurrence);

                if(life > 0){
                    Lines.stroke(warmup * (life * 1f + 0.2f));
                    Lines.poly(x + x, y + y, 8, (1f - life) * 3f);
                }
            }

            Draw.color();
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityAdded();

            boost = sumAttribute(attribute, tile.x, tile.y);
        }

        @Override
        public float getProgressIncrease(float baseTime){
            return super.getProgressIncrease(baseTime) * (1f + boost + attribute.env());
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            warmup = read.f();
        }
    }
}
