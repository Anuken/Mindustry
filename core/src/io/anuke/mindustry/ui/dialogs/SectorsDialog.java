package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.maps.Sector;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.scene.utils.ScissorStack;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class SectorsDialog extends FloatingDialog{
    private Rectangle clip = new Rectangle();
    private Sector selected;

    public SectorsDialog(){
        super("$text.sectors");

        addCloseButton();
        setup();
    }

    void setup(){
        content().clear();

        content().label(() -> Bundles.format("text.sector", selected == null ? "<none>" :
        selected.x + ", " + selected.y + (!selected.unlocked ? Bundles.get("text.sector.locked") : "")));
        content().row();
        content().add(new SectorView()).grow();
        content().row();
        buttons().addImageTextButton("$text.sector.deploy", "icon-play",  10*3, () -> {
            hide();

            ui.loadLogic(() -> {
                world.loadProceduralMap(selected.x, selected.y);
                logic.play();
            });
        }).size(230f, 64f).disabled(b -> selected == null);
    }

    class SectorView extends Element{
        float panX, panY;
        float lastX, lastY;
        float sectorSize = 100f;
        boolean clicked = false;

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

            addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    clicked = true;
                }
            });
        }

        @Override
        public void draw(){
            Draw.alpha(alpha);

            float clipSize = Math.min(width, height);
            int shownSectors = (int)(clipSize/sectorSize);
            clip.setSize(clipSize).setCenter(x + width/2f, y + height/2f);
            Graphics.flush();
            boolean clipped = ScissorStack.pushScissors(clip);

            int offsetX = (int)(panX / sectorSize);
            int offsetY = (int)(panY / sectorSize);

            Vector2 mouse = Graphics.mouse();

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

                    if(sector == null) continue;

                    Draw.color(Color.WHITE);
                    Draw.rect(sector.texture, drawX, drawY, sectorSize, sectorSize);

                    if(sector == selected){
                        Draw.color(Palette.place);
                    }else if(Mathf.inRect(mouse.x, mouse.y, drawX - sectorSize/2f, drawY - sectorSize/2f, drawX + sectorSize/2f, drawY + sectorSize/2f)){
                        if(clicked){
                            selected = sector;
                        }
                        Draw.color(Palette.remove);
                    }else if (sector.unlocked){
                        Draw.color(Palette.accent);
                    }else{
                        Draw.color(Color.LIGHT_GRAY);
                    }

                    Lines.stroke(selected == sector ? 5f : 3f);
                    Lines.crect(drawX, drawY, sectorSize, sectorSize);
                }
            }

            Draw.color(Palette.accent);
            Lines.stroke(4f);
            Lines.crect(x + width/2f, y + height/2f, clipSize, clipSize);

            Draw.reset();
            Graphics.flush();
            if(clipped) ScissorStack.popScissors();

            clicked = false;
        }
    }
}
