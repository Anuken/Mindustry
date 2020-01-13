package mindustry.type;

import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.ctype.*;
import mindustry.graphics.*;

public class Planet extends UnlockableContent{
    /** Mesh used for rendering. */
    public @NonNull PlanetMesh mesh;
    /** Grid used for the sectors on the planet. */
    public @NonNull PlanetGrid grid;

    public Planet(String name, PlanetMesh mesh){
        super(name);
        this.mesh = mesh;
    }

    //mods
    Planet(String name){
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
