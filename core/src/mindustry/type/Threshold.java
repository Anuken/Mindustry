package mindustry.type;

import arc.struct.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.ctype.*;
import mindustry.ctype.ContentType;
import mindustry.game.Objectives;
import mindustry.ui.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.content;

public class Threshold extends UnlockableContent {
    // Threshold weighs for vault vs launcher
    public float vaultFrac = 0f;
    public float launcherFrac = 0f;

    public Threshold(String name){
        super(name);
    }

    @Override
    public boolean alwaysUnlocked(){
        return true;
    }

    @Override
    public void displayInfo(Table table){
//        ContentDisplay.displayThreshold(table, this);
    }

    @Override
    public String toString(){
        return localizedName;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.threshold;
    }

}
