package mindustry.type;

import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;

public class Category{
    public static Seq<Category> all = new Seq<>();

    public static Category
    /** Offensive turrets. */
    turret = new Category(null, "turret"),
    /** Blocks that produce raw resources, such as drills. */
    production = new Category(null, "production"),
    /** Blocks that move items around. */
    distribution = new Category(null, "distribution"),
    /** Blocks that move liquids around. */
    liquid = new Category(null, "liquid"),
    /** Blocks that generate or transport power. */
    power = new Category(null, "power"),
    /** Walls and other defensive structures. */
    defense = new Category(null, "defense"),
    /** Blocks that craft things. */
    crafting = new Category(null, "crafting"),
    /** Blocks that create units. */
    units = new Category(null, "units"),
    /** Things for storage or passive effects. */
    effect = new Category(null, "effect"),
    /** Blocks related to logic. */
    logic = new Category(null, "logic");

    public int id;
    public String name;

    /** for modded categories? **/
    public TextureRegionDrawable icon;

    /** @param icon category ui icon <br> **/
    public Category(@Nullable TextureRegionDrawable icon, String name) {
        this.icon = icon;
        this.name = name;

        id = all.size;
        all.add(this);
    }

    public int ordinal() {
        return id;
    }

    public Category prev(){
        return all.get((ordinal() - 1 + all.size) % all.size);
    }

    public Category next(){
        return all.get((ordinal() + 1) % all.size);
    }
}
