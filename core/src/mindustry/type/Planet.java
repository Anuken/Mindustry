package mindustry.type;

import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.ctype.*;
import mindustry.graphics.*;
import mindustry.maps.planet.*;

public class Planet extends UnlockableContent{
    /** Mesh used for rendering. Created on load(). */
    public PlanetMesh mesh;
    /** Grid used for the sectors on the planet. */
    public @NonNull PlanetGrid grid;
    /** Generator that will make the planet. */
    public @NonNull PlanetGenerator generator;
    /** Detail in divisions. Must be between 1 and 10. 6 is a good number for this.*/
    public int detail = 3;
    /** Size in terms of divisions. This only controls the amount of sectors on the planet, not the visuals. */
    public int size = 3;

    public Planet(String name){
        super(name);
    }

    @Override
    public void load(){
        mesh = new PlanetMesh(detail, generator);
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
