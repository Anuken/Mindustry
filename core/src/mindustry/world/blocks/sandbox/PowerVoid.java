package mindustry.world.blocks.sandbox;

import arc.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

public class PowerVoid extends PowerBlock{
    public PowerVoid(String name){
        super(name);
        consumePowerDynamic(b -> (float)b.config() / 60f);
        envEnabled = Env.any;
        enableDrawStatus = false;
        configurable = true;
        saveConfig = true;
        canOverdrive = false;

        config(Float.class, (PowerVoidBuild build, Float f) -> build.powerDrain = f);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.powerUse);
    }

    public class PowerVoidBuild extends Building{
        public float powerDrain = 1000000f;

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.config) return powerDrain;

            return super.senseObject(sensor);
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black5, t -> {
                t.marginLeft(6f).marginRight(6f).right();
                t.field(String.valueOf(powerDrain), text -> {
                    configure(Strings.parseFloat(text));
                }).width(120).valid(Strings::canParsePositiveFloat).get().setFilter(TextFieldFilter.floatsOnly);
                t.add(Core.bundle.get("unit.powersecond")).left();
            });
        }

        @Override
        public Float config(){
            return powerDrain;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(powerDrain);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                powerDrain = read.f();
            }
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
