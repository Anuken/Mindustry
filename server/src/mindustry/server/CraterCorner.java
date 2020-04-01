package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class CraterCorner implements ApplicationListener{
    private Interval timer = new Interval();

    private final static Schematic crater = Schematics.readBase64("bXNjaAB4nD3WXY7TMBRAYaeJ7ftntyyERY2gDzwwSAWxahZB7DpnpFaZoya5/ao4SV/SY0vH58fPZyrfXh9/nq9bKr9/vc6NdP/x+fd5bn3/ukJK/85X2s+Xpu18P+bW+Mu0Qqs0oSnNaE6L1bZ0W21bZxvtoGVaoVWa0JRmNKcFra12m1PoPP+NttPGBLKPlufWaIVWaUJTmtGcFpyjcY6+2r68hsBGu9H2dbx9TTVaphVapQlNaUZzWtAa5+2c9762Dn7PA7VjzjeOcqz5RjtomVZolSY0pRnNaUFrtM4s97WV8cv45TXfaJdfxi/jl+d8+9y3rt8iz/neTWnGvs6+QWu0ziyXX8Gv4FfwK/gV/Mqcb5+fK2uCsuYbTWhKM5pzvOB4jdaZ5fKr+FX8Kn4Vv4pfXfONdvlV/Cp+Fb+65hvNOV5wvEbrzHL5CX6Cn+An+Al+gp/gJ/gJfoKf4Cf4CX6Cn+An+F3r3bbWtne7/BQ/xU/xU/wUP8VP8VP8FD/FT/FT/BQ/w8/wM/wMP8PPuH4NP8PP8DP8jOvX8DP8DD/Dz/Bz/Bw/x8/xc/yc69dZ/5z1z1n/nPXPuX4dP8fP8XP8HL/AL/AL7hqBX+AX+AXrX7D+BetfsP4FfoFf4BfcNQK/YL6GWuOu0bibNtQaag21hlpDraHWUGuoNe6mjbtGQ63j0rmbdp4eOk8PnaeHztND5+mh8/TQeXroPD107qYdlzvf9z7P1mfLc2u0wufq/H+8ZL6Ppuxr7Ovse33fx/vIJ/bjfeRtNKPNPbb/2EolFg==");

    @Override
    public void update(){
        if(!state.is(State.playing)) return;

        Core.app.post(() -> world.tile(0,0).set(Blocks.coreShard, Team.crux));

        if(!Config.crater.bool()) return;

        if(!timer.get(0, 60)) return;

        schematics.placeDown(crater, 3, world.height() - 3 - crater.height);
    }
}
