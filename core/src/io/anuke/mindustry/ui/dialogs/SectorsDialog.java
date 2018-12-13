package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.maps.Sector;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class SectorsDialog extends FloatingDialog{
    private static final float sectorSize = Unit.dp.scl(32*5);
    private Sector selected;
    private Table table;
    private SectorView view;

    public SectorsDialog(){
        super("");

        table = new Table(){
            @Override
            public float getPrefWidth(){
                return sectorSize*2f;
            }
        };
        table.visible(() -> selected != null);
        table.update(() -> {
            if(selected != null){

                int offsetX = (int)(view.panX / sectorSize);
                int offsetY = (int)(view.panY / sectorSize);
                float drawX = x + width/2f+ selected.x * (sectorSize-2) - offsetX * sectorSize - view.panX % sectorSize + sectorSize/2f;
                float drawY = y + height/2f + selected.y * (sectorSize-2) - offsetY * sectorSize - view.panY % sectorSize + sectorSize/2f;

                table.setPosition(drawX, drawY - sectorSize/2f + 1, Align.top);
            }
        });

        Group container = new Group();
        container.setTouchable(Touchable.childrenOnly);
        container.addChild(table);

        margin(0);
        getTitleTable().clear();
        clear();
        stack(content(), container, buttons()).grow();

        shown(this::setup);
    }

    void setup(){
        selected = null;

        table.clear();
        content().clear();
        buttons().clear();
        buttons().bottom().margin(15);

        addCloseButton();
        content().add(view = new SectorView()).grow();
    }

    void selectSector(Sector sector){
        selected = sector;

        table.clear();
        table.background("button").margin(5);

        table.defaults().pad(3);
        table.add(Bundles.format("text.sector", sector.x + ", " + sector.y));
        table.row();

        if(selected.completedMissions < selected.missions.size && !selected.complete){
            table.labelWrap(Bundles.format("text.mission", selected.getDominantMission().menuDisplayString())).growX();
            table.row();
        }

        if(selected.hasSave()){
            table.labelWrap(Bundles.format("text.sector.time", selected.getSave().getPlayTime())).growX();
            table.row();
        }

        table.table(t -> {
            Cell<?> cell = t.addImageTextButton(sector.hasSave() ? "$text.sector.resume" : "$text.sector.deploy", "icon-play", 10*3, () -> {
                hide();
                Vars.ui.loadLogic(() -> world.sectors.playSector(selected));
            }).height(60f);

            if(selected.hasSave()){
                t.addImageTextButton("$text.sector.abandon", "icon-cancel", 16 * 2, () ->
                    Vars.ui.showConfirm("$text.confirm", "$text.sector.abandon.confirm", () -> {
                        world.sectors.abandonSector(selected);
                        // Simulate a sector selection so the buttons get updated.
                        selectSector(selected);
                    })
                ).width(sectorSize / Unit.dp.scl(1f)).height(60f);
                cell.width(sectorSize / Unit.dp.scl(1f));
            }else{
                cell.width(sectorSize*2f / Unit.dp.scl(1f));
            }
        }).pad(-5).growX().padTop(0);

        table.pack();
        table.act(Gdx.graphics.getDeltaTime());
    }

    public Sector getSelected(){
        return selected;
    }

    class SectorView extends Element{
        float lastX, lastY;
        boolean clicked = false;
        float panX = sectorSize/2f, panY = sectorSize/2f;

        SectorView(){
            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                    if(pointer != 0) return false;
                    //Cursors.setHand();
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

            int shownSectorsX = (int)(width/sectorSize);
            int shownSectorsY = (int)(height/sectorSize);

            int offsetX = (int)(panX / sectorSize);
            int offsetY = (int)(panY / sectorSize);

            Vector2 mouse = Graphics.mouse();

            for(int x = -shownSectorsX; x <= shownSectorsX; x++){
                for(int y = -shownSectorsY; y <= shownSectorsY; y++){
                    int sectorX = offsetX + x;
                    int sectorY = offsetY + y;

                    float drawX = x + width/2f+ sectorX * (sectorSize-2) - offsetX * sectorSize - panX % sectorSize + sectorSize/2f;
                    float drawY = y + height/2f + sectorY * (sectorSize-2) - offsetY * sectorSize - panY % sectorSize + sectorSize/2f;

                    Sector sector = world.sectors.get(sectorX, sectorY);

                    if(sector == null || sector.texture == null){
                        Draw.reset();
                        Draw.rect("empty-sector", drawX, drawY, sectorSize, sectorSize);

                        int i = 0;
                        for(GridPoint2 point : Geometry.d4){
                            Sector other = world.sectors.get(sectorX + point.x, sectorY + point.y);
                            if(other != null){
                                Draw.rect("sector-edge", drawX, drawY, sectorSize, sectorSize, i*90);
                            }

                            i ++;
                        }
                        continue;
                    }

                    Draw.colorl(!sector.complete ? 0.3f : 1f);
                    Draw.rect(sector.texture, drawX, drawY, sectorSize, sectorSize);

                    if(sector.missions.size == 0) continue;

                    String region = sector.getDominantMission().getIcon();

                    if(sector.complete){
                        region = "icon-mission-done";
                    }

                    Color iconColor = Color.WHITE;
                    Color backColor = Color.BLACK;
                    Color selectColor = Color.CLEAR;

                    if(sector == selected){
                        selectColor = Palette.accent;
                    }else if(Mathf.inRect(mouse.x, mouse.y, drawX - sectorSize / 2f, drawY - sectorSize / 2f,
                        drawX + sectorSize / 2f, drawY + sectorSize / 2f)){
                        if(clicked){
                            selectSector(sector);
                        }
                        selectColor = Color.WHITE;
                    }else if(sector.hasSave()){
                        iconColor = Palette.command;
                    }else{
                        iconColor = Color.GRAY;
                    }

                    if(sector.complete){
                        iconColor = backColor = Color.CLEAR;
                    }

                    Draw.color(selectColor);
                    Draw.rect("sector-select", drawX, drawY, sectorSize, sectorSize);

                    Draw.color(backColor);
                    Draw.alpha(0.75f * backColor.a);
                    Draw.rect("icon-mission-background", drawX, drawY, Unit.dp.scl(18f * 5), Unit.dp.scl(18f * 5));

                    float size = Unit.dp.scl(10f * 5);

                    Draw.color(iconColor);
                    Draw.rect(region, drawX, drawY, size, size);
                }
            }

            Draw.reset();

            clicked = false;
        }
    }
}
