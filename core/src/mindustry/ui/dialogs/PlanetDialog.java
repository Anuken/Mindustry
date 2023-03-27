package mindustry.ui.dialogs;

import arc.*;
import arc.assets.loaders.TextureLoader.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.game.SectorInfo.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.graphics.g3d.*;
import mindustry.input.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.graphics.g3d.PlanetRenderer.*;
import static mindustry.ui.dialogs.PlanetDialog.Mode.*;

public class PlanetDialog extends BaseDialog implements PlanetInterfaceRenderer{
    static final String[] defaultIcons = {
    "effect", "power", "logic", "units", "liquid", "production", "defense", "turret", "distribution", "crafting",
    "settings", "cancel", "zoom", "ok", "star", "home", "pencil", "up", "down", "left", "right",
    "hammer", "warning", "tree", "admin", "map", "modePvp", "terrain",
    "modeSurvival", "commandRally", "commandAttack",
    };

    //if true, enables launching anywhere for testing
    public static boolean debugSelect = false;
    public static float sectorShowDuration = 60f * 2.4f;

    public final FrameBuffer buffer = new FrameBuffer(2, 2, true);
    public final LaunchLoadoutDialog loadouts = new LaunchLoadoutDialog();
    public final PlanetRenderer planets = renderer.planets;

    public PlanetParams state = new PlanetParams();
    public float zoom = 1f;
    public @Nullable Sector selected, hovered, launchSector;
    public Mode mode = look;
    public boolean launching;
    public Cons<Sector> listener = s -> {};

    public Seq<Sector> newPresets = new Seq<>();
    public float presetShow = 0f;
    public boolean showed = false, sectorsShown;
    public String searchText = "";

    public Table sectorTop = new Table(), notifs = new Table(), expandTable = new Table();
    public Label hoverLabel = new Label("");

    private Texture[] planetTextures;

