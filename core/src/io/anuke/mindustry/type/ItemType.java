package io.anuke.mindustry.type;

public enum ItemType{
    /**
     * Not used for anything besides crafting inside blocks.
     */
    resource,
    /**
     * Can be used for constructing blocks. Only materials are accepted into the core.
     */
    material,
    /**
     * Only used as ammo for turrets.
     */
    ammo
}
