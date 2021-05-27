package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

/** A smelter uses fuel to craft. Attribute tiles make it use less fuel. */
public class FuelCrafter extends GenericCrafter{
    public Attribute attribute = Attribute.heat;
    public Item fuelItem;
    public int fuelPerItem = 10, fuelPerCraft = 10;
    public int fuelCapacity = 30;
    /** 1 affinity = this amount removed from fuel use */
    public float fuelUseReduction = 1.5f;

    public boolean isSmelter = true;
    public Color flameColor = Color.valueOf("ffc999");
    public @Load("@-top") TextureRegion topRegion;

    public FuelCrafter(String name){
        super(name);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        int amount = Math.max(fuelPerCraft - Mathf.round(sumAttribute(attribute, x, y) * fuelUseReduction), 0);
        drawPlaceText(Core.bundle.format("bar.fuelUse",
            amount > 0 ? amount : Core.bundle.get("bar.fuel.unneeded")), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("fuel", (FuelSmelterBuild entity) -> new Bar(
            () -> Core.bundle.format("bar.fuel", entity.fuelNeeded() > 0 ? entity.fuel : Core.bundle.get("bar.fuel.unneeded")),
            () -> Pal.lightOrange,
            () -> entity.fuelNeeded() > 0 ? (float)entity.fuel / (float)fuelCapacity : 1f
        ));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.fuel, new FuelListValue(this));
    }

    public class FuelSmelterBuild extends GenericCrafterBuild{
        public int fuel;
        public float attrsum;

        @Override
        public void draw(){
            super.draw();

            //draw glowing center
            if(isSmelter && warmup > 0f && flameColor.a > 0.001f){
                float g = 0.3f;
                float r = 0.06f;
                float cr = Mathf.random(0.1f);

                Draw.z(Layer.block + 0.01f);

                Draw.alpha(((1f - g) + Mathf.absin(Time.time, 8f, g) + Mathf.random(r) - r) * warmup);

                Draw.tint(flameColor);
                Fill.circle(x, y, 3f + Mathf.absin(Time.time, 5f, 2f) + cr);
                Draw.color(1f, 1f, 1f, warmup);
                Draw.rect(topRegion, x, y);
                Fill.circle(x, y, 1.9f + Mathf.absin(Time.time, 5f, 1f) + cr);

                Draw.color();
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, (60f + Mathf.absin(10f, 5f)) * warmup * size, flameColor, 0.65f);
        }

        @Override
        public boolean shouldConsume(){
            return fuel >= fuelNeeded() && super.shouldConsume();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            attrsum = sumAttribute(attribute, tile.x, tile.y);
        }

        @Override
        public void consume(){
            fuel -= fuelNeeded();
            super.consume();
        }

        public int fuelNeeded(){
            return Math.max(fuelPerCraft - Mathf.round(attrsum * fuelUseReduction), 0);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return super.acceptItem(source, item) || item == fuelItem && fuel + fuelPerItem <= fuelCapacity && fuelNeeded() > 0f;
        }

        @Override
        public void handleItem(Building source, Item item){
            if(item == fuelItem && fuel + fuelPerItem <= fuelCapacity && fuelNeeded() > 0f){
                fuel += fuelPerItem;
                return;
            }

            if(block.consumes.itemFilters.get(item.id)){
                super.handleItem(source, item);
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(fuel);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);

            fuel = read.i();
        }
    }
}