package mindustry.type;

import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.graphics.*;

public class Planet extends UnlockableContent{
    /** Mesh used for rendering. Created on load(). */
    public PlanetMesh mesh;
    /** Grid used for the sectors on the planet. */
    public @NonNull PlanetGrid grid;

    public Planet(String name){
        super(name);
    }

    @Override
    public void load(){
        Time.mark();
        mesh = new PlanetMesh(6);
        Log.info("Time to generate planet mesh: {0}", Time.elapsed());
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
