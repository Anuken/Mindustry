package io.anuke.mindustry.resource;

import io.anuke.mindustry.world.Block;

public class Recipe {
    public Block result;
    public ItemStack[] requirements;
    public Section section;
    public boolean desktopOnly = false;

    public Recipe(Section section, Block result, ItemStack... requirements){
        this.result = result;
        this.requirements = requirements;
        this.section = section;
    }

    public Recipe setDesktop(){
        desktopOnly = true;
        return this;
    }
}
