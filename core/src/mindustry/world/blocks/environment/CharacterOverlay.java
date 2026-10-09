package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class CharacterOverlay extends OverlayFloor{
    /** This is a special reduced character set that fits in 6 bits! It is not ASCII! */
    public static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\"!?.,;:()[]{}<>|/@\\^-%+=#_&~";

    public @Load(value = "character-overlay#", length = 64) TextureRegion[] letterRegions;
    public Color color = Color.white;

    public CharacterOverlay(String name){
        super(name);
        saveData = true;
        variants = 0;
        rotate = true;
        drawArrow = false;
        saveConfig = true;
        editorConfigurable = true;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.color(color);
        int letterChar = CharOverlayData.character(tile.overlayData);
        Draw.rect(letterRegions[letterChar], tile.worldx(), tile.worldy(), CharOverlayData.rotation(tile.overlayData) * 90f);
        Draw.color();
    }

    @Override
    public Object getConfig(Tile tile){
        return (int)tile.overlayData;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        byte data = 0;

        if(plan.config instanceof Integer i){
            data = i.byteValue();
        }

        int letterChar = CharOverlayData.character(data);

        TextureRegion reg = letterRegions[letterChar];
        Draw.tint(color);
        Draw.rect(reg, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.tint(Color.white);
    }

    @Override
    public void onPicked(Tile tile){
        Vars.control.input.rotation = CharOverlayData.rotation(tile.overlayData);
    }

    @Override
    public void buildEditorConfig(Table table){
        char value = chars.charAt(lastConfig instanceof Integer i ? CharOverlayData.character(i.byteValue()) : 0);
        table.field(value + "", val -> {
            if(val.length() == 1){
                lastConfig = (int)charToData(val.charAt(0));
            }
        }).valid(t -> t.length() == 1 && chars.indexOf(Character.toUpperCase(t.charAt(0))) != -1).maxTextLength(1);
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        byte data = 0;
        if(config instanceof Integer i){
            data = i.byteValue();
        }
        tile.overlayData = CharOverlayData.get(data, (byte)rotation);
    }

    @Override
    public void editorPicked(Tile tile){
        lastConfig = (int)CharOverlayData.character(tile.overlayData);
    }

    public static byte charToData(char c){
        int index = chars.indexOf(Character.toUpperCase(c));
        return index == -1 ? 0 : (byte)index;
    }

    @Struct
    class CharOverlayDataStruct{
        @StructField(6)
        byte character;
        @StructField(2)
        byte rotation;
    }
}
