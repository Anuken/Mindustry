package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.*;

public class CharacterOverlay extends OverlayFloor{
    /** This is a reduced character set! It is not ASCII! */
    public static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\"!?.,;:()[]{}<>|/@\\^-%+=#_&~";

    public @Load(value = "character-overlay#", length = 64) TextureRegion[] letterRegions;
    public Color color = Color.white;

    public CharacterOverlay(String name){
        super(name);
        saveData = true;
        variants = 0;
        rotate = true;
        drawArrow = false;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.color(color);
        int letterChar = CharOverlayData.character(tile.overlayData);
        Draw.rect(letterRegions[letterChar], tile.worldx(), tile.worldy(), CharOverlayData.rotation(tile.overlayData) * 90f);
        Draw.color();
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        byte data = 0;
        if(config instanceof Integer i){
            data = i.byteValue();
        }
        tile.overlayData = CharOverlayData.get(data, (byte)rotation);
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
