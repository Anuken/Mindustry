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
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Unit;
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

        shown(this::setup);
    }

    void setup(){
        content().clear();
        buttons().clear();

        addCloseButton();

        content().label(() -> Bundles.format("text.sector", selected == null ? "<none>" :
        (selected.x + ", " + selected.y + (!selected.complete && selected.saveID != -1 ? " " + Bundles.get("text.sector.locked") : ""))
                + (selected.saveID == -1 ? " " + Bundles.get("text.sector.unexplored") :
                    (selected.hasSave() ? " [accent]/[white] " + Bundles.format("text.sector.time", selected.getSave().getPlayTime()) : ""))));
        content().row();
        content().add(new SectorView()).grow();
        content().row();
        buttons().addImageTextButton("$text.sector.deploy", "icon-play",  10*3, () -> {
            hide();

            ui.loadLogic(() -> world.sectors().playSector(selected));
        }).size(230f, 64f).name("deploy-button").disabled(b -> selected == null);

        if(debug){
            buttons().addButton("unlock",  () -> world.sectors().completeSector(selected.x, selected.y)).size(230f, 64f).disabled(b -> selected == null);
        }
    }

    void selectSector(Sector sector){
        buttons().<TextButton>find("deploy-button").setText(sector.hasSave() ? "$text.sector.resume" : "$text.sector.deploy");
        selected = sector;
    }

    class SectorView extends Element{
        float lastX, lastY;
        float sectorSize = Unit.dp.scl(100f);
        float sectorPadding = Unit.dp.scl(14f);
        boolean clicked = false;
        float panX = -sectorPadding/2f, panY = -sectorSize/2f;

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

            float padSectorSize = sectorSize + sectorPadding;

            float clipSize = Math.min(width, height);
            int shownSectors = (int)(clipSize/padSectorSize);
            clip.setSize(clipSize).setCenter(x + width/2f, y + height/2f);
            Graphics.flush();
            boolean clipped = ScissorStack.pushScissors(clip);

            int offsetX = (int)(panX / padSectorSize);
            int offsetY = (int)(panY / padSectorSize);

            Vector2 mouse = Graphics.mouse();

            for(int x = -shownSectors; x <= shownSectors; x++){
                for(int y = -shownSectors; y <= shownSectors; y++){
                    int sectorX = offsetX + x;
                    int sectorY = offsetY + y;

                    float drawX = x + width/2f+ sectorX * padSectorSize - offsetX * padSectorSize - panX % padSectorSize;
                    float drawY = y + height/2f + sectorY * padSectorSize - offsetY * padSectorSize - panY % padSectorSize;

                    Sector sector = world.sectors().get(sectorX, sectorY);
                    int size = (sector == null ? 1 : sector.size);
                    float padding = (size-1) * sectorPadding;

                    if(sector != null && (sector.x != sectorX || sector.y != sectorY)){
                        continue;
                    }

                    drawX += (size-1)/2f*padSectorSize;
                    drawY += (size-1)/2f*padSectorSize;

                    if(sector != null && sector.texture != null){
                        Draw.color(Color.WHITE);
                        Draw.rect(sector.texture, drawX, drawY, sectorSize * size + padding, sectorSize * size + padding);
                    }

                    float stroke = 4f;

                    if(sector == null){
                        Draw.color(Color.DARK_GRAY);
                    }else if(sector == selected){
                        Draw.color(Palette.place);
                        stroke = 6f;
                    }else if(Mathf.inRect(mouse.x, mouse.y, drawX - padSectorSize/2f * size, drawY - padSectorSize/2f * size,
                                                            drawX + padSectorSize/2f * size, drawY + padSectorSize/2f * size)){
                        if(clicked){
                            selectSector(sector);
                        }
                        Draw.color(Palette.remove);
                    }else if (sector.complete){
                        Draw.color(Palette.accent);
                    }else{
                        Draw.color(Color.LIGHT_GRAY);
                    }

                    Lines.stroke(Unit.dp.scl(stroke));
                    Lines.crect(drawX, drawY, sectorSize * size + padding, sectorSize * size + padding, (int)stroke);
                }
            }

            Draw.color(Palette.accent);
            Lines.stroke(Unit.dp.scl(4f));
            Lines.crect(x + width/2f, y + height/2f, clipSize, clipSize);

            Draw.reset();
            Graphics.flush();
            if(clipped) ScissorStack.popScissors();

            clicked = false;
        }
    }
}
