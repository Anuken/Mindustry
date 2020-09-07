package mindustry.logic;

import mindustry.game.*;
import mindustry.gen.*;

public enum RadarTarget{
    any((team, other) -> true),
    enemy((team, other) -> team != other.team),
    ally((team, other) -> team == other.team),
    player((team, other) -> other.isPlayer()),
    flying((team, other) -> other.isFlying()),
    ground((team, other) -> other.isGrounded());

    public final RadarTargetFunc func;

    public static final RadarTarget[] all = values();

    RadarTarget(RadarTargetFunc func){
        this.func = func;
    }

    public interface RadarTargetFunc{
        boolean get(Team team, Unit other);
    }
}
