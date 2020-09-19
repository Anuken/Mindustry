package mindustry.entities.comp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

@EntityDef(value = {Playerc.class}, serialize = false)
@Component(base = true)
abstract class PlayerComp implements UnitController, Entityc, Syncc, Timerc, Drawc{
    static final float deathDelay = 30f;

    @Import float x, y;

    @NonNull @ReadOnly Unit unit = Nulls.unit;
    transient private Unit lastReadUnit = Nulls.unit;
    transient @Nullable NetConnection con;

    @ReadOnly Team team = Team.sharded;
    @SyncLocal boolean typing, shooting, boosting;
    boolean admin;
    @SyncLocal float mouseX, mouseY;
    String name = "noname";
    Color color = new Color();

    transient float deathTimer;
    transient String lastText = "";
    transient float textFadeTime;

    public boolean isBuilder(){
        return unit instanceof Builderc;
    }

    public boolean isMiner(){
        return unit instanceof Minerc;
    }

    public @Nullable CoreBuild closestCore(){
        return state.teams.closestCore(x, y, team);
    }

    public @Nullable CoreBuild core(){
        return team.core();
    }

    public TextureRegion icon(){
        //display default icon for dead players
        if(dead()) return core() == null ? UnitTypes.alpha.icon(Cicon.full) : ((CoreBlock)core().block).unitType.icon(Cicon.full);

        return unit.icon();
    }

    public boolean displayAmmo(){
        return unit instanceof BlockUnitc || state.rules.unitAmmo;
    }

    public void reset(){
        team = state.rules.defaultTeam;
        admin = typing = false;
        textFadeTime = 0f;
        if(!dead()){
            unit.controller(unit.type().createController());
            unit = Nulls.unit;
        }
    }

    @Override
    public boolean isValidController(){
        return isAdded();
    }

    @Replace
    public float clipSize(){
        return unit.isNull() ? 20 : unit.type().hitsize * 2f;
    }

    @Override
    public void afterSync(){
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

        CoreBuild core = closestCore();

        if(!dead()){
            set(unit);
            unit.team(team);
            deathTimer = 0;

            //update some basic state to sync things
            if(unit.type().canBoost){
                Tile tile = unit.tileOn();
                unit.elevation = Mathf.approachDelta(unit.elevation, (tile != null && tile.solid()) || boosting ? 1f : 0f, 0.08f);
            }
        }else if(core != null){
            //have a small delay before death to prevent the camera from jumping around too quickly
            //(this is not for balance)
            deathTimer += Time.delta;
            if(deathTimer >= deathDelay){
                //request spawn - this happens serverside only
                core.requestSpawn(self());
                deathTimer = 0;
            }
        }

        textFadeTime -= Time.delta / (60 * 5);

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

    public Minerc miner(){
        return !(unit instanceof Minerc) ? Nulls.miner : (Minerc)unit;
    }

    public Builderc builder(){
        return !(unit instanceof Builderc) ? Nulls.builder : (Builderc)unit;
    }

    public void unit(Unit unit){
        if(unit == null) throw new IllegalArgumentException("Unit cannot be null. Use clearUnit() instead.");
        if(this.unit == unit) return;

        if(this.unit != Nulls.unit){
            //un-control the old unit
            this.unit.controller(this.unit.type().createController());
        }
        this.unit = unit;
        if(unit != Nulls.unit){
            unit.team(team);
            unit.controller(this);

            //this player just became remote, snap the interpolation so it doesn't go wild
            if(unit.isRemote()){
                unit.snapInterpolation();
            }
        }

        Events.fire(new UnitChangeEvent(self(), unit));
    }

    boolean dead(){
        return unit.isNull() || !unit.isValid();
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

    void kick(String reason){
        con.kick(reason);
    }

    void kick(String reason, int duration){
        con.kick(reason, duration);
    }

    @Override
    public void draw(){
        Draw.z(Layer.playerName);
        float z = Drawf.text();

        Font font = Fonts.def;
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
            String text = textFadeTime <= 0 || lastText == null ? "[lightgray]" + Strings.animated(Time.time(), 4, 15f, ".") : lastText;
            float width = 100f;
            float visualFadeTime = 1f - Mathf.curve(1f - textFadeTime, 0.9f);
            font.setColor(1f, 1f, 1f, textFadeTime <= 0 || lastText == null ? 1f : visualFadeTime);

            layout.setText(font, text, Color.white, width, Align.bottom, true);

            Draw.color(0f, 0f, 0f, 0.3f * (textFadeTime <= 0 || lastText == null  ? 1f : visualFadeTime));
            Fill.rect(unit.x, unit.y + textHeight + layout.height - layout.height/2f, layout.width + 2, layout.height + 3);
            font.draw(text, unit.x - width/2f, unit.y + textHeight + layout.height, width, Align.center, true);
        }

        Draw.reset();
        Pools.free(layout);
        font.getData().setScale(1f);
        font.setColor(Color.white);
        font.setUseIntegerPositions(ints);

        Draw.z(z);
    }

    void sendMessage(String text){
        if(isLocal()){
            if(ui != null){
                ui.chatfrag.addMessage(text, null);
            }
        }else{
            Call.sendMessage(con, text, null, null);
        }
    }

    void sendMessage(String text, Player from){
        sendMessage(text, from, NetClient.colorizeName(from.id(), from.name));
    }

     void sendMessage(String text, Player from, String fromName){
        if(isLocal()){
            if(ui != null){
                ui.chatfrag.addMessage(text, fromName);
            }
        }else{
            Call.sendMessage(con, text, fromName, from);
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
