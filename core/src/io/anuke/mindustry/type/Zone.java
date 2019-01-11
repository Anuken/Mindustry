package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.maps.generators.Generator;

public class Zone extends UnlockableContent{
    public final String name;
    public ItemStack[] deployCost = {};
    public Generator generator;

    public Zone(String name){
        this.name = name;
    }

    @Override
    public boolean isHidden(){
        return true;
    }

    //neither of these are implemented, as zones are not displayed in a normal fashion... yet
    @Override
    public void displayInfo(Table table){}

    @Override
    public TextureRegion getContentIcon(){ return null; }

    @Override
    public String getContentName(){
        return name;
    }

    @Override
    public String localizedName(){
        return Core.bundle.get("zone."+name+".name");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.zone;
    }
}
