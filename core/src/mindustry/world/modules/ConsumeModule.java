package mindustry.world.modules;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

/** @deprecated why is it a module? literally two booleans, why did I think it was a good idea? yay, more pointers???
 * the braindead java "make everything a separate class" mentality
 * */
@Deprecated
public class ConsumeModule extends BlockModule{
    private final Building build;

    public ConsumeModule(Building build){
        this.build = build;
    }

    public BlockStatus status(){
        return build.status();
    }

    public void update(){
        build.updateConsumption();
    }

    public void trigger(){
        build.consume();
    }

    public boolean valid(){
        return build.consValid();
    }

    public boolean canConsume(){
        return build.canConsume();
    }

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
