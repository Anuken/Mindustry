package mindustry.logic.instructions;

import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class SetRuleI implements LogicInstruction{
    public LogicRule rule = LogicRule.waveSpacing;
    public LogicVar value, p1, p2, p3, p4;

    public SetRuleI(LogicRule rule, LogicVar value, LogicVar p1, LogicVar p2, LogicVar p3, LogicVar p4){
        this.rule = rule;
        this.value = value;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }

    public SetRuleI(){
    }

    @Override
    public void run(LogicExecutor exec){
        switch(rule){
            case waveTimer -> state.rules.waveTimer = value.bool();
            case wave -> state.wave = Math.max(value.numi(), 1);
            case currentWaveTime -> state.wavetime = Math.max(value.numf() * 60f, 0f);
            case waves -> state.rules.waves = value.bool();
            case waveSending -> state.rules.waveSending = value.bool();
            case attackMode -> state.rules.attackMode = value.bool();
            case waveSpacing -> state.rules.waveSpacing = value.numf() * 60f;
            case enemyCoreBuildRadius -> state.rules.enemyCoreBuildRadius = value.numf() * 8f;
            case dropZoneRadius -> state.rules.dropZoneRadius = value.numf() * 8f;
            case unitCap -> state.rules.unitCap = Math.max(value.numi(), 0);
            case lighting -> state.rules.lighting = value.bool();
            case canGameOver -> state.rules.canGameOver = value.bool();
            case pauseDisabled -> state.rules.pauseDisabled = value.bool();
            case musicVolume -> state.rules.musicVolume = Mathf.clamp(value.numf());
            case mapArea -> {
                int x = p1.numi(), y = p2.numi(), w = p3.numi(), h = p4.numi();
                if(!checkMapArea(x, y, w, h, false)){
                    Call.setMapArea(x, y, w, h);
                }
            }
            case ambientLight -> state.rules.ambientLight.fromDouble(value.num());
            case unitLight -> state.rules.unitLight = value.bool();
            case solarMultiplier -> state.rules.solarMultiplier = Math.max(value.numf(), 0f);
            case dragMultiplier -> state.rules.dragMultiplier = Math.max(value.numf(), 0f);
            case ban -> {
                Object cont = value.obj();
                if(cont instanceof Block b){
                    // Rebuild PlacementFragment if anything has changed
                    if(state.rules.bannedBlocks.add(b) && !headless) ui.hudfrag.blockfrag.rebuild();
                }else if(cont instanceof UnitType u){
                    state.rules.bannedUnits.add(u);
                }
            }
            case unban -> {
                Object cont = value.obj();
                if(cont instanceof Block b){
                    if(state.rules.bannedBlocks.remove(b) && !headless) ui.hudfrag.blockfrag.rebuild();
                }else if(cont instanceof UnitType u){
                    state.rules.bannedUnits.remove(u);
                }
            }
            case unitHealth, unitBuildSpeed, unitMineSpeed, unitCost, unitDamage, blockHealth, blockDamage, buildSpeed, rtsMinSquad, rtsMinWeight -> {
                Team team = p1.team();
                if(team != null){
                    float num = value.numf();
                    switch(rule){
                        case buildSpeed -> team.rules().buildSpeedMultiplier = Mathf.clamp(num, 0.001f, 50f);
                        case unitHealth -> team.rules().unitHealthMultiplier = Math.max(num, 0.001f);
                        case unitBuildSpeed -> team.rules().unitBuildSpeedMultiplier = Mathf.clamp(num, 0f, 50f);
                        case unitMineSpeed -> team.rules().unitMineSpeedMultiplier = Math.max(num, 0f);
                        case unitCost -> team.rules().unitCostMultiplier = Math.max(num, 0f);
                        case unitDamage -> team.rules().unitDamageMultiplier = Math.max(num, 0f);
                        case blockHealth -> team.rules().blockHealthMultiplier = Math.max(num, 0.001f);
                        case blockDamage -> team.rules().blockDamageMultiplier = Math.max(num, 0f);
                        case rtsMinWeight -> team.rules().rtsMinWeight = num;
                        case rtsMinSquad -> team.rules().rtsMinSquad = (int)num;
                    }
                }
            }
        }
    }


    /** @return whether the map area is already set to this value. */
    public static boolean checkMapArea(int x, int y, int w, int h, boolean set){
        x = Math.max(x, 0);
        y = Math.max(y, 0);
        w = Math.min(state.world.width, w);
        h = Math.min(state.world.height, h);
        boolean full = x == 0 && y == 0 && w == state.world.width && h == state.world.height;

        if(state.rules.limitMapArea){
            if(state.rules.limitX == x && state.rules.limitY == y && state.rules.limitWidth == w && state.rules.limitHeight == h){
                return true;
            }else if(full){
                //disable the rule, covers the whole map
                if(set){
                    int prevX = state.rules.limitX, prevY = state.rules.limitY, prevW = state.rules.limitWidth, prevH = state.rules.limitHeight;
                    state.rules.limitMapArea = false;
                    if(!headless){
                        renderer.updateAllDarkness();
                    }
                    state.world.checkMapArea(prevX, prevY, prevW, prevH);
                    return false;
                }
            }
        }else if(full){ //was already disabled, no need to change anything
            return true;
        }

        if(set){
            int prevX = state.rules.limitX, prevY = state.rules.limitY, prevW = state.rules.limitWidth, prevH = state.rules.limitHeight;
            if(!state.rules.limitMapArea){
                //it was never on in the first place, so the old bounds don't apply
                prevW = 0;
                prevH = 0;
                prevX = -1;
                prevY = -1;
            }
            state.rules.limitMapArea = true;
            state.rules.limitX = x;
            state.rules.limitY = y;
            state.rules.limitWidth = w;
            state.rules.limitHeight = h;
            state.world.checkMapArea(prevX, prevY, prevW, prevH);

            if(!headless){
                renderer.updateAllDarkness();
            }
        }

        return false;
    }

    @Remote(called = Loc.server)
    public static void setMapArea(int x, int y, int w, int h){
        checkMapArea(x, y, w, h, true);
    }
}
