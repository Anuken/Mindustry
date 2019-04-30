package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.scene.Group;
import io.anuke.mindustry.input.InputHandler;

/** Fragment for displaying overlays such as block inventories. */
public class OverlayFragment extends Fragment{
    public final BlockInventoryFragment inv;
    public final BlockConfigFragment config;

    private Group group = new Group();

    public OverlayFragment(InputHandler input){
        inv = new BlockInventoryFragment();
        config = new BlockConfigFragment();
    }

    @Override
    public void build(Group parent){
        group.setFillParent(true);
        parent.addChild(group);

        inv.build(group);
        config.build(group);
    }

    public void remove(){
        group.remove();
    }
}
