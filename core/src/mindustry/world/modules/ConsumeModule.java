package mindustry.world.modules;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

/**
 * why is it a module? literally two booleans, why did I think it was a good idea? yay, more pointers???
 * the braindead java "make everything a separate class" mentality
 * */
public class ConsumeModule extends BlockModule{
    private final Building build;

    public ConsumeModule(Building build){
        this.build = build;
    }

    /** @deprecated use build.status() */
    @Deprecated
    public BlockStatus status(){
        return build.status();
    }

    /** @deprecated use build.updateConsumption() */
    @Deprecated
    public void update(){
        build.updateConsumption();
    }

    /** @deprecated use build.consume() */
    @Deprecated
    public void trigger(){
        build.consume();
    }

    /** @deprecated use build.consValid() */
    @Deprecated
    public boolean valid(){
        return build.consValid();
    }

    /** @deprecated use build.canConsume() */
    @Deprecated
    public boolean canConsume(){
        return build.canConsume();
    }

    /** @deprecated use build.consOptionalValid() */
    @Deprecated
    public boolean optionalValid(){
        return build.consOptionalValid();
    }

    //hahahahahahahahahaha
    @Override
    public void write(Writes write){
    }

    @Override
    public void read(Reads read){
    }
}
