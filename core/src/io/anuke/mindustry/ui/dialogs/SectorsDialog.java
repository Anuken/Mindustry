package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.maps.Sector;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class SectorsDialog extends FloatingDialog{
    private Sector selected;

    public SectorsDialog(){
        super("");

        margin(0);
        getTitleTable().clear();
        clear();
        stack(content(), buttons()).grow();

        shown(this::setup);
    }

    void setup(){
        selected = null;
        content().clear();
        buttons().clear();
        buttons().bottom().margin(15);

        addCloseButton();

        /*
        content().label(() -> Bundles.format("text.sector", selected == null ? Bundles.get("text.none") :
        (selected.x + ", " + selected.y + (!selected.complete && selected.saveID != -1 ? " " + Bundles.get("text.sector.locked") : ""))
                + (selected.saveID == -1 ? " " + Bundles.get("text.sector.unexplored") :
                    (selected.hasSave() ? "  [accent]/[white] " + Bundles.format("text.sector.time", selected.getSave().getPlayTime()) : ""))));
        content().row();
        content().label(() -> Bundles.format("text.mission.main", selected == null || selected.completedMissions >= selected.missions.size
        ? Bundles.get("text.none") : selected.getDominantMission().menuDisplayString()));
        content().row();*/
        content().add(new SectorView()).grow();
        //content().row();
        /*

        buttons().addImageTextButton("$text.sector.abandon", "icon-cancel",  16*2, () ->
                ui.showConfirm("$text.confirm", "$text.sector.abandon.confirm", () -> world.sectors.abandonSector(selected)))
        .size(200f, 64f).disabled(b -> selected == null || !selected.hasSave());

        buttons().row();

        buttons().addImageTextButton("$text.sector.deploy", "icon-play",  10*3, () -> {
            hide();
            ui.loadLogic(() -> world.sectors.playSector(selected));
        }).disabled(b -> selected == null)
            .fillX().height(64f).colspan(2).update(t -> t.setText(selected != null && selected.hasSave() ? "$text.sector.resume" : "$text.sector.deploy"));*/
    }

    void selectSector(Sector sector){
        selected = sector;
    }

    public Sector getSelected(){
        return selected;
    }

    class SectorView extends Element{
        float lastX, lastY;
        float sectorSize = Unit.dp.scl(32*5);
        boolean clicked = false;
        float panX = 0, panY = -sectorSize/2f;

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

            float padSectorSize = sectorSize;

            int shownSectorsX = (int)(width/padSectorSize);
            int shownSectorsY = (int)(height/padSectorSize);

            int offsetX = (int)(panX / padSectorSize);
            int offsetY = (int)(panY / padSectorSize);

            Vector2 mouse = Graphics.mouse();

            for(int x = -shownSectorsX; x <= shownSectorsX; x++){
                for(int y = -shownSectorsY; y <= shownSectorsY; y++){
                    int sectorX = offsetX + x;
                    int sectorY = offsetY + y;

                    float drawX = x + width/2f+ sectorX * padSectorSize - offsetX * padSectorSize - panX % padSectorSize + padSectorSize/2f;
                    float drawY = y + height/2f + sectorY * padSectorSize - offsetY * padSectorSize - panY % padSectorSize + padSectorSize/2f;

                    Sector sector = world.sectors.get(sectorX, sectorY);

                    if(sector == null || sector.texture == null){
                        Draw.reset();
                        Draw.rect("empty-sector", drawX, drawY, sectorSize + 1f, sectorSize + 1f);
                        continue;
                    }

                    Draw.colorl(!sector.complete ? 0.3f : 1f);
                    Draw.rect(sector.texture, drawX, drawY, sectorSize + 1f, sectorSize + 1f);

                    if(sector.missions.size == 0) continue;

                    Draw.color(Color.BLACK);
                    Draw.alpha(0.75f);
                    Draw.rect("icon-mission-background", drawX, drawY, Unit.dp.scl(18f * 5), Unit.dp.scl(18f * 5));

                    String region = sector.getDominantMission().getIcon();

                    if(sector.complete){
                        region = "icon-mission-done";
                    }

                    if(sector == selected){
                        Draw.color(Color.WHITE);
                    }else if(Mathf.inRect(mouse.x, mouse.y, drawX - padSectorSize / 2f, drawY - padSectorSize / 2f,
                        drawX + padSectorSize / 2f, drawY + padSectorSize / 2f)){
                        if(clicked){
                            selectSector(sector);
                        }
                        Draw.color(Palette.remove);
                    }else if(sector.complete){
                        Draw.color(Palette.accent);
                    }else{
                        Draw.color(Color.LIGHT_GRAY);
                    }

                    float size = Unit.dp.scl(10f * 5);

                    Shaders.outline.color = Color.BLACK;
                    Shaders.outline.region = Draw.region(region);
                    Graphics.shader(Shaders.outline);
                    Draw.rect(region, drawX, drawY, size, size);
                    Graphics.shader();
                }
            }

            Draw.reset();

            clicked = false;
        }
    }
}
