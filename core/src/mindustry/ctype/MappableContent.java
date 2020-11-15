package mindustry.ctype;

import mindustry.*;

public abstract class MappableContent extends Content{
    public final String name, prefix;

    public MappableContent(String name){
        this.name = Vars.content.transformName(name, prefix);
        Vars.content.handleMappableContent(this);
    }

    @Override
    public String toString(){
        return name;
    }
}
