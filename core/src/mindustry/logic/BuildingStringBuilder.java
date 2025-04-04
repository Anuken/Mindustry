package mindustry.logic;

import arc.util.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MessageBlock;

public class BuildingStringBuilder implements CharSequence{
    public Point2 source;

    public BuildingStringBuilder(Point2 source){
        this.source = source;
    }

    public @Nullable StringBuilder obtainBuilder(){
        @Nullable Building src = Vars.world.build(source.x, source.y);
        if(src == null)return(null);
        if(src instanceof MessageBlock.MessageBuild msg){
            return(msg.message);
        }else if(src instanceof LogicBlock.LogicBuild proc){
            return(proc.executor.textBuffer);
        }
        return(null);
    }

    @Override
    public int length(){
        @Nullable StringBuilder build = obtainBuilder();
        return(build == null ? 0 : build.length());
    }

    @Override
    public char charAt(int i){
        @Nullable StringBuilder build = obtainBuilder();
        return(build == null ? 0 : build.charAt(i));
    }

    @Override
    public CharSequence subSequence(int start, int end){
        @Nullable StringBuilder build = obtainBuilder();
        return(build == null ? "" : build.subSequence(start, end));
    }

    @Override
    public String toString(){
        @Nullable StringBuilder build = obtainBuilder();
        return(build == null ? "" : build.toString());
    }
}
