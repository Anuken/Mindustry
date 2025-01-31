package mindustry.world.blocks.campaign;

import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;

public class LandingPad extends Block{

    public LandingPad(String name){
        super(name);

        hasItems = true;
        solid = true;
        update = true;
        configurable = true;
        acceptsItems = false;

        config(Item.class, (LandingPadBuild build, Item item) -> build.config = item);
        configClear((LandingPadBuild build) -> build.config = null);
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class LandingPadBuild extends Building{
        public @Nullable Item config;

        @Override
        public void updateTile(){
            if(items.total() > 0){
                dumpAccumulate(config == null || items.get(config) != items.total() ? null : config);
            }
        }

        @Override
        public boolean canDump(Building to, Item item){
            //hack: canDump is only ever called right before item offload, so count the item as "produced" before that.
            //TODO: is this necessary?
            produced(item);
            return true;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public @Nullable Object config(){
            return config;
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            config = TypeIO.readItem(read);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            TypeIO.writeItem(write, config);
        }
    }
}
