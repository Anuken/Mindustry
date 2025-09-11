package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class ColoredWall extends StaticWall{
    /** If the alpha value of the color is set to this value, different colors are ignored and no border is drawn. */
    public static final int flagIgnoreDifferentColor = 1;
    /** If the alpha value of the color is set to this value, the wall will have darkness applied, as other walls do. */
    public static final int flagApplyDarkness = 2;

    public Color defaultColor = Color.white;
    protected int defaultColorRgba;

    public ColoredWall(String name){
        super(name);
        saveData = true;
        editorConfigurable = true;
        saveConfig = true;
    }

    @Override
    public void init(){
        super.init();
        lastConfig = defaultColorRgba = defaultColor.rgba();
    }

    @Override
    public Object getConfig(Tile tile){
        return tile.extraData;
    }

    @Override
    public void buildEditorConfig(Table table){
        ColoredFloor.showColorEdit(table, this);
    }

    @Override
    public void drawBase(Tile tile){
        //make sure to mask out the alpha channel - it's generally undesirable, and leads to invisible blocks when thtoe data is not initialized
        Draw.color(tile.extraData | 0xff);
        super.drawBase(tile);
        Draw.color();
    }

    @Override
    public void blockChanged(Tile tile){
        //reset to white on first placement
        if(tile.extraData == 0){
            tile.extraData = defaultColorRgba;
        }
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        //config is assumed to be an integer RGBA color
        if(config instanceof Integer i){
            tile.extraData = i;
        }
    }

    @Override
    public void editorPicked(Tile tile){
        lastConfig = tile.extraData;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        if(plan.config instanceof Integer i){
            Draw.tint(Tmp.c1.set(i | 0xff));
        }
        drawDefaultPlanRegion(plan, list);
    }

    @Override
    public boolean checkAutotileSame(Tile tile, @Nullable Tile other){
        return other != null && other.block() == this && ((tile.extraData == flagIgnoreDifferentColor) || tile.extraData == other.extraData);
    }

    @Override
    public boolean isDarkened(Tile tile){
        return (tile.extraData == flagApplyDarkness);
    }

    @Override
    public int minimapColor(Tile tile){
        return tile.extraData | 0xff;
    }
}
