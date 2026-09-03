package mindustry.entities;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.Units.*;
import mindustry.game.*;
import mindustry.gen.*;

/** Unit cluster counter for grouped targeting. */
public class UnitCluster implements Sortf{
    public final float radius, dstWeight;
    final IntIntMap counts = new IntIntMap();
    float timer;

    public UnitCluster(float radius){
        this(radius, -1f);
    }

    public UnitCluster(float radius, float dstWeight){
        this.radius = radius;
        this.dstWeight = dstWeight;
    }

    /** Only updated when needed, regardless of callers */
    public void update(){
        if((timer += Time.delta) > 10f || counts.size == 0){
            timer = 0f;
            counts.clear();
            Groups.unit.each(u -> counts.increment(clusterKey(u.team, u.x, u.y, radius)));
        }
    }

    @Override
    public float cost(Unit u, float x, float y){
        update();
        return -counts.get(clusterKey(u.team, u.x, u.y, radius), 0) + (dstWeight > 0 ? Mathf.dst2(u.x, u.y, x, y) / dstWeight : 0f);
    }

    //8 bits for team. Does not support negative coords
    private static int clusterKey(Team team, float x, float y, float radius){
        return (team.id << 24) | ((Mathf.floor(x / radius) & 0xFFF) << 12) | (Mathf.floor(y / radius) & 0xFFF);
    }
}
