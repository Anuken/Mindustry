package io.anuke.mindustry.resource;

public class LiquidStack {
    public Liquid liquid;
    public float amount;

    public LiquidStack(Liquid liquid, float amount){
        this.liquid = liquid;
        this.amount = amount;
    }

    public boolean equals(LiquidStack other){
        return other != null && other.liquid == liquid && other.amount == amount;
    }
}
