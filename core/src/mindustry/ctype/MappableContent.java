package mindustry.ctype;

import mindustry.*;

public abstract class MappableContent extends Content{
    public final String name;

    public MappableContent(String name){
        this.name = Vars.content.transformName(name);
        Vars.content.handleMappableContent(this);
    }

    @Override
    public String toString(){
        return name;
    }
}
