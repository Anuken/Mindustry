package io.anuke.mindustry.resource;

public class ItemStack{
	public Item item;
	public int amount;
	public float pos;
	
	public ItemStack(Item item, int amount){
		this.item = item;
		this.amount = amount;
	}
}
