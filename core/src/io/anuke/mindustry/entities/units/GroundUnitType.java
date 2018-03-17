package io.anuke.mindustry.entities.units;

import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public abstract class GroundUnitType extends UnitType{
    //only use for drawing!
    protected Translator tr1 = new Translator();
    //only use for updating!
    protected Translator tr2 = new Translator();

    public GroundUnitType(String name) {
        super(name);
    }

    @Override
    public void draw(BaseUnit unit) {
        float walktime = unit.walkTime; //TODO!

        float ft = Mathf.sin(walktime, 6f, 2f);

        for (int i : Mathf.signs) {
            tr1.trns(unit.baseRotation, ft * i);
            Draw.rect(name + "-leg", unit.x + tr1.x, unit.y + tr1.y, 12f * i, 12f - Mathf.clamp(ft * i, 0, 2), unit.baseRotation - 90);
        }

        Draw.rect(name + "-base", unit.x, unit.y, unit.baseRotation- 90);

        Draw.rect(name, unit.x, unit.y, unit.rotation -90);
    }

    @Override
    public void behavior(BaseUnit unit) {
        tr2.set(unit.target.x, unit.target.y).sub(unit.x, unit.y).limit(speed);

        unit.move(tr2.x, tr2.y);
        unit.rotate(tr2.angle());
    }
}
