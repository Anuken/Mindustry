package io.anuke.mindustry.ui.fragments;

import io.anuke.mindustry.input.InputHandler;
import io.anuke.ucore.scene.Group;

/**Fragment for displaying overlays such as block inventories. One is created for each input handler.*/
public class OverlayFragment implements Fragment{
    public final BlockInventoryFragment inv;
    public final BlockConfigFragment config;

    private Group group = new Group();
    private InputHandler input;

    public OverlayFragment(InputHandler input){
        this.input = input;

        inv = new BlockInventoryFragment(input);
        config = new BlockConfigFragment(input);
    }

    @Override
    public void build(Group parent){
        group.setFillParent(true);
        parent.addChild(group);

        inv.build(group);
        config.build(group);

        input.buildUI(group);
    }

    public void remove(){
        group.remove();
    }
}
