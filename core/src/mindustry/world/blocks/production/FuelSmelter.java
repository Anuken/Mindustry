package mindustry.world.blocks.production;

import arc.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

/** A smelter uses fuel to craft. Attribute tiles make it use less fuel. */
public class FuelSmelter extends GenericSmelter{
    public Attribute attribute = Attribute.heat;
    public Item fuelItem;
    public int fuelPerItem = 10, fuelPerCraft = 10;
    public int fuelCapacity = 30;
    /** 1 affinity = this amount removed from fuel use */
    public float fuelUseReduction = 1.5f;

    public FuelSmelter(String name){
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

    public class FuelSmelterBuild extends SmelterBuild{
        public int fuel;
        public float attrsum;

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
            if(item == fuelItem){
                fuel += fuelPerItem;
            }
            super.handleItem(source, item);
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