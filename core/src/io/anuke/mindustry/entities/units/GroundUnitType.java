package io.anuke.mindustry.entities.units;

import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public abstract class GroundUnitType extends UnitType{
    protected Translator tr = new Translator();

    public GroundUnitType(String name) {
        super(name);
    }

    @Override
    public void draw(BaseUnit unit) {
        float walktime = 0; //TODO!

        float ft = Mathf.sin(walktime, 6f, 2f);

        for (int i : Mathf.signs) {
            tr.trns(unit.baseRotation, ft * i);
            Draw.rect(name + "-leg", unit.x + tr.x, unit.y + tr.y, 12f * i, 12f - Mathf.clamp(ft * i, 0, 2), unit.baseRotation - 90);
        }

        Draw.rect(name + "-base", unit.x, unit.y, unit.baseRotation- 90);

        Draw.rect(name, unit.x, unit.y, unit.rotation -90);
    }

    @Override
    public void behavior(BaseUnit unit) {

    }
}
