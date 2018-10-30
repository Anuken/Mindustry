package io.anuke.ucore.scene.event;

import io.anuke.ucore.function.BooleanProvider;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.scene.utils.Disableable;

public class HandCursorListener extends ClickListener{
	private BooleanProvider enabled = () -> true;
	private boolean set;

	public void setEnabled(BooleanProvider vis){
		this.enabled = vis;
	}

	@Override
	public void enter (InputEvent event, float x, float y, int pointer, Element fromActor) {
		super.enter(event, x, y, pointer, fromActor);

		if(!enabled.get()) return;
		
		if(event.getTarget() instanceof Disableable){
			if(((Disableable)event.getTarget()).isDisabled())
				return;
		}
		if (pointer == -1 && event.getTarget().isVisible()) {
			Cursors.setHand();
			set = true;
		}
	}

	@Override
	public void exit (InputEvent event, float x, float y, int pointer, Element toActor) {
		super.exit(event, x, y, pointer, toActor);

		if(!enabled.get() || !set) return;

		if (pointer == -1) {
			Cursors.restoreCursor();
		}
		set = false;
	}
}
