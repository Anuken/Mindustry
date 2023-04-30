package mindustry.type;

import arc.struct.*;
import mindustry.world.*;

public class Category implements Comparable<Category>{
    public static final Seq<Category> all = new Seq<>();

    public static Category

    /** Offensive turrets. */
    turret = new Category("turret"),
    /** Blocks that produce raw resources, such as drills. */
    production = new Category("production"),
    /** Blocks that move items around. */
    distribution = new Category("distribution"),
    /** Blocks that move liquids around. */
    liquid = new Category("liquid"),
    /** Blocks that generate or transport power. */
    power = new Category("power"),
    /** Walls and other defensive structures. */
    defense = new Category("defense"),
    /** Blocks that craft things. */
    crafting = new Category("crafting"),
    /** Blocks that create units. */
    units = new Category("units"),
    /** Things for storage or passive effects. */
    effect = new Category("effect"),
    /** Blocks related to logic. */
    logic = new Category("logic");

    public int id;
    public String name;
    public Seq<Block> blocks = new Seq<>();

    public Category(String name){
        id = all.size;
        this.name = name;
        all.add(this);
    }

    public void add(Block b){
        int index = -1;
        for(int i = 0; i < blocks.size; i++){
            if(blocks.get(i).uiPosition > b.uiPosition){
                index = i;
                break;
            }
        }

        if(index == -1){
            blocks.add(b);
        }else{
            blocks.insert(index, b);
        }
    }

    public Category prev(){
        return all.get((id - 1 + all.size) % all.size);
    }

    public Category next(){
        return all.get((id + 1) % all.size);
    }

    @Override
    public int compareTo(Category o){
        return id - o.id;
    }
}
