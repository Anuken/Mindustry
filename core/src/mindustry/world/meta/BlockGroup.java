package mindustry.world.meta;

public enum BlockGroup{
    none, walls(true), projectors(true), turrets, transportation(true), power, liquids(true), drills, storage, units, logic(true);

    /** if true, any block in this category replaces any other block in this category. */
    public final boolean anyReplace;

    BlockGroup(boolean anyReplace){
        this.anyReplace = anyReplace;
    }

    BlockGroup(){
        this(false);
    }
}
