package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

public class GenericCrafter extends Block{
    public @Nullable ItemStack outputItem;
    public @Nullable LiquidStack outputLiquid;

    public float craftTime = 80;
    public Effect craftEffect = Fx.none;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.04f;

    public DrawBlock drawer = new DrawBlock();

    public GenericCrafter(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        ambientSound = Sounds.machine;
        sync = true;
        ambientSoundVolume = 0.03f;
        flags = EnumSet.of(BlockFlag.factory);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);

        if(outputItem != null){
            stats.add(Stat.output, outputItem);
        }

        if(outputLiquid != null){
            stats.add(Stat.output, outputLiquid.liquid, outputLiquid.amount * (60f / craftTime), true);
        }
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public void init(){
        outputsLiquid = outputLiquid != null;
        super.init();
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.icons(this);
    }

    @Override
    public boolean outputsItems(){
        return outputItem != null;
    }

    public class GenericCrafterBuild extends Building{
        public float progress;
        public float totalProgress;
        public float warmup;

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public boolean shouldConsume(){
            if(outputItem != null && items.get(outputItem.item) >= itemCapacity){
                return false;
            }
            return (outputLiquid == null || !(liquids.get(outputLiquid.liquid) >= liquidCapacity - 0.001f)) && enabled;
        }

        @Override
        public void updateTile(){
            if(consValid()){

                progress += getProgressIncrease(craftTime);
                totalProgress += delta();
                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);

                if(Mathf.chanceDelta(updateEffectChance)){
                    updateEffect.at(getX() + Mathf.range(size * 4f), getY() + Mathf.range(size * 4));
                }
            }else{
                warmup = Mathf.lerp(warmup, 0f, 0.02f);
            }

            if(progress >= 1f){
                consume();

                if(outputItem != null){
                    for(int i = 0; i < outputItem.amount; i++){
                        offload(outputItem.item);
                    }
                }

                if(outputLiquid != null){
                    handleLiquid(this, outputLiquid.liquid, outputLiquid.amount);
                }

                craftEffect.at(x, y);
                progress %= 1f;
            }

            if(outputItem != null && timer(timerDump, dumpTime)){
                dump(outputItem.item);
            }

            if(outputLiquid != null){
                dumpLiquid(outputLiquid.liquid);
            }
        }

        @Override
        public int getMaximumAccepted(Item item){
            return itemCapacity;
        }

        @Override
        public boolean shouldAmbientSound(){
            return cons.valid();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            warmup = read.f();
        }
    }
}
