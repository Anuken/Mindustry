package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.world.*;

public class ColoredFloor extends Floor{
    public Color defaultColor = Color.white;
    protected int defaultColorRgba;

    public ColoredFloor(String name){
        super(name);
        saveData = true;
    }

    @Override
    public void init(){
        super.init();
        defaultColorRgba = defaultColor.rgba();
    }

    @Override
    public void drawBase(Tile tile){
        //make sure to mask out the alpha channel - it's generally undesirable, and leads to invisible blocks when the data is not initialized
        Draw.color(tile.extraData | 0xff);
        super.drawBase(tile);
        Draw.color();
    }

    @Override
    public void drawOverlay(Tile tile){
        //make sure color doesn't carry over
        Draw.color();
        super.drawOverlay(tile);
    }

    @Override
    public void floorChanged(Tile tile){
        //reset to white
        tile.extraData = defaultColorRgba;
    }

    @Override
    public int minimapColor(Tile tile){
        return tile.extraData | 0xff;
    }
}
