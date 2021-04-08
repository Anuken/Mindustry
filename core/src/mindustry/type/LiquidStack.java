package mindustry.type;

import arc.math.*;
import arc.struct.*;
import mindustry.content.*;

public class LiquidStack implements Comparable<LiquidStack>{
    public Liquid liquid;
    public float amount;

    public LiquidStack(Liquid liquid, float amount){
        this.liquid = liquid;
        this.amount = amount;
    }

    /** serialization only*/
    protected LiquidStack(){
        //prevent nulls.
        liquid = Liquids.water;
    }

    public LiquidStack set(Liquid liquid, float amount){
        this.liquid = liquid;
        this.amount = amount;
        return this;
    }

    public LiquidStack copy(){
        return new LiquidStack(liquid, amount);
    }

    public boolean equals(LiquidStack other){
        return other != null && other.liquid == liquid && other.amount == amount;
    }

    public static LiquidStack[] mult(LiquidStack[] stacks, float amount){
        LiquidStack[] copy = new LiquidStack[stacks.length];
        for(int i = 0; i < copy.length; i++){
            copy[i] = new LiquidStack(stacks[i].liquid, Mathf.round(stacks[i].amount * amount));
        }
        return copy;
    }

    public static LiquidStack[] with(Object... items){
        LiquidStack[] stacks = new LiquidStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new LiquidStack((Liquid)items[i], ((Number)items[i + 1]).intValue());
        }
        return stacks;
    }

    public static Seq<LiquidStack> list(Object... items){
        Seq<LiquidStack> stacks = new Seq<>(items.length / 2);
        for(int i = 0; i < items.length; i += 2){
            stacks.add(new LiquidStack((Liquid)items[i], ((Number)items[i + 1]).intValue()));
        }
        return stacks;
    }

    @Override
    public int compareTo(LiquidStack liquidStack){
        return liquid.compareTo(liquidStack.liquid);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof LiquidStack stack)) return false;
        return amount == stack.amount && liquid == stack.liquid;
    }

    @Override
    public String toString(){
        return "LiquidStack{" +
        "liquid=" + liquid +
        ", amount=" + amount +
        '}';
    }
}
