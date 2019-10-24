package io.anuke.mindustry.world.meta;

import io.anuke.mindustry.ctype.Content;

public class Producers{
    private Content output;

    public void set(Content content){
        this.output = content;
    }

    public Content get(){
        return output;
    }

    public boolean is(Content content){
        return content == output;
    }
}
