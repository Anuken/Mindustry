package mindustry.world.blocks.sandbox;

import arc.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

public class PowerSource extends PowerNode{
    /** Kept for backwards compatibility. Defines the default power proudction amount. */
    public float powerProduction = 10000f;

    public PowerSource(String name){
        super(name);
        maxNodes = 100;
        outputsPower = true;
        consumesPower = false;
        //TODO maybe don't?
        envEnabled = Env.any;

        config(Object[].class, (PowerSourceBuild build, Object[] configs) -> {
            for(Object obj : configs){
                build.configure(obj);
            }
        });

        config(Float.class, (PowerSourceBuild build, Float f) -> build.configPower = f);
    }

    public class PowerSourceBuild extends PowerNodeBuild{
        public float configPower = powerProduction;

        @Override
        public float getPowerProduction(){
            return enabled ? configPower / 60f : 0f;
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black5, t -> {
                t.marginLeft(6f).marginRight(6f).right();
                t.field(String.valueOf(configPower), text -> {
                    float newPower = configPower;
                    if(Strings.canParsePositiveFloat(text)){
                        newPower = Strings.parseFloat(text);
                    }
                    configure(newPower);
                }).width(120).get().setFilter(TextFieldFilter.floatsOnly);
                t.add(Core.bundle.get("unit.powersecond")).left();
            });
        }

        @Override
        public Object[] config(){
            return new Object[]{super.config(), configPower};
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(configPower);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                configPower = read.f();
            }
        }

        @Override
        public byte version(){
            return 1;
        }
    }

}
