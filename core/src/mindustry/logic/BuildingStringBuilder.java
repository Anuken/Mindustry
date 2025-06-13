package mindustry.logic;

import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.blocks.logic.*;

public class BuildingStringBuilder implements CharSequence{
    public int source;

    public BuildingStringBuilder(int source){
        this.source = source;
    }

    public @Nullable StringBuilder obtainBuilder(){
        @Nullable Building src = Vars.world.build(source);
        if(src == null) return null;
        if(src instanceof MessageBlock.MessageBuild msg){
            return msg.message;
        }else if(src instanceof LogicBlock.LogicBuild proc){
            return proc.executor.textBuffer;
        }
        return null;
    }

    @Override
    public int length(){
        @Nullable StringBuilder build = obtainBuilder();
        return build == null ? 0 : build.length();
    }

    @Override
    public char charAt(int i){
        @Nullable StringBuilder build = obtainBuilder();
        return build == null ? 0 : build.charAt(i);
    }

    @Override
    public CharSequence subSequence(int start, int end){
        @Nullable StringBuilder build = obtainBuilder();
        return build == null ? "" : build.subSequence(start, end);
    }

    @Override
    public String toString(){
        @Nullable StringBuilder build = obtainBuilder();
        return build == null ? "" : build.toString();
    }
}
