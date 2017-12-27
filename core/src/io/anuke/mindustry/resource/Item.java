package io.anuke.mindustry.resource;

import io.anuke.ucore.util.Bundles;

public enum Item{
	stone, iron, coal, steel, titanium, dirium, uranium;

	public String localized(){
		return Bundles.get("item."+name() + ".name");
	}

	@Override
	public String toString() {
		return localized();
	}
}