    public PlanetDialog(){
        super("", Styles.fullDialog);
        
        state.renderer = this;
        state.drawUi = true;

        shouldPause = true;
        state.planet = content.getByName(ContentType.planet, Core.settings.getString("lastplanet", "serpulo"));
        if(state.planet == null) state.planet = Planets.serpulo;

        addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, KeyCode key){
                if(event.targetActor == PlanetDialog.this && (key == KeyCode.escape || key == KeyCode.back || key == Core.keybinds.get(Binding.planet_map).key)){
                    if(showing() && newPresets.size > 1){
                        //clear all except first, which is the last sector.
                        newPresets.truncate(1);
                    }else if(selected != null){
                        selectSector(null);
                    }else{
                        Core.app.post(() -> hide());
                    }
                    return true;
                }
                return false;
            }
        });

        hoverLabel.setStyle(Styles.outlineLabel);
        hoverLabel.setAlignment(Align.center);

        rebuildButtons();

        onResize(this::rebuildButtons);

        dragged((cx, cy) -> {
            //no multitouch drag
            if(Core.input.getTouches() > 1) return;

            if(showing()){
                newPresets.clear();
            }

            Vec3 pos = state.camPos;

            float upV = pos.angle(Vec3.Y);
            float xscale = 9f, yscale = 10f;
            float margin = 1;

            //scale X speed depending on polar coordinate
            float speed = 1f - Math.abs(upV - 90) / 90f;

            pos.rotate(state.camUp, cx / xscale * speed);

            //prevent user from scrolling all the way up and glitching it out
            float amount = cy / yscale;
            amount = Mathf.clamp(upV + amount, margin, 180f - margin) - upV;

            pos.rotate(Tmp.v31.set(state.camUp).rotate(state.camDir, 90), amount);
        });

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                if(event.targetActor == PlanetDialog.this){
                    zoom = Mathf.clamp(zoom + amountY / 10f, state.planet.minZoom, 2f);
                }
                return true;
            }
        });

        addCaptureListener(new ElementGestureListener(){
            float lastZoom = -1f;

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
               if(lastZoom < 0){
                   lastZoom = zoom;
               }

               zoom = (Mathf.clamp(initialDistance / distance * lastZoom, state.planet.minZoom, 2f));
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                lastZoom = zoom;
            }
        });

        shown(this::setup);

        //show selection of Erekir/Serpulo campaign if the user has no bases, and hasn't selected yet (essentially a "have they played campaign before" check)
        shown(() -> {
            if(!settings.getBool("campaignselect") && !content.planets().contains(p -> p.sectors.contains(s -> s.hasBase()))){
                var diag = new BaseDialog("@campaign.select");

                Planet[] selected = {null};
                var group = new ButtonGroup<>();
                group.setMinCheckCount(0);
                state.planet = Planets.sun;
                Planet[] choices = {Planets.serpulo, Planets.erekir};
                int i = 0;
                for(var planet : choices){
                    TextureRegion tex = new TextureRegion(planetTextures[i]);

                    diag.cont.button(b -> {
                        b.top();
                        b.add(planet.localizedName).color(Pal.accent).style(Styles.outlineLabel);
                        b.row();
                        b.image(new TextureRegionDrawable(tex)).grow().scaling(Scaling.fit);
                    }, Styles.togglet, () -> selected[0] = planet).size(Core.app.isMobile() ? 220f : 320f).group(group);
                    i ++;
                }

                diag.cont.row();
                diag.cont.label(() -> selected[0] == null ? "@campaign.none" : "@campaign." + selected[0].name).labelAlign(Align.center).style(Styles.outlineLabel).width(440f).wrap().colspan(2);

                diag.buttons.button("@ok", Icon.ok, () -> {
                    state.planet = selected[0];
                    lookAt(state.planet.getStartSector());
                    selectSector(state.planet.getStartSector());
                    settings.put("campaignselect", true);
                    diag.hide();
                }).size(300f, 64f).disabled(b -> selected[0] == null);

                app.post(diag::show);
            }
        });

        planetTextures = new Texture[2];
        String[] names = {"sprites/planets/serpulo.png", "sprites/planets/erekir.png"};
        for(int i = 0; i < names.length; i++){
            int fi = i;
            assets.load(names[i], Texture.class, new TextureParameter(){{
                minFilter = magFilter = TextureFilter.linear;
            }}).loaded = t -> planetTextures[fi] = t;
            assets.finishLoadingAsset(names[i]);
        }

        //unlock defaults for older campaign saves (TODO move? where to?)
        if(content.planets().contains(p -> p.sectors.contains(s -> s.hasBase())) || Blocks.scatter.unlocked() || Blocks.router.unlocked()){
            Seq.with(Blocks.junction, Blocks.mechanicalDrill, Blocks.conveyor, Blocks.duo, Items.copper, Items.lead).each(UnlockableContent::quietUnlock);
        }
    }

    /** show with no limitations, just as a map. */
    @Override
    public Dialog show(){
        if(net.client()){
            ui.showInfo("@map.multiplayer");
            return this;
        }

        //view current planet by default
        if(Vars.state.rules.sector != null){
            state.planet = Vars.state.rules.sector.planet;
            settings.put("lastplanet", state.planet.name);
        }

        if(!selectable(state.planet)){
            state.planet = Planets.serpulo;
        }

        rebuildButtons();
        mode = look;
        state.otherCamPos = null;
        selected = hovered = launchSector = null;
        launching = false;

        zoom = 1f;
        state.zoom = 1f;
        state.uiAlpha = 0f;
        launchSector = Vars.state.getSector();
        presetShow = 0f;
        showed = false;
        listener = s -> {};

        newPresets.clear();

        //announce new presets
        for(SectorPreset preset : content.sectors()){
            if(preset.unlocked() && !preset.alwaysUnlocked && !preset.sector.info.shown && !preset.sector.hasBase() && preset.planet == state.planet){
                newPresets.add(preset.sector);
                preset.sector.info.shown = true;
                preset.sector.saveInfo();
            }
        }

        if(newPresets.any()){
            newPresets.add(state.planet.getLastSector());
        }

        newPresets.reverse();
        updateSelected();

        if(state.planet.getLastSector() != null){
            lookAt(state.planet.getLastSector());
        }

        return super.show();
    }

    void rebuildButtons(){
        buttons.clearChildren();

        buttons.bottom();

        if(Core.graphics.isPortrait()){
            buttons.add(sectorTop).colspan(2).fillX().row();
            addBack();
            addTech();
        }else{
            addBack();
            buttons.add().growX();
            buttons.add(sectorTop).minWidth(230f);
            buttons.add().growX();
            addTech();
        }
    }

    void addBack(){
        buttons.button("@back", Icon.left, this::hide).size(200f, 54f).pad(2).bottom();
    }

    void addTech(){
        buttons.button("@techtree", Icon.tree, () -> ui.research.show()).size(200f, 54f).pad(2).bottom();
    }

    public void showOverview(){
        //TODO implement later if necessary
        /*
        sectors.captured = Captured Sectors
        sectors.explored = Explored Sectors
        sectors.production.total = Total Production
        sectors.resources.total = Total Resources
         */
        var dialog = new BaseDialog("@overview");
        dialog.addCloseButton();

        dialog.add("@sectors.captured");
    }

    //TODO not fully implemented, cutscene needed
    public void showPlanetLaunch(Sector sector, Cons<Sector> listener){
        selected = null;
        hovered = null;
        launching = false;
        this.listener = listener;
        launchSector = sector;

        //automatically select next planets;
        if(sector.planet.launchCandidates.size == 1){
            state.planet = sector.planet.launchCandidates.first();
            state.otherCamPos = sector.planet.position;
            state.otherCamAlpha = 0f;

            //unlock and highlight sector
            var destSec = state.planet.sectors.get(state.planet.startSector);
            var preset = destSec.preset;
            if(preset != null){
                preset.unlock();
            }
            selected = destSec;
            updateSelected();
            rebuildExpand();
        }

        //TODO pan over to correct planet

        //update view to sector
        zoom = 1f;
        state.zoom = 1f;
        state.uiAlpha = 0f;

        mode = planetLaunch;

        super.show();
    }

    public void showSelect(Sector sector, Cons<Sector> listener){
        selected = null;
        hovered = null;
        launching = false;
        this.listener = listener;

        //update view to sector
        lookAt(sector);
        zoom = 1f;
        state.zoom = 1f;
        state.uiAlpha = 0f;
        state.otherCamPos = null;
        launchSector = sector;

        mode = select;

        super.show();
    }

    void lookAt(Sector sector){
        if(sector.tile == Ptile.empty) return;

        state.planet = sector.planet;
        state.camPos.set(Tmp.v33.set(sector.tile.v).rotate(Vec3.Y, -sector.planet.getRotation()));
    }

    boolean canSelect(Sector sector){
        if(mode == select) return sector.hasBase() && launchSector != null && sector.planet == launchSector.planet;
        //cannot launch to existing sector w/ accelerator TODO test
        if(mode == planetLaunch) return sector.id == sector.planet.startSector;
        if(sector.hasBase() || sector.id == sector.planet.startSector) return true;
        //preset sectors can only be selected once unlocked
        if(sector.preset != null){
            TechNode node = sector.preset.techNode;
            return node == null || node.parent == null || (node.parent.content.unlocked() && (!(node.parent.content instanceof SectorPreset preset) || preset.sector.hasBase()));
        }

        return sector.planet.generator != null ?
            //use planet impl when possible
            sector.planet.generator.allowLanding(sector) :
            sector.hasBase() || sector.near().contains(Sector::hasBase); //near an occupied sector
    }

    Sector findLauncher(Sector to){
        Sector launchSector = this.launchSector != null && this.launchSector.planet == to.planet && this.launchSector.hasBase() ? this.launchSector : null;
        //directly nearby.
        if(to.near().contains(launchSector)) return launchSector;

        Sector launchFrom = launchSector;
        if(launchFrom == null || (to.preset == null && !to.near().contains(launchSector))){
            //TODO pick one with the most resources
            launchFrom = to.near().find(Sector::hasBase);
            if(launchFrom == null && to.preset != null){
                if(launchSector != null) return launchSector;
                launchFrom = state.planet.sectors.min(s -> !s.hasBase() ? Float.MAX_VALUE : s.tile.v.dst2(to.tile.v));
                if(!launchFrom.hasBase()) launchFrom = null;
            }
        }

        return launchFrom;
    }

    boolean showing(){
        return newPresets.any();
    }

    @Override
    public void renderSectors(Planet planet){

        //draw all sector stuff
        if(state.uiAlpha > 0.01f){
            for(Sector sec : planet.sectors){
                if(canSelect(sec) || sec.unlocked() || debugSelect){

                    Color color =
                    sec.hasBase() ? Tmp.c2.set(Team.sharded.color).lerp(Team.crux.color, sec.hasEnemyBase() ? 0.5f : 0f) :
                    sec.preset != null ?
                        sec.preset.unlocked() ? Tmp.c2.set(Team.derelict.color).lerp(Color.white, Mathf.absin(Time.time, 10f, 1f)) :
                        Color.gray :
                    sec.hasEnemyBase() ? Team.crux.color :
                    null;

                    if(color != null){
                        planets.drawSelection(sec, Tmp.c1.set(color).mul(0.8f).a(state.uiAlpha), 0.026f, -0.001f);
                    }
                }else{
                    planets.fill(sec, Tmp.c1.set(shadowColor).mul(1, 1, 1, state.uiAlpha), -0.001f);
                }
            }
        }

        Sector current = Vars.state.getSector() != null && Vars.state.getSector().isBeingPlayed() && Vars.state.getSector().planet == state.planet ? Vars.state.getSector() : null;

        if(current != null){
            planets.fill(current, hoverColor.write(Tmp.c1).mulA(state.uiAlpha), -0.001f);
        }

        //draw hover border
        if(hovered != null){
            planets.fill(hovered, hoverColor.write(Tmp.c1).mulA(state.uiAlpha), -0.001f);
            planets.drawBorders(hovered, borderColor, state.uiAlpha);
        }

        //draw selected borders
        if(selected != null){
            planets.drawSelection(selected, state.uiAlpha);
            planets.drawBorders(selected, borderColor, state.uiAlpha);
        }

        planets.batch.flush(Gl.triangles);

        if(hovered != null && !hovered.hasBase()){
            Sector launchFrom = findLauncher(hovered);
            if(launchFrom != null && hovered != launchFrom && canSelect(hovered)){
                planets.drawArc(planet, launchFrom.tile.v, hovered.tile.v);
            }
        }

        if(state.uiAlpha > 0.001f){
            for(Sector sec : planet.sectors){
                if(sec.hasBase()){
                    if(planet.allowSectorInvasion){
                        for(Sector enemy : sec.near()){
                            if(enemy.hasEnemyBase()){
                                planets.drawArc(planet, enemy.tile.v, sec.tile.v, Team.crux.color.write(Tmp.c2).a(state.uiAlpha), Color.clear, 0.24f, 110f, 25);
                            }
                        }
                    }

                    if(selected != null && selected != sec && selected.hasBase()){
                        //imports
                        if(sec.info.getRealDestination() == selected && sec.info.anyExports()){
                            planets.drawArc(planet, sec.tile.v, selected.tile.v, Color.gray.write(Tmp.c2).a(state.uiAlpha), Pal.accent.write(Tmp.c3).a(state.uiAlpha), 0.4f, 90f, 25);
                        }
                        //exports
                        if(selected.info.getRealDestination() == sec && selected.info.anyExports()){
                            planets.drawArc(planet, selected.tile.v, sec.tile.v, Pal.place.write(Tmp.c2).a(state.uiAlpha), Pal.accent.write(Tmp.c3).a(state.uiAlpha), 0.4f, 90f, 25);
                        }
                    }
                }
            }
        }

    }

    @Override
    public void renderProjections(Planet planet){
        float iw = 48f/4f;

        for(Sector sec : planet.sectors){
            if(sec != hovered){
                var preficon = sec.icon();
                var icon =
                    sec.isAttacked() ? Fonts.getLargeIcon("warning") :
                    !sec.hasBase() && sec.preset != null && sec.preset.unlocked() && preficon == null ?
                    Fonts.getLargeIcon("terrain") :
                    sec.preset != null && sec.preset.locked() && sec.preset.techNode != null && !sec.preset.techNode.parent.content.locked() ? Fonts.getLargeIcon("lock") :
                    preficon;
                var color = sec.preset != null && !sec.hasBase() ? Team.derelict.color : Team.sharded.color;

                if(icon != null){
                    planets.drawPlane(sec, () -> {
                        //use white for content icons
                        Draw.color(preficon == icon && sec.info.contentIcon != null ? Color.white : color, state.uiAlpha);
                        Draw.rect(icon, 0, 0, iw, iw * icon.height / icon.width);
                    });
                }
            }
        }

        Draw.reset();

        if(hovered != null && state.uiAlpha > 0.01f){
            planets.drawPlane(hovered, () -> {
                Draw.color(hovered.isAttacked() ? Pal.remove : Color.white, Pal.accent, Mathf.absin(5f, 1f));
                Draw.alpha(state.uiAlpha);

                var icon = hovered.locked() && !canSelect(hovered) ? Fonts.getLargeIcon("lock") : hovered.isAttacked() ? Fonts.getLargeIcon("warning") : hovered.icon();

                if(icon != null){
                    Draw.rect(icon, 0, 0, iw, iw * icon.height / icon.width);
                }

                Draw.reset();
            });
        }

        Draw.reset();
    }

    boolean selectable(Planet planet){
        //TODO what if any sector is selectable?
        //TODO launch criteria - which planets can be launched to? Where should this be defined? Should planets even be selectable?
        if(mode == select) return planet == state.planet;
        if(mode == planetLaunch) return launchSector != null && planet != launchSector.planet && launchSector.planet.launchCandidates.contains(planet);
        return (planet.alwaysUnlocked && planet.isLandable()) || planet.sectors.contains(Sector::hasBase) || debugSelect;
    }

    void setup(){
        searchText = "";
        zoom = state.zoom = 1f;
        state.uiAlpha = 1f;
        ui.minimapfrag.hide();

        clearChildren();

        margin(0f);

        stack(
        new Element(){
            {
                //add listener to the background rect, so it doesn't get unnecessary touch input
                addListener(new ElementGestureListener(){
                    @Override
                    public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                        if(showing()) return;

                        if(hovered != null && selected == hovered && count == 2){
                            playSelected();
                        }

                        if(hovered != null && (canSelect(hovered) || debugSelect)){
                            selected = hovered;
                        }

                        if(selected != null){
                            updateSelected();
                        }
                    }
                });
            }

            @Override
            public void act(float delta){
                if(scene.getDialog() == PlanetDialog.this && !scene.hit(input.mouseX(), input.mouseY(), true).isDescendantOf(e -> e instanceof ScrollPane)){
                    scene.setScrollFocus(PlanetDialog.this);
                }

                super.act(delta);
            }

            @Override
            public void draw(){
                planets.render(state);
            }
        },
        //info text
        new Table(t -> {
            t.touchable = Touchable.disabled;
            t.top();
            t.label(() -> mode == select ? "@sectors.select" : "").style(Styles.outlineLabel).color(Pal.accent);
        }),
        buttons,
        //planet selection
        new Table(t -> {
            t.top().left();
            if(content.planets().count(this::selectable) > 1){
                t.table(Tex.pane, pt -> {
                    pt.margin(4f);
                    for(int i = 0; i < content.planets().size; i++){
                        Planet planet = content.planets().get(i);
                        if(selectable(planet)){
                            pt.button(planet.localizedName, Icon.icons.get(planet.icon + "Small", Icon.icons.get(planet.icon, Icon.commandRallySmall)), Styles.flatTogglet, () -> {
                                selected = null;
                                launchSector = null;
                                if(state.planet != planet){
                                    newPresets.clear();
                                    state.planet = planet;
                                    rebuildExpand();
                                }
                                settings.put("lastplanet", planet.name);
                            }).width(190).height(40).growX().update(bb -> bb.setChecked(state.planet == planet)).with(w -> w.marginLeft(10f)).get().getChildren().get(1).setColor(planet.iconColor);
                            pt.row();
                        }
                    }
                });
            }
        }),

        new Table(c -> {
            expandTable = c;
        })).grow();

        rebuildExpand();
    }

    void rebuildExpand(){
        Table c = expandTable;
        c.clear();
        c.visible(() -> !(graphics.isPortrait() && mobile));
        if(state.planet.sectors.contains(Sector::hasBase)){
            int attacked = state.planet.sectors.count(Sector::isAttacked);

            //sector notifications & search
            c.top().right();
            c.defaults().width(290f);

            c.button(bundle.get("sectorlist") +
            (attacked == 0 ? "" : "\n[red]⚠[lightgray] " + bundle.format("sectorlist.attacked", "[red]" + attacked + "[]")),
            Icon.downOpen, Styles.squareTogglet, () -> sectorsShown = !sectorsShown)
            .height(60f).checked(b -> {
                Image image = (Image)b.getCells().first().get();
                image.setDrawable(sectorsShown ? Icon.upOpen : Icon.downOpen);
                return sectorsShown;
            }).with(t -> t.left().margin(7f)).with(t -> t.getLabelCell().grow().left()).row();

            c.collapser(t -> {
                t.background(Styles.black8);

                notifs = t;
                rebuildList();
            }, false, () -> sectorsShown).padBottom(64f).row();
        }
    }

    void rebuildList(){
        if(notifs == null) return;

        notifs.clear();

        var all = state.planet.sectors.select(Sector::hasBase);
        all.sort(Structs.comps(Structs.comparingBool(s -> !s.isAttacked()), Structs.comparingInt(s -> s.save == null ? 0 : -(int)s.save.meta.timePlayed)));

        notifs.pane(p -> {
            Runnable[] readd = {null};

            p.table(s -> {
                s.image(Icon.zoom).padRight(4);
                s.field(searchText, t -> {
                    searchText = t;
                    readd[0].run();
                }).growX().height(50f);
            }).growX().row();

            Table con = p.table().growX().get();
            con.touchable = Touchable.enabled;

            readd[0] = () -> {
                con.clearChildren();
                for(Sector sec : all){
                    if(sec.hasBase() && (searchText.isEmpty() || sec.name().toLowerCase().contains(searchText.toLowerCase()))){
                        con.button(t -> {
                            t.marginRight(10f);
                            t.left();
                            t.defaults().growX();

                            t.table(head -> {
                                head.left().defaults();

                                if(sec.isAttacked()){
                                    head.image(Icon.warningSmall).update(i -> {
                                        i.color.set(Pal.accent).lerp(Pal.remove, Mathf.absin(Time.globalTime, 9f, 1f));
                                    }).padRight(4f);
                                }

                                String ic = sec.iconChar() == null ? "" : sec.iconChar() + " ";

                                head.add(ic + sec.name()).growX().wrap();
                            }).growX().row();

                            if(sec.isAttacked()){
                                addSurvivedInfo(sec, t, true);
                            }
                        }, Styles.underlineb, () -> {
                            lookAt(sec);
                            selected = sec;
                            updateSelected();
                        }).margin(8f).marginLeft(13f).marginBottom(6f).marginTop(6f).padBottom(3f).padTop(3f).growX().checked(b -> selected == sec).row();
                        //for resources: .tooltip(sec.info.resources.toString("", u -> u.emoji()))
                    }
                }

                if(con.getChildren().isEmpty()){
                    con.add("@none.found").pad(10f);
                }
            };

            readd[0].run();
        }).grow().scrollX(false);
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
            Draw.rect(Draw.wrap(buffer.getTexture()), width/2f, height/2f, width, -height);
            Draw.color();
        }
    }

    public void lookAt(Sector sector, float alpha){
        float len = state.camPos.len();
        state.camPos.slerp(Tmp.v31.set(sector.tile.v).rotate(Vec3.Y, -sector.planet.getRotation()).setLength(len), alpha);
    }

    @Override
    public void act(float delta){
        super.act(delta);

        //update lerp
        if(state.otherCamPos != null){
            state.otherCamAlpha = Mathf.lerpDelta(state.otherCamAlpha, 1f, 0.05f);
            state.camPos.set(0f, camLength, 0.1f);

            if(Mathf.equal(state.otherCamAlpha, 1f, 0.01f)){
                //TODO change zoom too
                state.camPos.set(Tmp.v31.set(state.otherCamPos).lerp(state.planet.position, state.otherCamAlpha).add(state.camPos).sub(state.planet.position));

                state.otherCamPos = null;
                //announce new sector
                newPresets.add(state.planet.sectors.get(state.planet.startSector));

            }
        }

        if(hovered != null && !mobile && state.planet.hasGrid()){
            addChild(hoverLabel);
            hoverLabel.toFront();
            hoverLabel.touchable = Touchable.disabled;
            hoverLabel.color.a = state.uiAlpha;

            Vec3 pos = planets.cam.project(Tmp.v31.set(hovered.tile.v).setLength(PlanetRenderer.outlineRad * state.planet.radius).rotate(Vec3.Y, -state.planet.getRotation()).add(state.planet.position));
            hoverLabel.setPosition(pos.x - Core.scene.marginLeft, pos.y - Core.scene.marginBottom, Align.center);

            hoverLabel.getText().setLength(0);
            if(hovered != null){
                StringBuilder tx = hoverLabel.getText();
                if(!canSelect(hovered)){
                    if(mode == planetLaunch){
                        tx.append("[gray]").append(Iconc.cancel);
                    }else{
                        tx.append("[gray]").append(Iconc.lock).append(" ").append(Core.bundle.get("locked"));
                    }
                }else{
                    tx.append("[accent][[ [white]").append(hovered.name()).append("[accent] ]");
                }
            }
            hoverLabel.invalidateHierarchy();
        }else{
            hoverLabel.remove();
        }

        if(launching && selected != null){
            lookAt(selected, 0.1f);
        }

        if(showing()){
            Sector to = newPresets.peek();

            presetShow += Time.delta;

            lookAt(to, 0.11f);
            zoom = 0.75f;

            if(presetShow >= 20f && !showed && newPresets.size > 1){
                showed = true;
                ui.announce(Iconc.lockOpen + " [accent]" + to.name(), 2f);
            }

            if(presetShow > sectorShowDuration){
                newPresets.pop();
                showed = false;
                presetShow = 0f;
            }
        }

        if(state.planet.hasGrid()){
            hovered = Core.scene.getDialog() == this ? state.planet.getSector(planets.cam.getMouseRay(), PlanetRenderer.outlineRad * state.planet.radius) : null;
        }else if(state.planet.isLandable()){
            boolean wasNull = selected == null;
            //always have the first sector selected.
            //TODO better support for multiple sectors in gridless planets?
            hovered = selected = state.planet.sectors.first();

            //autoshow
            if(wasNull){
                updateSelected();
            }
        }else{
            hovered = selected = null;
        }

        state.zoom = Mathf.lerpDelta(state.zoom, zoom, 0.4f);
        state.uiAlpha = Mathf.lerpDelta(state.uiAlpha, Mathf.num(state.zoom < 1.9f), 0.1f);
    }

    void displayItems(Table c, float scl, ObjectMap<Item, ExportStat> stats, String name){
        displayItems(c, scl, stats, name, t -> {});
    }

    void displayItems(Table c, float scl, ObjectMap<Item, ExportStat> stats, String name, Cons<Table> builder){
        Table t = new Table().left();

        int i = 0;
        for(var item : content.items()){
            var stat = stats.get(item);
            if(stat == null) continue;
            int total = (int)(stat.mean * 60 * scl);
            if(total > 1){
                t.image(item.uiIcon).padRight(3);
                t.add(UI.formatAmount(total) + " " + Core.bundle.get("unit.perminute")).color(Color.lightGray).padRight(3);
                if(++i % 3 == 0){
                    t.row();
                }
            }
        }

        if(t.getChildren().any()){
            c.defaults().left();
            c.add(name).row();
            builder.get(c);
            c.add(t).padLeft(10f).row();
        }
    }

    void showStats(Sector sector){
        BaseDialog dialog = new BaseDialog(sector.name());

        dialog.cont.pane(c -> {
            c.defaults().padBottom(5);

            if(sector.preset != null && sector.preset.description != null){
                c.add(sector.preset.displayDescription()).width(420f).wrap().left().row();
            }

            c.add(Core.bundle.get("sectors.time") + " [accent]" + sector.save.getPlayTime()).left().row();

            if(sector.info.waves && sector.hasBase()){
                c.add(Core.bundle.get("sectors.wave") + " [accent]" + (sector.info.wave + sector.info.wavesPassed)).left().row();
            }

            if(sector.isAttacked() || !sector.hasBase()){
                c.add(Core.bundle.get("sectors.threat") + " [accent]" + sector.displayThreat()).left().row();
            }

            if(sector.save != null && sector.info.resources.any()){
                c.add("@sectors.resources").left().row();
                c.table(t -> {
                    for(UnlockableContent uc : sector.info.resources){
                        if(uc == null) continue;
                        t.image(uc.uiIcon).scaling(Scaling.fit).padRight(3).size(iconSmall);
                    }
                }).padLeft(10f).left().row();
            }

            //production
            displayItems(c, sector.getProductionScale(), sector.info.production, "@sectors.production");

            //export
            displayItems(c, sector.getProductionScale(), sector.info.export, "@sectors.export", t -> {
                if(sector.info.destination != null && sector.info.destination.hasBase()){
                    String ic = sector.info.destination.iconChar();
                    t.add(Iconc.rightOpen + " " + (ic == null || ic.isEmpty() ? "" : ic + " ") + sector.info.destination.name()).padLeft(10f).row();
                }
            });

            //import
            if(sector.hasBase()){
                displayItems(c, 1f, sector.info.importStats(sector.planet), "@sectors.import", t -> {
                    sector.info.eachImport(sector.planet, other -> {
                        String ic = other.iconChar();
                        t.add(Iconc.rightOpen + " " + (ic == null || ic.isEmpty() ? "" : ic + " ") + other.name()).padLeft(10f).row();
                    });
                });
            }

            ItemSeq items = sector.items();

            //stored resources
            if(sector.hasBase() && items.total > 0){

                c.add("@sectors.stored").left().row();
                c.table(t -> {
                    t.left();

                    t.table(res -> {

                        int i = 0;
                        for(ItemStack stack : items){
                            res.image(stack.item.uiIcon).padRight(3);
                            res.add(UI.formatAmount(Math.max(stack.amount, 0))).color(Color.lightGray);
                            if(++i % 4 == 0){
                                res.row();
                            }
                        }
                    }).padLeft(10f);
                }).left().row();
            }
        });

        dialog.addCloseButton();

        if(sector.hasBase()){
            dialog.buttons.button("@sector.abandon", Icon.cancel, () -> abandonSectorConfirm(sector, dialog::hide));
        }

        dialog.show();
    }

    public void abandonSectorConfirm(Sector sector, Runnable listener){
        ui.showConfirm("@sector.abandon.confirm", () -> {
            if(listener != null) listener.run();

            if(sector.isBeingPlayed()){
                hide();
                //after dialog is hidden
                Time.runTask(7f, () -> {
                    //force game over in a more dramatic fashion
                    for(var core : player.team().cores().copy()){
                        core.kill();
                    }
                });
            }else{
                if(sector.save != null){
                    sector.save.delete();
                }
                sector.save = null;
            }
            updateSelected();
        });
    }

    void addSurvivedInfo(Sector sector, Table table, boolean wrap){
        if(!wrap){
            table.add(sector.planet.allowWaveSimulation ? Core.bundle.format("sectors.underattack", (int)(sector.info.damage * 100)) : "@sectors.underattack.nodamage").wrapLabel(wrap).row();
        }

        if(sector.planet.allowWaveSimulation && sector.info.wavesSurvived >= 0 && sector.info.wavesSurvived - sector.info.wavesPassed >= 0 && !sector.isBeingPlayed()){
            int toCapture = sector.info.attack || sector.info.winWave <= 1 ? -1 : sector.info.winWave - (sector.info.wave + sector.info.wavesPassed);
            boolean plus = (sector.info.wavesSurvived - sector.info.wavesPassed) >= SectorDamage.maxRetWave - 1;
            table.add(Core.bundle.format("sectors.survives", Math.min(sector.info.wavesSurvived - sector.info.wavesPassed, toCapture <= 0 ? 200 : toCapture) +
            (plus ? "+" : "") + (toCapture < 0 ? "" : "/" + toCapture))).wrapLabel(wrap).row();
        }
    }

    void selectSector(Sector sector){
        selected = sector;
        updateSelected();
    }

    void updateSelected(){
        Sector sector = selected;
        Table stable = sectorTop;

        if(sector == null){
            stable.clear();
            stable.visible = false;
            return;
        }
        stable.visible = true;

        float x = stable.getX(Align.center), y = stable.getY(Align.center);
        stable.clear();
        stable.background(Styles.black6);

        stable.table(title -> {
            title.add("[accent]" + sector.name()).padLeft(3);
            if(sector.preset == null){
                title.add().growX();

                title.button(Icon.pencilSmall, Styles.clearNonei, () -> {
                   ui.showTextInput("@sectors.rename", "@name", 20, sector.name(), v -> {
                       sector.setName(v);
                       updateSelected();
                       rebuildList();
                   });
                }).size(40f).padLeft(4);
            }

            var icon = sector.info.contentIcon != null ?
                new TextureRegionDrawable(sector.info.contentIcon.uiIcon) :
                Icon.icons.get(sector.info.icon + "Small");

            title.button(icon == null ? Icon.noneSmall : icon, Styles.clearNonei, iconSmall, () -> {
                new Dialog(""){{
                    closeOnBack();
                    setFillParent(true);

                    Runnable refresh = () -> {
                        sector.saveInfo();
                        hide();
                        updateSelected();
                        rebuildList();
                    };

                    cont.pane(t -> {
                        resized(true, () -> {
                            t.clearChildren();
                            t.marginRight(19f);
                            t.defaults().size(48f);

                            t.button(Icon.none, Styles.squareTogglei, () -> {
                                sector.info.icon = null;
                                sector.info.contentIcon = null;
                                refresh.run();
                            }).checked(sector.info.icon == null && sector.info.contentIcon == null);

                            int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                            int i = 1;
                            for(var key : defaultIcons){
                                var value = Icon.icons.get(key);

                                t.button(value, Styles.squareTogglei, () -> {
                                    sector.info.icon = key;
                                    sector.info.contentIcon = null;
                                    refresh.run();
                                }).checked(key.equals(sector.info.icon));

                                if(++i % cols == 0) t.row();
                            }

                            for(ContentType ctype : defaultContentIcons){
                                t.row();
                                t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                                t.row();

                                i = 0;
                                for(UnlockableContent u : content.getBy(ctype).<UnlockableContent>as()){
                                    if(!u.isHidden() && u.unlocked()){
                                        t.button(new TextureRegionDrawable(u.uiIcon), Styles.squareTogglei, iconMed, () -> {
                                            sector.info.icon = null;
                                            sector.info.contentIcon = u;
                                            refresh.run();
                                        }).checked(sector.info.contentIcon == u);

                                        if(++i % cols == 0) t.row();
                                    }
                                }
                            }
                        });
                    });
                    buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
                }}.show();
            }).size(40f).tooltip("@sector.changeicon");
        }).row();

        stable.image().color(Pal.accent).fillX().height(3f).pad(3f).row();

        boolean locked = sector.preset != null && sector.preset.locked() && !sector.hasBase() && sector.preset.techNode != null;

        if(locked){
            stable.table(r -> {
                r.add("@complete").colspan(2).left();
                r.row();
                for(Objective o : sector.preset.techNode.objectives){
                    if(o.complete()) continue;

                    r.add("> " + o.display()).color(Color.lightGray).left();
                    r.image(o.complete() ? Icon.ok : Icon.cancel, o.complete() ? Color.lightGray : Color.scarlet).padLeft(3);
                    r.row();
                }
            }).row();
        }else if(!sector.hasBase()){
            stable.add(Core.bundle.get("sectors.threat") + " [accent]" + sector.displayThreat()).row();
        }

        if(sector.isAttacked()){
            addSurvivedInfo(sector, stable, false);
        }else if(sector.hasBase() && sector.planet.allowSectorInvasion && sector.near().contains(Sector::hasEnemyBase)){
            stable.add("@sectors.vulnerable");
            stable.row();
        }else if(!sector.hasBase() && sector.hasEnemyBase()){
            stable.add("@sectors.enemybase");
            stable.row();
        }

        if(sector.save != null && sector.info.resources.any()){
            stable.table(t -> {
                t.add("@sectors.resources").padRight(4);
                for(UnlockableContent c : sector.info.resources){
                    if(c == null) continue; //apparently this is possible.
                    t.image(c.uiIcon).padRight(3).scaling(Scaling.fit).size(iconSmall);
                }
            }).padLeft(10f).fillX().row();
        }

        stable.row();

        if(sector.hasBase()){
            stable.button("@stats", Icon.info, Styles.cleart, () -> showStats(sector)).height(40f).fillX().row();
        }

        if((sector.hasBase() && mode == look) || canSelect(sector) || (sector.preset != null && sector.preset.alwaysUnlocked) || debugSelect){
            stable.button(
                mode == select ? "@sectors.select" :
                sector.isBeingPlayed() ? "@sectors.resume" :
                sector.hasBase() ? "@sectors.go" :
                locked ? "@locked" : "@sectors.launch",
                locked ? Icon.lock : Icon.play, this::playSelected).growX().height(54f).minWidth(170f).padTop(4).disabled(locked);
        }

        stable.pack();
        stable.setPosition(x, y, Align.center);

        stable.act(0f);
    }

    void playSelected(){
        if(selected == null) return;

        Sector sector = selected;

        if(sector.isBeingPlayed()){
            //already at this sector
            hide();
            return;
        }

        if(sector.preset != null && sector.preset.locked() && sector.preset.techNode != null && !sector.hasBase()){
            return;
        }

        //make sure there are no under-attack sectors (other than this one)
        for(Planet planet : content.planets()){
            if(!planet.allowWaveSimulation && !debugSelect && planet.allowWaveSimulation == sector.planet.allowWaveSimulation){
                //if there are two or more attacked sectors... something went wrong, don't show the dialog to prevent softlock
                Sector attacked = planet.sectors.find(s -> s.isAttacked() && s != sector);
                if(attacked != null &&  planet.sectors.count(s -> s.isAttacked()) < 2){
                    BaseDialog dialog = new BaseDialog("@sector.noswitch.title");
                    dialog.cont.add(bundle.format("sector.noswitch", attacked.name(), attacked.planet.localizedName)).width(400f).labelAlign(Align.center).center().wrap();
                    dialog.addCloseButton();
                    dialog.buttons.button("@sector.view", Icon.eyeSmall, () -> {
                        dialog.hide();
                        lookAt(attacked);
                        selectSector(attacked);
                    });
                    dialog.show();

                    return;
                }
            }
        }

        boolean shouldHide = true;

        //save before launch.
        if(control.saves.getCurrent() != null && Vars.state.isGame() && mode != select){
            try{
                control.saves.getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
                ui.showException("[accent]" + Core.bundle.get("savefail"), e);
            }
        }

        if(mode == look && !sector.hasBase()){
            shouldHide = false;
            Sector from = findLauncher(sector);

            if(from == null){
                //clear loadout information, so only the basic loadout gets used
                universe.clearLoadoutInfo();
                //free launch.
                control.playSector(sector);
            }else{
                CoreBlock block = sector.allowLaunchSchematics() ? (from.info.bestCoreType instanceof CoreBlock b ? b : (CoreBlock)from.planet.defaultCore) : (CoreBlock)from.planet.defaultCore;

                loadouts.show(block, from, sector, () -> {
                    var loadout = universe.getLastLoadout();
                    var schemCore = loadout.findCore();
                    from.removeItems(loadout.requirements());
                    from.removeItems(universe.getLaunchResources());

                    Events.fire(new SectorLaunchLoadoutEvent(sector, from, loadout));

                    if(settings.getBool("skipcoreanimation")){
                        //just... go there
                        control.playSector(from, sector);
                        //hide only after load screen is shown
                        Time.runTask(8f, this::hide);
                    }else{
                        //hide immediately so launch sector is visible
                        hide();

                        //allow planet dialog to finish hiding before actually launching
                        Time.runTask(5f, () -> {
                            Runnable doLaunch = () -> {
                                renderer.showLaunch(schemCore);
                                //run with less delay, as the loading animation is delayed by several frames
                                Time.runTask(coreLandDuration - 8f, () -> control.playSector(from, sector));
                            };

                            //load launchFrom sector right before launching so animation is correct
                            if(!from.isBeingPlayed()){
                                //run *after* the loading animation is done
                                Time.runTask(9f, doLaunch);
                                control.playSector(from);
                            }else{
                                doLaunch.run();
                            }
                        });
                    }
                });
            }
        }else if(mode == select){
            listener.get(sector);
        }else if(mode == planetLaunch){ //TODO make sure it doesn't have a base already.

            //TODO animation
            //schematic selection and cost handled by listener
            listener.get(sector);
            //unlock right before launch
            sector.planet.unlockedOnLand.each(UnlockableContent::unlock);
            control.playSector(sector);
        }else{
            //sector should have base here
            control.playSector(sector);
        }

        if(shouldHide) hide();
    }

    public enum Mode{
        /** Look around for existing sectors. Can only deploy. */
        look,
        /** Select a sector for some purpose. */
        select,
        /** Launch between planets. */
        planetLaunch
    }
}
