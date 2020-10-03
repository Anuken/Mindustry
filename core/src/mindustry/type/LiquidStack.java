package mindustry.type;

import mindustry.content.*;

public class LiquidStack{
    public Liquid liquid;
    public float amount;

    public LiquidStack(Liquid liquid, float amount){
        this.liquid = liquid;
        this.amount = amount;
    }

    /** serialization only*/
    protected LiquidStack(){
        liquid = Liquids.water;
    }

    @Override
    public String toString(){
        return "LiquidStack{" +
        "liquid=" + liquid +
        ", amount=" + amount +
        '}';
    }
}
