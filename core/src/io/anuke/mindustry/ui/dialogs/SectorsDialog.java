package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.maps.Sector;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.scene.utils.ScissorStack;

import static io.anuke.mindustry.Vars.world;

public class SectorsDialog extends FloatingDialog{
    private Rectangle clip = new Rectangle();

    public SectorsDialog(){
        super("$text.sectors");

        addCloseButton();
        setup();
    }

    void setup(){
        content().clear();

        content().add(new SectorView()).grow();
    }

    class SectorView extends Element{
        float panX, panY;
        float lastX, lastY;
        float sectorSize = 100f;

        SectorView(){
            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                    Cursors.setHand();
                    lastX = x;
                    lastY = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    panX -= x - lastX;
                    panY -= y - lastY;

                    lastX = x;
                    lastY = y;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button){
                    Cursors.restoreCursor();
                }
            });
        }

        @Override
        public void draw(){
            Draw.alpha(alpha);

            float clipSize = Math.min(width, height);
            int shownSectors = Math.round(clipSize/sectorSize/2f + 1f);
            clip.setSize(clipSize).setCenter(x + width/2f, y + height/2f);
            Graphics.flush();
            boolean clipped = ScissorStack.pushScissors(clip);

            int offsetX = (int)(panX / sectorSize);
            int offsetY = (int)(panY / sectorSize);

            for(int x = -shownSectors; x <= shownSectors; x++){
                for(int y = -shownSectors; y <= shownSectors; y++){
                    int sectorX = offsetX + x;
                    int sectorY = offsetY + y;

                    float drawX = x + width/2f+ sectorX * sectorSize - offsetX * sectorSize - panX % sectorSize;
                    float drawY = y + height/2f + sectorY * sectorSize - offsetY * sectorSize - panY % sectorSize;

                    if(world.sectors().get(sectorX, sectorY) == null){
                        world.sectors().unlockSector(sectorX, sectorY);
                    }

                    Sector sector = world.sectors().get(sectorX, sectorY);
                    Draw.rect(sector.texture, drawX, drawY, sectorSize, sectorSize);
                    Lines.stroke(2f);
                    Lines.crect(drawX, drawY, sectorSize, sectorSize);

                }
            }

            Draw.reset();
            Graphics.flush();
            if(clipped) ScissorStack.popScissors();
        }
    }
}
