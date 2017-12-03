package io.anuke.mindustry.resource;

public class ItemStack{
	public Item item;
	public int amount;
	public float pos;
	
	public ItemStack(Item item, int amount){
		this.item = item;
		this.amount = amount;
	}
	
	public boolean equals(ItemStack other){
		return other != null && other.item == item && other.amount == amount;
	}
}
