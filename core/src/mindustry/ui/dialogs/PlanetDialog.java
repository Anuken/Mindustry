package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PlanetDialog extends BaseDialog{
    private final FrameBuffer buffer = new FrameBuffer(2, 2, true);
    private final PlanetRenderer planets = renderer.planets;

    private float zoom = 1f;
    private Table stable;

    public PlanetDialog(){
        super("", Styles.fullDialog);

        getCell(buttons).padBottom(-4);
        buttons.background(Styles.black).defaults().growX().height(64f).pad(0);

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                Core.app.post(this::hide);
            }
        });

        dragged((cx, cy) -> {
            Vec3 pos = planets.camPos;

            float upV = pos.angle(Vec3.Y);
            float xscale = 9f, yscale = 10f;
            float margin = 1;

            //scale X speed depending on polar coordinate
            float speed = 1f - Math.abs(upV - 90) / 90f;

            pos.rotate(planets.cam.up, cx / xscale * speed);

            //prevent user from scrolling all the way up and glitching it out
            float amount = cy / yscale;
            amount = Mathf.clamp(upV + amount, margin, 180f - margin) - upV;

            pos.rotate(Tmp.v31.set(planets.cam.up).rotate(planets.cam.direction, 90), amount);
        });

        scrolled(value -> {
            zoom = Mathf.clamp(zoom + value / 10f, 0.5f, 2f);
        });

        addListener(new ElementGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                planets.selected = planets.hovered != null && planets.hovered.locked() ? null : planets.hovered;
                if(planets.selected != null){
                    updateSelected();
                }
            }
        });

        stable = new Table();
        stable.background(Styles.black3);

        shown(this::setup);
    }

    /** show with no limitations, just as a map. */
    @Override
    public Dialog show(){
        //TODO
        return super.show();
    }

    public void show(Sector selected, int range){
        planets.selected = null;
        planets.hovered = null;

        //update view to sector
        planets.camPos.set(Tmp.v33.set(selected.tile.v).rotate(Vec3.Y, -selected.planet.getRotation()));
        zoom = planets.zoom = 1f;

        show();
        //TODO
    }

    void setup(){
        cont.clear();
        titleTable.remove();

        cont.rect((x, y, w, h) -> planets.render()).grow();
    }

    @Override
    public void draw(){
        boolean doBuffer = color.a < 0.99f;

        if(doBuffer){
            buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
            buffer.begin(Color.clear);
        }

        super.draw();

        if(doBuffer){
            buffer.end();
            Draw.color(color);
            Draw.rect(Draw.wrap(buffer.getTexture()), Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f, Core.graphics.getWidth(), -Core.graphics.getHeight());
            Draw.color();
        }
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(planets.selected != null){
            addChild(stable);
            Vec3 pos = planets.cam.project(Tmp.v31.set(planets.selected.tile.v).setLength(PlanetRenderer.outlineRad).rotate(Vec3.Y, -planets.planet.getRotation()).add(planets.planet.position));
            stable.setPosition(pos.x, pos.y, Align.center);
            stable.toFront();
        }else{
            stable.remove();
        }

        if(planets.planet.isLandable()){
            planets.hovered = planets.planet.getSector(planets.cam.getMouseRay(), PlanetRenderer.outlineRad);
        }else{
            planets.hovered = planets.selected = null;
        }

        planets.zoom = Mathf.lerpDelta(planets.zoom, zoom, 0.4f);
        planets.selectAlpha = Mathf.lerpDelta(planets.selectAlpha, Mathf.num(planets.zoom < 1.9f), 0.1f);
    }

    //TODO add strings to bundle after prototyping is done
    private void updateSelected(){
        Sector sector = planets.selected;

        float x = stable.getX(Align.center), y = stable.getY(Align.center);
        stable.clear();
        stable.background(Styles.black6);

        stable.add("[accent]" + (sector.preset == null ? sector.id : sector.preset.localizedName)).row();
        stable.image().color(Pal.accent).fillX().height(3f).pad(3f).row();
        stable.add(sector.save != null ? sector.save.getPlayTime() : "[lightgray]Unexplored").row();

        stable.add("Resources:").row();
        stable.table(t -> {
            t.left();
            int idx = 0;
            int max = 5;
            for(UnlockableContent c : sector.data.resources){
                t.image(c.icon(Cicon.small)).padRight(3);
                if(++idx % max == 0) t.row();
            }
        }).fillX().row();

        //production
        if(sector.hasBase() && sector.save.meta.hasProduction){
            stable.add("Production:").row();
            stable.table(t -> {
                t.left();

                sector.save.meta.secinfo.exportRates().each(entry -> {
                    int total = (int)(entry.value * turnDuration / 60f);
                    if(total > 1){
                        t.image(entry.key.icon(Cicon.small)).padRight(3);
                        t.add(ui.formatAmount(total) + " /turn").color(Color.lightGray);
                        t.row();
                    }
                });
            }).row();
        }

        //stored resources
        if(sector.hasBase() && sector.save.meta.secinfo.coreItems.size > 0){
            stable.add("Stored:").row();
            stable.table(t -> {
                t.left();

                t.table(res -> {
                    int i = 0;
                    for(Item item : content.items()){
                        int amount = sector.save.meta.secinfo.coreItems.get(item);
                        if(amount > 0){
                            res.image(item.icon(Cicon.small)).padRight(3);
                            res.add(ui.formatAmount(amount)).color(Color.lightGray);
                            if(++i % 2 == 0){
                                res.row();
                            }
                        }
                    }
                });


            }).row();
        }

        //display how many turns this sector has been attacked
        if(sector.getTurnsPassed() > 0 && sector.hasBase()){
            stable.row();

            stable.add("[scarlet]" + Iconc.warning + " " + sector.getTurnsPassed() + "x attacks");
        }

        stable.row();

        stable.button("Launch", Styles.transt, () -> {
            if(sector.is(SectorAttribute.naval)){
                ui.showInfo("You need a naval loadout to launch here.");
                return;
            }
            control.playSector(sector);
            hide();
        }).growX().padTop(2f).height(50f).minWidth(170f);

        stable.pack();
        stable.setPosition(x, y, Align.center);

        stable.update(() -> {
            if(planets.selected != null){
                //fade out UI when not facing selected sector
                Tmp.v31.set(planets.selected.tile.v).rotate(Vec3.Y, -planets.planet.getRotation()).scl(-1f).nor();
                float dot = planets.cam.direction.dot(Tmp.v31);
                stable.getColor().a = Math.max(dot, 0f)*2f;
                if(dot*2f <= -0.1f){
                    stable.remove();
                    planets.selected = null;
                }
            }
        });

        stable.act(0f);
    }
}
