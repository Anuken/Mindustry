package io.anuke.mindustry.entities;

import io.anuke.mindustry.resource.*;

public class UnitInventory {
    public final AmmoEntry ammo = new AmmoEntry(AmmoType.getByID(0), 0);
    public final ItemStack item = new ItemStack(Item.getByID(0), 0);
    public final LiquidStack liquid = new LiquidStack(Liquid.getByID(0), 0);
    public float power = 0f;
}
