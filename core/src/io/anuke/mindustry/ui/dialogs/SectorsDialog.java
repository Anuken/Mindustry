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
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.scene.utils.ScissorStack;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.ui;
import static io.anuke.mindustry.Vars.world;

public class SectorsDialog extends FloatingDialog{
    private Rectangle clip = new Rectangle();
    private Sector selected;

    public SectorsDialog(){
        super("$text.sectors");

        shown(this::setup);
    }

    void setup(){
        selected = null;
        content().clear();
        buttons().clear();

        addCloseButton();

        content().label(() -> Bundles.format("text.sector", selected == null ? Bundles.get("text.none") :
        (selected.x + ", " + selected.y + (!selected.complete && selected.saveID != -1 ? " " + Bundles.get("text.sector.locked") : ""))
                + (selected.saveID == -1 ? " " + Bundles.get("text.sector.unexplored") :
                    (selected.hasSave() ? "  [accent]/[white] " + Bundles.format("text.sector.time", selected.getSave().getPlayTime()) : ""))));
        content().row();
        content().label(() -> Bundles.format("text.mission.main", selected == null || selected.completedMissions >= selected.missions.size
        ? Bundles.get("text.none") : selected.getDominantMission().menuDisplayString()));
        content().row();
        content().add(new SectorView()).grow();
        content().row();

        buttons().addImageTextButton("$text.sector.abandon", "icon-cancel",  16*2, () ->
                ui.showConfirm("$text.confirm", "$text.sector.abandon.confirm", () -> world.sectors.abandonSector(selected)))
        .size(200f, 64f).disabled(b -> selected == null || !selected.hasSave());

        buttons().row();

        buttons().addImageTextButton("$text.sector.deploy", "icon-play",  10*3, () -> {
            hide();
            ui.loadLogic(() -> world.sectors.playSector(selected));
        }).disabled(b -> selected == null)
            .fillX().height(64f).colspan(2).update(t -> t.setText(selected != null && selected.hasSave() ? "$text.sector.resume" : "$text.sector.deploy"));
    }

    void selectSector(Sector sector){
        selected = sector;
    }

    public Sector getSelected(){
        return selected;
    }

    class SectorView extends Element{
        float lastX, lastY;
        float sectorSize = Unit.dp.scl(32*4);
        float sectorPadding = Unit.dp.scl(14f);
        boolean clicked = false;
        float panX = -sectorPadding/2f, panY = -sectorSize/2f;

        SectorView(){
            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                    if(pointer != 0) return false;
                    Cursors.setHand();
                    lastX = x;
                    lastY = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    if(pointer != 0) return;
                    panX -= x - lastX;
                    panY -= y - lastY;

                    lastX = x;
                    lastY = y;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button){
                    if(pointer != 0) return;
                    Cursors.restoreCursor();
                }
            });

            clicked(() -> clicked = true);
        }

        @Override
        public void draw(){
            Draw.alpha(alpha);

            float padSectorSize = sectorSize + sectorPadding;

            int shownSectorsX = (int)(width/padSectorSize);
            int shownSectorsY = (int)(height/padSectorSize);
            clip.setSize(width, height).setCenter(x + width/2f, y + height/2f);
            Graphics.flush();
            boolean clipped = ScissorStack.pushScissors(clip);

            int offsetX = (int)(panX / padSectorSize);
            int offsetY = (int)(panY / padSectorSize);

            Vector2 mouse = Graphics.mouse();

            for(int x = -shownSectorsX; x <= shownSectorsX; x++){
                for(int y = -shownSectorsY; y <= shownSectorsY; y++){
                    int sectorX = offsetX + x;
                    int sectorY = offsetY + y;

                    float drawX = x + width/2f+ sectorX * padSectorSize - offsetX * padSectorSize - panX % padSectorSize;
                    float drawY = y + height/2f + sectorY * padSectorSize - offsetY * padSectorSize - panY % padSectorSize;

                    Sector sector = world.sectors.get(sectorX, sectorY);
                    int width = (sector == null ? 1 : sector.width);
                    int height = (sector == null ? 1 : sector.height);
                    float paddingx = (width-1) * sectorPadding;
                    float paddingy = (height-1) * sectorPadding;

                    if(sector != null && (sector.x != sectorX || sector.y != sectorY)){
                        continue;
                    }

                    drawX += (width-1)/2f*padSectorSize;
                    drawY += (height-1)/2f*padSectorSize;

                    if(sector != null && sector.texture != null){
                        Draw.colorl(!sector.complete ? 0.3f : 1f);
                        Draw.rect(sector.texture, drawX, drawY, sectorSize * width + paddingx, sectorSize * height + paddingy);
                    }

                    float stroke = 4f;

                    if(sector == null){
                        Draw.color(Color.DARK_GRAY);
                    }else if(sector == selected){
                        Draw.color(Palette.place);
                        stroke = 6f;
                    }else if(Mathf.inRect(mouse.x, mouse.y, drawX - padSectorSize/2f * width, drawY - padSectorSize/2f * height,
                                                            drawX + padSectorSize/2f * width, drawY + padSectorSize/2f * height)){
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
                    Lines.crect(drawX, drawY, sectorSize * width + paddingx, sectorSize * height + paddingy, (int)stroke);
                }
            }

            Draw.color(Palette.accent);
            Lines.stroke(Unit.dp.scl(4f));
            Lines.crect(x + width/2f, y + height/2f, width, height);

            Draw.reset();
            Graphics.flush();
            if(clipped) ScissorStack.popScissors();

            clicked = false;
        }
    }
}
