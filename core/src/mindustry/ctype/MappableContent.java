package mindustry.ctype;

import mindustry.*;

public abstract class MappableContent extends Content{
    public final String name, prefix;
    public boolean loadPrefix;

    public MappableContent(String name){
        prefix = Vars.content.getModPrefix();
        this.name = prefix + name;
        Vars.content.handleMappableContent(this);
    }

    @Override
    public String toString(){
        return name;
    }
}
