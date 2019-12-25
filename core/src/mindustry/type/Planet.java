package mindustry.type;

import arc.scene.ui.layout.*;
import mindustry.ctype.*;

//TODO add full icon for this planet
public class Planet extends UnlockableContent{

    public Planet(String name){
        super(name);
    }

    /** Planets cannot be viewed in the database dialog. */
    @Override
    public boolean isHidden(){
        return true;
    }

    @Override
    public void displayInfo(Table table){

    }

    @Override
    public ContentType getContentType(){
        return ContentType.planet;
    }
}
