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

public class RuneOverlay extends OverlayFloor{
    public static final int characters = 109;
    public static final int unicodeOffset = 0x142B;

    public @Load(value = "@#", fallback = "rune-overlay#", length = characters) TextureRegion[] letterRegions;
    public Color color = Color.white;

    public RuneOverlay(String name){
        super(name);
        saveData = true;
        variants = 0;
        saveConfig = true;
        editorConfigurable = true;
    }

    /** Encodes rune data bytes into a string that can be displayed in the font. */
    public static String bytesToString(byte[] data){
        StringBuilder result = new StringBuilder();
        for(byte b : data){
            result.append((char)(b & 0xff + unicodeOffset));
        }
        return result.toString();
    }

    /** Converts a displayable string into rune data bytes. Will generate garbage data if the string doesn't contain the right character set. */
    public static byte[] stringToBytes(String s){
        byte[] bytes = new byte[s.length()];
        for(int i = 0; i < s.length(); i++){
            bytes[i] = (byte)(s.charAt(i) - unicodeOffset);
        }
        return bytes;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.color(color);
        if((tile.overlayData & 0xff) < characters){
            Draw.rect(letterRegions[tile.overlayData & 0xff], tile.worldx(), tile.worldy());
        }
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

        int letterChar = data & 0xff;

        if(letterChar < characters){
            TextureRegion reg = letterRegions[letterChar];
            Draw.tint(color);
            Draw.rect(reg, plan.drawx(), plan.drawy());
            Draw.tint(Color.white);
        }
    }

    @Override
    public void onPicked(Tile tile){
        Vars.control.input.rotation = CharOverlayData.rotation(tile.overlayData);
    }

    @Override
    public void buildEditorConfig(Table table){
        int value = lastConfig instanceof Integer i ? i : 0;
        table.field(value + "", val -> lastConfig = Strings.parseInt(val))
        .valid(t -> Strings.canParsePositiveInt(t) && Strings.parseInt(t, 999) < characters);
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        if(config instanceof Integer i){
            tile.overlayData = i.byteValue();
        }
    }

    @Override
    public void editorPicked(Tile tile){
        lastConfig = tile.overlayData;
    }
}
