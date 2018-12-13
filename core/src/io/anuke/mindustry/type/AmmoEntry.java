package io.anuke.mindustry.type;

/**Used to store ammo amounts in turrets.*/
public class AmmoEntry{
    public AmmoType type;
    public int amount;

    public AmmoEntry(AmmoType type, int amount){
        this.type = type;
        this.amount = amount;
    }
}
