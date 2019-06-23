package io.anuke.mindustry.game;

public abstract class MappableContent extends Content{
    public final String name;

    public MappableContent(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }
}
