package mindustry.entities.comp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

@EntityDef(value = {Playerc.class}, serialize = false)
@Component(base = true)
abstract class PlayerComp implements UnitController, Entityc, Syncc, Timerc, Drawc{
    static final float deathDelay = 60f;

    @Import float x, y;

    @ReadOnly Unit unit = Nulls.unit;
    transient @Nullable NetConnection con;
    @ReadOnly Team team = Team.sharded;
    @SyncLocal boolean typing, shooting, boosting;
    @SyncLocal float mouseX, mouseY;
    boolean admin;
    String name = "frog";
    Color color = new Color();
    transient String locale = "en";
    transient float deathTimer;
    transient String lastText = "";
    transient float textFadeTime;

    transient private Unit lastReadUnit = Nulls.unit;
    transient private int wrongReadUnits;
    transient @Nullable Unit justSwitchFrom, justSwitchTo;

    public boolean isBuilder(){
        return unit.canBuild();
    }

    public @Nullable CoreBuild closestCore(){
        return state.teams.closestCore(x, y, team);
    }

    public @Nullable CoreBuild core(){
        return team.core();
    }

    /** @return largest/closest core, with largest cores getting priority */
    @Nullable
    public CoreBuild bestCore(){
        return team.cores().min(Structs.comps(Structs.comparingInt(c -> -c.block.size), Structs.comparingFloat(c -> c.dst(x, y))));
    }

    public TextureRegion icon(){
        //display default icon for dead players
        if(dead()) return core() == null ? UnitTypes.alpha.fullIcon : ((CoreBlock)bestCore().block).unitType.fullIcon;

        return unit.icon();
    }

    public boolean displayAmmo(){
        return unit instanceof BlockUnitc || state.rules.unitAmmo;
    }

    public void reset(){
        team = state.rules.defaultTeam;
        admin = typing = false;
        textFadeTime = 0f;
        x = y = 0f;
        if(!dead()){
            unit.resetController();
            unit = Nulls.unit;
        }
    }

    @Override
    public boolean isValidController(){
        return isAdded();
    }

    @Override
    public boolean isLogicControllable(){
        return false;
    }

    @Replace
    public float clipSize(){
        return unit.isNull() ? 20 : unit.type.hitSize * 2f;
    }

    @Override
    public void afterSync(){
        //fix rubberbanding:
        //when the player recs a unit that they JUST transitioned away from, use the new unit instead
        //reason: we know the server is lying here, essentially skip the unit snapshot because we know the client's information is more recent
        if(isLocal() && unit == justSwitchFrom && justSwitchFrom != null && justSwitchTo != null){
            unit = justSwitchTo;
            //if several snapshots have passed and this unit is still incorrect, something's wrong
            if(++wrongReadUnits >= 2){
                justSwitchFrom = null;
                wrongReadUnits = 0;
            }
        }else{
            justSwitchFrom = null;
            justSwitchTo = null;
            wrongReadUnits = 0;
        }

        //simulate a unit change after sync
        Unit set = unit;
        unit = lastReadUnit;
        unit(set);
        lastReadUnit = unit;

        unit.aim(mouseX, mouseY);
        //this is only necessary when the thing being controlled isn't synced
        unit.controlWeapons(shooting, shooting);
        //extra precaution, necessary for non-synced things
        unit.controller(this);
    }

    @Override
    public void update(){
        if(!unit.isValid()){
            clearUnit();
        }

        CoreBuild core;

        if(!dead()){
            set(unit);
            unit.team(team);
            deathTimer = 0;

            //update some basic state to sync things
            if(unit.type.canBoost){
                unit.elevation = Mathf.approachDelta(unit.elevation, unit.onSolid() || boosting || (unit.isFlying() && !unit.canLand()) ? 1f : 0f, unit.type.riseSpeed);
            }
        }else if((core = bestCore()) != null){
            //have a small delay before death to prevent the camera from jumping around too quickly
            //(this is not for balance, it just looks better this way)
            deathTimer += Time.delta;
            if(deathTimer >= deathDelay){
                //request spawn - this happens serverside only
                core.requestSpawn(self());
                deathTimer = 0;
            }
        }

        textFadeTime -= Time.delta / (60 * 5);

    }

    public void checkSpawn(){
        CoreBuild core = bestCore();
        if(core != null){
            core.requestSpawn(self());
        }
    }

    @Override
    public void remove(){
        //clear unit upon removal
        if(!unit.isNull()){
            clearUnit();
        }
    }

    public void team(Team team){
        this.team = team;
        unit.team(team);
    }

    public void clearUnit(){
        unit(Nulls.unit);
    }

    public Unit unit(){
        return unit;
    }

