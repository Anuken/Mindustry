package mindustry.world.blocks.sandbox;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

public class PowerSourceConfigurable extends PowerDistributor{

    public PowerSourceConfigurable(String name){
        super(name);
        outputsPower = true;
        consumesPower = false;
        envEnabled = Env.any;
        configurable = true;

        config(Float.class, (PowerSourceConfigurableBuild tile, Float power) -> {
            tile.powerProduction = (float) Math.pow(10,power) / 60f;
            tile.current = power;
        });
        configClear((PowerSourceConfigurableBuild tile) -> {
            tile.powerProduction = 0f;
            tile.current = 0f;
        });
    }

    public class PowerSourceConfigurableBuild extends Building{

        public float current = 3f;
        public float powerProduction = (float) Math.pow(10,current) / 60f;

        @Override
        public void buildConfiguration(Table table){
            Table temp = table.table(Tex.pane).get();
            //Styles.clearTransi
            temp.add(new Label(() -> "power: " + powerProduction * 60f));
            temp.row();
            temp.slider(0f, 6f, 0.1f, current, this::configure);
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
        }

        @Override
        public Float config(){
            return powerProduction;
        }

        @Override
        public float getPowerProduction(){
            return enabled ? powerProduction : 0f;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(current);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            current = read.f();
        }
    }
}
