package mindustry.world.blocks.campaign;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
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
    public @Load(value = "@-launch-arrow", fallback = "launch-arrow") TextureRegion arrowRegion;
    public @Load("select-arrow-small") TextureRegion selectArrowRegion;

    /** Core block that is launched. Should match the starting core of the planet being launched to. */
    public Block launchBlock = Blocks.coreNucleus;
    public float powerBufferRequirement;
    /** Override for planets that this block can launch to. If null, the planet's launch candidates are used. */
    public @Nullable Seq<Planet> launchCandidates;

    //TODO: launching needs audio!

    public Music launchMusic = Musics.coreLaunch;
    public float launchDuration = 120f;
    public float chargeDuration = 220f;
    public float buildDuration = 120f;
    public Interp landZoomInterp = Interp.pow4In, chargeZoomInterp = Interp.pow4In;
    public float landZoomFrom = 0.02f, landZoomTo = 4f, chargeZoomTo = 5f;

    public int chargeRings = 4;
    public float ringRadBase = 60f, ringRadSpacing = 25f, ringRadPow = 1.6f, ringStroke = 3f, ringSpeedup = 1.4f, chargeRingMerge = 2f, ringArrowRad = 3f;
    public float ringHandleTilt = 0.8f, ringHandleLen = 30f;
    public Color ringColor = Pal.accent;

    public int launchLightning = 20;
    public Color lightningColor = Pal.accent;
    public float lightningDamage = 40;
    public float lightningOffset = 24f;
    public int lightningLengthMin = 5, lightningLengthMax = 25;
    public double lightningLaunchChance = 0.8;

    protected int[] capacities = {};

    public Accelerator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 8000;
        configurable = true;
        emitLight = true;
        lightRadius = 70f;
        lightColor = Pal.accent;
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
        public float time, launchHeat;
        public boolean launching;
        public float launchTime;

        protected float cloudSeed;

        @Override
        public void updateTile(){
            super.updateTile();
            heat = Mathf.lerpDelta(heat, launching ? 1f : efficiency, 0.05f);
            statusLerp = Mathf.lerpDelta(statusLerp, power.status, 0.05f);

            if(!launching){
                time += Time.delta * efficiency;
            }else{
                time = Mathf.slerpDelta(time, 0f, 0.4f);
            }

            launchHeat = Mathf.lerpDelta(launchHeat, launching ? 1f : 0f, 0.1f);

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
                if(launching){
                    Draw.reset();

                    Draw.blend(Blending.additive);
                    Fill.light(x, y, 15, launchBlock.size * tilesize * 1f, Tmp.c2.set(Pal.accent).a(launchTime / chargeDuration), Tmp.c1.set(Pal.accent).a(0f));
                    Draw.blend();

                    Draw.rect(launchBlock.fullIcon, x, y);

                    Draw.z(Layer.bullet);
                    Draw.mixcol(Pal.accent, Mathf.clamp(launchTime / chargeDuration));
                    Draw.color(1f, 1f, 1f, Interp.pow2In.apply(Mathf.clamp(launchTime / chargeDuration * 0.7f)));
                    Draw.rect(launchBlock.fullIcon, x, y);
                    Draw.reset();
                }else{
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
                }

                Draw.reset();
            }

            if(heat < 0.0001f) return;

            float rad = size * tilesize / 2f * 0.74f * Mathf.lerp(1f, 1.3f, launchHeat);
            float scl = 2f;

            Draw.z(Layer.bullet - 0.0001f);
            Lines.stroke(1.75f * heat, Pal.accent);
            Lines.square(x, y, rad * 1.22f, Mathf.lerp(45f, 0f, launchHeat));

            //TODO: lock time when launching

            Lines.stroke(3f * heat, Pal.accent);
            Lines.square(x, y, rad * Mathf.lerp(1f, 1.3f, launchHeat), 45f + time / scl);
            Lines.square(x, y, rad * Mathf.lerp(1f, 1.8f, launchHeat), Mathf.lerp(45f, 0f, launchHeat) - time / scl);

            Draw.color(team.color);
            Draw.alpha(Mathf.clamp(heat * 3f));

            for(int i = 0; i < 4; i++){
                float rot = i*90f + 45f + (-time/3f)%360f;
                float length = 26f * heat * Mathf.lerp(1f, 1.5f, launchHeat);
                Draw.rect(arrowRegion, x + Angles.trnsx(rot, length), y + Angles.trnsy(rot, length), rot + 180f);
            }

            Draw.reset();
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius, lightColor, launchHeat);
        }

        public boolean canLaunch(){
            return isValid() && !net.client() && state.isCampaign() && efficiency > 0f && power.graph.getBatteryStored() >= powerBufferRequirement-0.00001f && progress >= 1f && !launching;
        }

        @Override
        public Cursor getCursor(){
            return canLaunch() ? SystemCursor.hand : super.getCursor();
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            if(power.graph.getBatteryStored() < powerBufferRequirement && !launching){
                drawPlaceText(Core.bundle.get("bar.nobatterypower"), tile.x, tile.y, false);
            }
        }

        @Override
        public void buildConfiguration(Table table){
            deselect();

            if(!canLaunch()) return;

            ui.planet.showPlanetLaunch(state.rules.sector, launchCandidates == null ? state.rules.sector.planet.launchCandidates : launchCandidates, sector -> {
                if(canLaunch()){
                    consume();
                    power.graph.useBatteries(powerBufferRequirement);
                    progress = 0f;

                    renderer.showLaunch(this);

                    Time.runTask(launchDuration() - 6f, () -> {
                        //unlock right before launch
                        launching = false;
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
        public float launchDuration(){
            return launchDuration + chargeDuration;
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
            if(!launching) return;

            this.launching = true;
            Fx.coreLaunchConstruct.at(x, y, launchBlock.size);

            cloudSeed = Mathf.random(1f);
            float margin = 30f;

            Image image = new Image();
            image.color.a = 0f;
            image.touchable = Touchable.disabled;
            image.setFillParent(true);
            image.actions(Actions.delay((launchDuration() - margin) / 60f), Actions.fadeIn(margin / 60f, Interp.pow2In), Actions.delay(6f / 60f), Actions.remove());
            image.update(() -> {
                image.toFront();
                ui.loadfrag.toFront();
                if(state.isMenu()){
                    image.remove();
                }
            });
            Core.scene.add(image);

            Time.run(chargeDuration, () -> {
                Fx.coreLaunchConstruct.at(x, y, launchBlock.size);
                Fx.launchAccelerator.at(x, y);
                Effect.shake(10f, 14f, this);

                for(int i = 0; i < launchLightning; i++){
                    float a = Mathf.random(360f);
                    Lightning.create(team, lightningColor, lightningDamage, x + Angles.trnsx(a, lightningOffset), y + Angles.trnsy(a, lightningOffset), a, Mathf.random(lightningLengthMin, lightningLengthMax));
                }

                float spacing = 12f;
                for(int i = 0; i < 13; i++){
                    int fi = i;
                    Time.run(i * 1.1f, () -> {
                        float radius = block.size/2f + 1 + spacing * fi;
                        int rays = Mathf.ceil(radius * Mathf.PI * 2f / 6f);
                        for(int r = 0; r < rays; r++){
                            if(Mathf.chance(0.7f - fi  * 0.02f)){
                                float angle = r * 360f / (float)rays;
                                float ox = Angles.trnsx(angle, radius), oy = Angles.trnsy(angle, radius);
                                Tile t = world.tileWorld(x + ox, y + oy);
                                if(t != null){
                                    Fx.coreLandDust.at(t.worldx(), t.worldy(), angle + Mathf.range(30f), Tmp.c1.set(t.floor().mapColor).mul(1.7f + Mathf.range(0.15f)));
                                }
                            }
                        }
                    });
                }


            });
        }

        @Override
        public void endLaunch(){
            launching = false;
            launchTime = 0f;
        }

        @Override
        public float zoomLaunch(){
            float rawTime = launchDuration() - renderer.getLandTime();
            float shake = rawTime < chargeDuration ? Interp.pow10In.apply(Mathf.clamp(rawTime/chargeDuration)) : 0f;

            Core.camera.position.set(x, y).add(Tmp.v1.setToRandomDirection().scl(shake * 2f));

            if(rawTime < chargeDuration){
                float fin = rawTime / chargeDuration;

                return chargeZoomInterp.apply(Scl.scl(landZoomTo), Scl.scl(chargeZoomTo), fin);
            }else{
                float rawFin = renderer.getLandTimeIn();
                float fin = 1f - Mathf.clamp((1f - rawFin) - (chargeDuration / (launchDuration + chargeDuration))) / (1f - (chargeDuration / (launchDuration + chargeDuration)));

                return landZoomInterp.apply(Scl.scl(landZoomFrom), Scl.scl(landZoomTo), fin);
            }
        }

        @Override
        public void updateLaunch(){
            float in = renderer.getLandTimeIn() * launchDuration();
            launchTime = launchDuration() - in;
            float tsize = Mathf.sample(CoreBlock.thrusterSizes, (in + 35f) / launchDuration());

            float rawFin = renderer.getLandTimeIn();
            float chargeFin = 1f - Mathf.clamp((1f - rawFin) / (chargeDuration / (launchDuration + chargeDuration)));
            float chargeFout = 1f - chargeFin;

            if(in > launchDuration){
                if(Mathf.chanceDelta(lightningLaunchChance * Interp.pow3In.apply(chargeFout))){
                    float a = Mathf.random(360f);
                    Lightning.create(team, lightningColor, lightningDamage, x + Angles.trnsx(a, lightningOffset), y + Angles.trnsy(a, lightningOffset), a, Mathf.random(lightningLengthMin, lightningLengthMax));
                }
            }
        }

        @Override
        public void drawLaunch(){
            var clouds = Core.assets.get("sprites/clouds.png", Texture.class);

            float rawFin = renderer.getLandTimeIn();
            float rawTime = launchDuration() - renderer.getLandTime();
            float fin = 1f - Mathf.clamp((1f - rawFin) - (chargeDuration / (launchDuration + chargeDuration))) / (1f - (chargeDuration / (launchDuration + chargeDuration)));

            float chargeFin = 1f - Mathf.clamp((1f - rawFin) / (chargeDuration / (launchDuration + chargeDuration)));
            float chargeFout = 1f - chargeFin;

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

            if(rawTime >= chargeDuration){
                drawLanding(fin, x, y);
            }

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

        @Override
        public void drawLaunchGlobalZ(){
            float rawFin = renderer.getLandTimeIn();

            float chargeFin = 1f - Mathf.clamp((1f - rawFin) / (chargeDuration / (launchDuration + chargeDuration)));
            float fin = 1f - Mathf.clamp((1f - rawFin) - (chargeDuration / (launchDuration + chargeDuration))) / (1f - (chargeDuration / (launchDuration + chargeDuration)));
            float fout = 1f - fin;
            float chargeFout = 1f - chargeFin;

            //fade out rings during launch.
            chargeFout = Mathf.clamp(chargeFout - fout * 2f);

            float
            spacing = 1f / (chargeRings + chargeRingMerge);

            for(int i = 0; i < chargeRings; i++){
                float cfin = Mathf.clamp((chargeFout*ringSpeedup - spacing * i) / (spacing * (1f + chargeRingMerge)));
                if(cfin > 0){
                    drawRing(ringRadBase + ringRadSpacing * Mathf.pow(i, ringRadPow), cfin);
                }
            }
        }

        protected void drawRing(float radius, float fin){
            Draw.z(Layer.effect);

            float fout = 1f - fin;
            float rotate = Interp.pow4In.apply(fout) * 90f;
            float rad = radius + 20f * Interp.pow4In.apply(fout);

            Lines.stroke(ringStroke * fin, ringColor);

            Draw.color(Pal.command, ringColor, fin);

            //handles
            for(int i = 0; i < 4; i++){
                float angle = i * 90f + 45f + rotate;
                Lines.beginLine();
                Lines.linePoint(Tmp.v1.trns(angle - ringHandleLen, rad * ringHandleTilt).add(x, y));
                Lines.linePoint(Tmp.v2.trns(angle, rad).add(x, y));
                Lines.linePoint(Tmp.v3.trns(angle + ringHandleLen, rad * ringHandleTilt).add(x, y));
                Lines.endLine(false);

            }

            Draw.scl(fin);

            //selection triangles
            for(int i = 0; i < 4; i++){
                float angle = i * 90f + rotate;


                Draw.rect(selectArrowRegion, x + Angles.trnsx(angle, rad), y + Angles.trnsy(angle, rad), angle + 180f + 45f);

                //shape variant:
                //Lines.poly(x + Angles.trnsx(angle, rad), y + Angles.trnsy(angle, rad), 3, ringArrowRad * fin, angle + 180f);
            }

            Draw.scl();

        }

        protected void drawLanding(float fin, float x, float y){
            float rawTime = launchDuration() - renderer.getLandTime();
            float fout = 1f - fin;

            float scl = rawTime < chargeDuration ? 1f : Scl.scl(4f) / renderer.getDisplayScale();
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
    }
}
