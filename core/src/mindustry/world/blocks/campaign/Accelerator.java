package mindustry.world.blocks.campaign;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class Accelerator extends Block{
    public @Load("launch-arrow") TextureRegion arrowRegion;

    /** Core block that is launched. Should match the starting core of the planet being launched to. */
    public Block launchBlock = Blocks.coreNucleus;
    public float powerBufferRequirement;
    /** Override for planets that this block can launch to. If null, the planet's launch candidates are used. */
    public @Nullable Seq<Planet> launchCandidates;

    public Music launchMusic = Musics.coreLaunch;
    public float launchDuration = 160f;
    public float buildDuration = 120f;

    protected int[] capacities = {};

    public Accelerator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 8000;
        configurable = true;
    }

    @Override
    public void init(){
        itemCapacity = 0;
        capacities = new int[content.items().size];
        for(ItemStack stack : launchBlock.requirements){
            capacities[stack.item.id] = stack.amount;
            itemCapacity += stack.amount;
        }
        consumeItems(launchBlock.requirements);
        super.init();
    }

    @Override
    public void setBars(){
        super.setBars();

        if(powerBufferRequirement > 0f){
            addBar("powerBufferRequirement", b -> new Bar(
            () -> Core.bundle.format("bar.powerbuffer",UI.formatAmount((long)b.power.graph.getBatteryStored()),  UI.formatAmount((long)powerBufferRequirement)),
            () -> Pal.powerBar,
            () -> b.power.graph.getBatteryStored() / powerBufferRequirement
            ));
        }
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class AcceleratorBuild extends Building implements LaunchAnimator{
        public float heat, statusLerp;
        public float progress;
        public float time;

        protected float cloudSeed;

        @Override
        public void updateTile(){
            super.updateTile();
            heat = Mathf.lerpDelta(heat, efficiency, 0.05f);
            statusLerp = Mathf.lerpDelta(statusLerp, power.status, 0.05f);

            time += Time.delta * efficiency;

            if(efficiency >= 0f){
                progress += Time.delta * efficiency / buildDuration;
                progress = Math.min(progress, 1f);
            }
        }

        @Override
        public float progress(){
            return progress;
        }

        @Override
        public void draw(){
            super.draw();

            for(int l = 0; l < 4; l++){
                float length = 7f + l * 5f;
                Draw.color(Tmp.c1.set(Pal.darkMetal).lerp(team.color, statusLerp), Pal.darkMetal, Mathf.absin(Time.time + l*50f, 10f, 1f));

                for(int i = 0; i < 4; i++){
                    float rot = i*90f + 45f;
                    Draw.rect(arrowRegion, x + Angles.trnsx(rot, length), y + Angles.trnsy(rot, length), rot + 180f);
                }
            }

            {
                Drawf.shadow(x, y, launchBlock.size * tilesize * 2f, progress);
                Draw.draw(Layer.blockBuilding, () -> {
                    Draw.color(Pal.accent, heat);

                    for(TextureRegion region : launchBlock.getGeneratedIcons()){
                        Shaders.blockbuild.region = region;
                        Shaders.blockbuild.time = time;
                        Shaders.blockbuild.progress = progress;

                        Draw.rect(region, x, y);
                        Draw.flush();
                    }

                    Draw.color();
                });

                //TODO: build line?
                //Draw.z(Layer.blockBuilding + 1);
                //Draw.color(Pal.accent, heat);

                //Lines.lineAngleCenter(x + Mathf.sin(time, 10f, Vars.tilesize / 2f * recipe.size + 1f), y, 90, recipe.size * Vars.tilesize + 1f);

                Draw.reset();
            }

            if(heat < 0.0001f) return;

            float rad = size * tilesize / 2f * 0.74f;
            float scl = 2f;

            Draw.z(Layer.bullet - 0.0001f);
            Lines.stroke(1.75f * heat, Pal.accent);
            Lines.square(x, y, rad * 1.22f, 45f);

            Lines.stroke(3f * heat, Pal.accent);
            Lines.square(x, y, rad, Time.time / scl);
            Lines.square(x, y, rad, -Time.time / scl);

            Draw.color(team.color);
            Draw.alpha(Mathf.clamp(heat * 3f));

            for(int i = 0; i < 4; i++){
                float rot = i*90f + 45f + (-Time.time /3f)%360f;
                float length = 26f * heat;
                Draw.rect(arrowRegion, x + Angles.trnsx(rot, length), y + Angles.trnsy(rot, length), rot + 180f);
            }

            Draw.reset();
        }

        public boolean canLaunch(){
            return isValid() && state.isCampaign() && efficiency > 0f && power.graph.getBatteryStored() >= powerBufferRequirement-0.00001f && progress >= 1f;
        }

        @Override
        public Cursor getCursor(){
            return canLaunch() ? SystemCursor.hand : super.getCursor();
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            if(power.graph.getBatteryStored() < powerBufferRequirement){
                drawPlaceText(Core.bundle.get("bar.nobatterypower"), tile.x, tile.y, false);
            }
        }

        @Override
        public void buildConfiguration(Table table){
            deselect();

            if(!canLaunch()) return;

            ui.planet.showPlanetLaunch(state.rules.sector, launchCandidates == null ? state.rules.sector.planet.launchCandidates : launchCandidates, sector -> {
                if(canLaunch()){
                    //TODO: animation!

                    consume();
                    power.graph.useBatteries(powerBufferRequirement);
                    progress = 0f;

                    var core = team.core();

                    renderer.showLaunch(this);

                    Time.runTask(core.landDuration() - 8f, () -> {
                        //unlock right before launch
                        sector.planet.unlockedOnLand.each(UnlockableContent::unlock);

                        universe.clearLoadoutInfo();
                        universe.updateLoadout((CoreBlock)launchBlock);

                        control.playSector(sector);
                    });
                }
            });

            Events.fire(Trigger.acceleratorUse);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return capacities[item.id];
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                progress = read.f();
            }
        }

        //launch animator stuff:

        @Override
        public float zoomLaunching(){
            CoreBlock core = (CoreBlock)launchBlock;
            Core.camera.position.set(this);
            return core.landZoomInterp.apply(Scl.scl(core.landZoomFrom), Scl.scl(core.landZoomTo), renderer.getLandTimeIn());
        }

        @Override
        public void updateLaunching(){
            float in = renderer.getLandTimeIn() * landDuration();
            float tsize = Mathf.sample(CoreBlock.thrusterSizes, (in + 35f) / landDuration());

            renderer.setLandPTimer(renderer.getLandPTimer() + tsize * Time.delta);
            if(renderer.getLandTime() >= 1f){
                tile.getLinkedTiles(t -> {
                    if(Mathf.chance(0.4f)){
                        Fx.coreLandDust.at(t.worldx(), t.worldy(), angleTo(t.worldx(), t.worldy()) + Mathf.range(30f), Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                    }
                });

                renderer.setLandPTimer(0f);
            }
        }

        @Override
        public float landDuration(){
            return launchDuration;
        }

        @Override
        public Music landMusic(){
            //unused
            return launchMusic;
        }

        @Override
        public Music launchMusic(){
            return launchMusic;
        }

        @Override
        public void beginLaunch(boolean launching){
            cloudSeed = Mathf.random(1f);
            if(launching){
                Fx.coreLaunchConstruct.at(x, y, launchBlock.size);
            }

            if(!headless){
                // Add fade-in and fade-out foreground when landing or launching.
                if(renderer.isLaunching()){
                    float margin = 30f;

                    Image image = new Image();
                    image.color.a = 0f;
                    image.touchable = Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(Actions.delay((landDuration() - margin) / 60f), Actions.fadeIn(margin / 60f, Interp.pow2In), Actions.delay(6f / 60f), Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()){
                            image.remove();
                        }
                    });
                    Core.scene.add(image);
                }else{
                    Image image = new Image();
                    image.color.a = 1f;
                    image.touchable = Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(Actions.fadeOut(35f / 60f), Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()){
                            image.remove();
                        }
                    });
                    Core.scene.add(image);

                    Time.run(landDuration(), () -> {
                        CoreBlock core = (CoreBlock)launchBlock;
                        core.launchEffect.at(this);
                        Effect.shake(5f, 5f, this);

                        if(state.isCampaign() && Vars.showSectorLandInfo && (state.rules.sector.preset == null || state.rules.sector.preset.showSectorLandInfo)){
                            ui.announce("[accent]" + state.rules.sector.name() + "\n" +
                            (state.rules.sector.info.resources.any() ? "[lightgray]" + Core.bundle.get("sectors.resources") + "[white] " +
                            state.rules.sector.info.resources.toString(" ", UnlockableContent::emoji) : ""), 5);
                        }
                    });
                }
            }
        }

        @Override
        public void endLaunch(){}

        @Override
        public void drawLanding(){
            var clouds = Core.assets.get("sprites/clouds.png", Texture.class);

            float fin = renderer.getLandTimeIn();
            float cameraScl = renderer.getDisplayScale();

            float fout = 1f - fin;
            float scl = Scl.scl(4f) / cameraScl;
            float pfin = Interp.pow3Out.apply(fin), pf = Interp.pow2In.apply(fout);

            //draw particles
            Draw.color(Pal.lightTrail);
            Angles.randLenVectors(1, pfin, 100, 800f * scl * pfin, (ax, ay, ffin, ffout) -> {
                Lines.stroke(scl * ffin * pf * 3f);
                Lines.lineAngle(x + ax, y + ay, Mathf.angle(ax, ay), (ffin * 20 + 1f) * scl);
            });
            Draw.color();

            drawLanding(x, y);

            Draw.color();
            Draw.mixcol(Color.white, Interp.pow5In.apply(fout));

            //accent tint indicating that the core was just constructed
            if(renderer.isLaunching()){
                float f = Mathf.clamp(1f - fout * 12f);
                if(f > 0.001f){
                    Draw.mixcol(Pal.accent, f);
                }
            }

            //draw clouds
            if(state.rules.cloudColor.a > 0.0001f){
                float scaling = CoreBlock.cloudScaling;
                float sscl = Math.max(1f + Mathf.clamp(fin + CoreBlock.cfinOffset) * CoreBlock.cfinScl, 0f) * cameraScl;

                Tmp.tr1.set(clouds);
                Tmp.tr1.set(
                (Core.camera.position.x - Core.camera.width/2f * sscl) / scaling,
                (Core.camera.position.y - Core.camera.height/2f * sscl) / scaling,
                (Core.camera.position.x + Core.camera.width/2f * sscl) / scaling,
                (Core.camera.position.y + Core.camera.height/2f * sscl) / scaling);

                Tmp.tr1.scroll(10f * cloudSeed, 10f * cloudSeed);

                Draw.alpha(Mathf.sample(CoreBlock.cloudAlphas, fin + CoreBlock.calphaFinOffset) * CoreBlock.cloudAlpha);
                Draw.mixcol(state.rules.cloudColor, state.rules.cloudColor.a);
                Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height);
                Draw.reset();
            }
        }

        public void drawLanding(float x, float y){
            float fin = renderer.getLandTimeIn();
            float fout = 1f - fin;

            float scl = Scl.scl(4f) / renderer.getDisplayScale();
            float shake = 0f;
            float s = launchBlock.region.width * launchBlock.region.scl() * scl * 3.6f * Interp.pow2Out.apply(fout);
            float rotation = Interp.pow2In.apply(fout) * 135f;
            x += Mathf.range(shake);
            y += Mathf.range(shake);
            float thrustOpen = 0.25f;
            float thrusterFrame = fin >= thrustOpen ? 1f : fin / thrustOpen;
            float thrusterSize = Mathf.sample(CoreBlock.thrusterSizes, fin);

            //when launching, thrusters stay out the entire time.
            if(renderer.isLaunching()){
                Interp i = Interp.pow2Out;
                thrusterFrame = i.apply(Mathf.clamp(fout*13f));
                thrusterSize = i.apply(Mathf.clamp(fout*9f));
            }

            Draw.color(Pal.lightTrail);
            //TODO spikier heat
            Draw.rect("circle-shadow", x, y, s, s);

            Draw.scl(scl);

            //draw thruster flame
            float strength = (1f + (launchBlock.size - 3)/2.5f) * scl * thrusterSize * (0.95f + Mathf.absin(2f, 0.1f));
            float offset = (launchBlock.size - 3) * 3f * scl;

            for(int i = 0; i < 4; i++){
                Tmp.v1.trns(i * 90 + rotation, 1f);

                Tmp.v1.setLength((launchBlock.size * tilesize/2f + 1f)*scl + strength*2f + offset);
                Draw.color(team.color);
                Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 6f * strength);

                Tmp.v1.setLength((launchBlock.size * tilesize/2f + 1f)*scl + strength*0.5f + offset);
                Draw.color(Color.white);
                Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 3.5f * strength);
            }

            drawLandingThrusters(x, y, rotation, thrusterFrame);

            Drawf.spinSprite(launchBlock.region, x, y, rotation);

            Draw.alpha(Interp.pow4In.apply(thrusterFrame));
            drawLandingThrusters(x, y, rotation, thrusterFrame);
            Draw.alpha(1f);

            if(launchBlock.teamRegions[team.id] == launchBlock.teamRegion) Draw.color(team.color);

            Drawf.spinSprite(launchBlock.teamRegions[team.id], x, y, rotation);

            Draw.color();
            Draw.scl();
            Draw.reset();
        }

        protected void drawLandingThrusters(float x, float y, float rotation, float frame){
            CoreBlock core = (CoreBlock)launchBlock;
            float length = core.thrusterLength * (frame - 1f) - 1f/4f;
            float alpha = Draw.getColorAlpha();

            //two passes for consistent lighting
            for(int j = 0; j < 2; j++){
                for(int i = 0; i < 4; i++){
                    var reg = i >= 2 ? core.thruster2 : core.thruster1;
                    float rot = (i * 90) + rotation % 90f;
                    Tmp.v1.trns(rot, length * Draw.xscl);

                    //second pass applies extra layer of shading
                    if(j == 1){
                        Tmp.v1.rotate(-90f);
                        Draw.alpha((rotation % 90f) / 90f * alpha);
                        rot -= 90f;
                        Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                    }else{
                        Draw.alpha(alpha);
                        Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                    }
                }
            }
            Draw.alpha(1f);
        }

        public void drawThrusters(float frame){
            CoreBlock core = (CoreBlock)launchBlock;
            float length = core.thrusterLength * (frame - 1f) - 1f/4f;
            for(int i = 0; i < 4; i++){
                var reg = i >= 2 ? core.thruster2 : core.thruster1;
                float dx = Geometry.d4x[i] * length, dy = Geometry.d4y[i] * length;
                Draw.rect(reg, x + dx, y + dy, i * 90);
            }
        }
    }
}