    public void unit(Unit unit){
        //refuse to switch when the unit was just transitioned from
        if(isLocal() && unit == justSwitchFrom && justSwitchFrom != null && justSwitchTo != null){
            return;
        }

        if(unit == null) throw new IllegalArgumentException("Unit cannot be null. Use clearUnit() instead.");
        if(this.unit == unit) return;

        if(this.unit != Nulls.unit){
            //un-control the old unit
            this.unit.resetController();
        }
        this.unit = unit;
        if(unit != Nulls.unit){
            unit.team(team);
            unit.controller(this);

            //this player just became remote, snap the interpolation so it doesn't go wild
            if(unit.isRemote()){
                unit.snapInterpolation();
            }

            //reset selected block when switching units
            if(!headless && isLocal()){
                control.input.block = null;
            }
        }

        Events.fire(new UnitChangeEvent(self(), unit));
    }

    boolean dead(){
        return unit.isNull() || !unit.isValid();
    }

    String ip(){
        return con == null ? "localhost" : con.address;
    }

    String uuid(){
        return con == null ? "[LOCAL]" : con.uuid;
    }

    String usid(){
        return con == null ? "[LOCAL]" : con.usid;
    }

    void kick(KickReason reason){
        con.kick(reason);
    }

    void kick(KickReason reason, long duration){
        con.kick(reason, duration);
    }

    void kick(String reason){
        con.kick(reason);
    }

    void kick(String reason, long duration){
        con.kick(reason, duration);
    }

    @Override
    public void draw(){
        if(unit != null && unit.inFogTo(Vars.player.team())) return;

        Draw.z(Layer.playerName);
        float z = Drawf.text();

        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        final float nameHeight = 11;
        final float textHeight = 15;

        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f));
        layout.setText(font, name);

        if(!isLocal()){
            Draw.color(0f, 0f, 0f, 0.3f);
            Fill.rect(unit.x, unit.y + nameHeight - layout.height / 2, layout.width + 2, layout.height + 3);
            Draw.color();
            font.setColor(color);
            font.draw(name, unit.x, unit.y + nameHeight, 0, Align.center, false);

            if(admin){
                float s = 3f;
                Draw.color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f);
                Draw.rect(Icon.adminSmall.getRegion(), unit.x + layout.width / 2f + 2 + 1, unit.y + nameHeight - 1.5f, s, s);
                Draw.color(color);
                Draw.rect(Icon.adminSmall.getRegion(), unit.x + layout.width / 2f + 2 + 1, unit.y + nameHeight - 1f, s, s);
            }
        }

        if(Core.settings.getBool("playerchat") && ((textFadeTime > 0 && lastText != null) || typing)){
            String text = textFadeTime <= 0 || lastText == null ? "[lightgray]" + Strings.animated(Time.time, 4, 15f, ".") : lastText;
            float width = 100f;
            float visualFadeTime = 1f - Mathf.curve(1f - textFadeTime, 0.9f);
            font.setColor(1f, 1f, 1f, textFadeTime <= 0 || lastText == null ? 1f : visualFadeTime);

            layout.setText(font, text, Color.white, width, Align.bottom, true);

            Draw.color(0f, 0f, 0f, 0.3f * (textFadeTime <= 0 || lastText == null ? 1f : visualFadeTime));
            Fill.rect(unit.x, unit.y + textHeight + layout.height - layout.height / 2f, layout.width + 2, layout.height + 3);
            font.draw(text, unit.x - width / 2f, unit.y + textHeight + layout.height, width, Align.center, true);
        }

        Draw.reset();
        Pools.free(layout);
        font.getData().setScale(1f);
        font.setColor(Color.white);
        font.setUseIntegerPositions(ints);

        Draw.z(z);
    }

    /** @return name with a markup color prefix */
    String coloredName(){
        return  "[#" + color.toString().toUpperCase() + "]" + name;
    }

    String plainName(){
        return Strings.stripColors(name);
    }

    void sendMessage(String text){
        if(isLocal()){
            if(ui != null){
                ui.chatfrag.addMessage(text);
            }
        }else{
            Call.sendMessage(con, text, null, null);
        }
    }

    void sendMessage(String text, Player from){
        sendMessage(text, from, null);
    }

    void sendMessage(String text, Player from, String unformatted){
        if(isLocal()){
            if(ui != null){
                ui.chatfrag.addMessage(text);
            }
        }else{
            Call.sendMessage(con, text, unformatted, from);
        }
    }

    PlayerInfo getInfo(){
        if(isLocal()){
            throw new IllegalArgumentException("Local players cannot be traced and do not have info.");
        }else{
            return netServer.admins.getInfo(uuid());
        }
    }
}
