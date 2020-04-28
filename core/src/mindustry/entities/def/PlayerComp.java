package mindustry.entities.def;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

@EntityDef(value = {Playerc.class}, serialize = false)
@Component
abstract class PlayerComp implements UnitController, Entityc, Syncc, Timerc, Drawc{
    static final float deathDelay = 30f;

    @NonNull @ReadOnly Unitc unit = Nulls.unit;

    @ReadOnly Team team = Team.sharded;
    String name = "noname";
    @Nullable NetConnection con;
    boolean admin, typing;
    Color color = new Color();
    float mouseX, mouseY;
    float deathTimer;

    String lastText = "";
    float textFadeTime;

    public boolean isBuilder(){
        return unit instanceof Builderc;
    }

    public boolean isMiner(){
        return unit instanceof Minerc;
    }

    public @Nullable CoreEntity closestCore(){
        return state.teams.closestCore(x(), y(), team);
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
    public float clipSize(){
        return 20;
    }

    @Override
    public void update(){
        if(unit.dead()){
            clearUnit();
        }

        CoreEntity core = closestCore();

        if(!dead()){
            x(unit.x());
            y(unit.y());
            unit.team(team);
            deathTimer = 0;
        }else if(core != null){
            deathTimer += Time.delta();
            if(deathTimer >= deathDelay){
                core.requestSpawn((Playerc)this);
            }
        }

        textFadeTime -= Time.delta() / (60 * 5);
    }

    public void team(Team team){
        this.team = team;
        unit.team(team);
    }

    public void clearUnit(){
        unit(Nulls.unit);
    }

    public Unitc unit(){
        return unit;
    }

    public Minerc miner(){
        return !(unit instanceof Minerc) ? Nulls.miner : (Minerc)unit;
    }

    public Builderc builder(){
        return !(unit instanceof Builderc) ? Nulls.builder : (Builderc)unit;
    }

    public void unit(Unitc unit){
        if(unit == null) throw new IllegalArgumentException("Unit cannot be null. Use clearUnit() instead.");
        if(this.unit != Nulls.unit){
            //un-control the old unit
            this.unit.controller(this.unit.type().createController());
        }
        this.unit = unit;
        if(unit != Nulls.unit){
            unit.team(team);
            unit.controller(this);
        }
    }

    boolean dead(){
        return unit.isNull();
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

    @Override
    public void draw(){
        Draw.z(Layer.playerName);
        float z = Drawf.text();

        BitmapFont font = Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        final float nameHeight = 11;
        final float textHeight = 15;

        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f));
        layout.setText(font, name);

        if(!isLocal()){
            Draw.color(0f, 0f, 0f, 0.3f);
            Fill.rect(unit.x(), unit.y() + nameHeight - layout.height / 2, layout.width + 2, layout.height + 3);
            Draw.color();
            font.setColor(color);
            font.draw(name, unit.x(), unit.y() + nameHeight, 0, Align.center, false);

            if(admin){
                float s = 3f;
                Draw.color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f);
                Draw.rect(Icon.adminSmall.getRegion(), unit.x() + layout.width / 2f + 2 + 1, unit.y() + nameHeight - 1.5f, s, s);
                Draw.color(color);
                Draw.rect(Icon.adminSmall.getRegion(), unit.x() + layout.width / 2f + 2 + 1, unit.y() + nameHeight - 1f, s, s);
            }
        }

        if(Core.settings.getBool("playerchat") && ((textFadeTime > 0 && lastText != null) || typing)){
            String text = textFadeTime <= 0 || lastText == null ? "[LIGHT_GRAY]" + Strings.animated(Time.time(), 4, 15f, ".") : lastText;
            float width = 100f;
            float visualFadeTime = 1f - Mathf.curve(1f - textFadeTime, 0.9f);
            font.setColor(1f, 1f, 1f, textFadeTime <= 0 || lastText == null ? 1f : visualFadeTime);

            layout.setText(font, text, Color.white, width, Align.bottom, true);

            Draw.color(0f, 0f, 0f, 0.3f * (textFadeTime <= 0 || lastText == null  ? 1f : visualFadeTime));
            Fill.rect(unit.x(), unit.y() + textHeight + layout.height - layout.height/2f, layout.width + 2, layout.height + 3);
            font.draw(text, unit.x() - width/2f, unit.y() + textHeight + layout.height, width, Align.center, true);
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

    void sendMessage(String text, Playerc from){
        sendMessage(text, from, NetClient.colorizeName(from.id(), from.name()));
    }

     void sendMessage(String text, Playerc from, String fromName){
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
