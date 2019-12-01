package io.anuke.mindustry.world.meta;

public enum Restriction{
    /** Blocks configuring this block outside the editor */
    unconfigurable,
    /** Blocks deconstructing this block outside the editor */
    unremovable,
    /** Blocks will not die if their health reaches zero */
    unkillable,
}
