package io.anuke.mindustry.entities;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.function.Callable;

public class ItemTransfer extends TimedEntity {


    public static void create(Item item, float fromx, float fromy, float tox, float toy, Callable done){

    }

    private ItemTransfer(){}

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void draw() {

    }

    @Override
    public <T extends Entity> T add() {
        return super.add(Vars.effectGroup);
    }
}
