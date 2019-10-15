package io.anuke.mindustry.game;

import io.anuke.mindustry.*;

public abstract class MappableContent extends Content{
    public final String name;

    public MappableContent(String name){
        this.name = name;
        Vars.content.handleMappableContent(this);
    }

    @Override
    public String toString(){
        return name;
    }
}
