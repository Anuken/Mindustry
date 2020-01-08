package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.world.*;

public class SeamlessSolarGenerator extends SolarGenerator{
    private TextureRegion[] compass = new TextureRegion[8];

    public SeamlessSolarGenerator(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();

        for(int i = 0; i < compass.length; i++){
            compass[i] = Core.atlas.find(name + "-" + i);
        }
    }

    @Override
    protected TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-full")};
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        boolean // where it should edge
        up = foreign(tile, 0, 1),
        right = foreign(tile, 1, 0),
        down = foreign(tile, 0, -1),
        left = foreign(tile, -1, 0);

        if(up)    Draw.rect(compass[0], tile.drawx(), tile.drawy());
        if(right) Draw.rect(compass[2], tile.drawx(), tile.drawy());
        if(down)  Draw.rect(compass[4], tile.drawx(), tile.drawy());
        if(left)  Draw.rect(compass[6], tile.drawx(), tile.drawy());

        if(up && right)   Draw.rect(compass[1], tile.drawx(), tile.drawy());
        if(right && down) Draw.rect(compass[3], tile.drawx(), tile.drawy());
        if(down && left)  Draw.rect(compass[5], tile.drawx(), tile.drawy());
        if(left && up)    Draw.rect(compass[7], tile.drawx(), tile.drawy());
    }

    private boolean foreign(Tile tile, int dx, int dy){
        if(tile.getNearby(dx, dy) == null) return true;
        if(tile.getNearby(dx, dy).block() == Blocks.solarPanel) return false;

        return true;
    }
}
