package mindustry.world.meta;

public enum BlockGroup{
    none, walls(true), projectors(true), turrets(true), transportation(true), power, liquids(true), drills, units, logic(true);

    /** if true, any block in this category replaces any other block in this category. */
    public final boolean anyReplace;

    BlockGroup(boolean anyReplace){
        this.anyReplace = anyReplace;
    }

    BlockGroup(){
        this(false);
    }
}
