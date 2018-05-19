package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Strings;
import static io.anuke.mindustry.Vars.*;

public class RollbackDialog extends FloatingDialog {
	
	public RollbackDialog(){
		super("$text.server.rollback");
		
		setup();
		shown(this::setup);
	}
	
	private void setup(){
		content().clear();
		buttons().clear();
		
		if(gwt) return;
		
		content().row();
		content().add("$text.server.rollback.numberfield");
		
		TextField field = content().addField("", t->{}).size(60f, 48f).get();
		field.setTextFieldFilter((f, c) -> field.getText().length() < 4);
		
		content().row();
		buttons().defaults().size(200f, 50f).left().pad(2f);
		buttons().addButton("$text.cancel", this::hide);
		
		buttons().addButton("$text.ok", () -> {
			NetEvents.handleRollbackRequest(Integer.valueOf(field.getText()));
			hide();
		}).disabled(b -> field.getText().isEmpty() || !Strings.canParsePostiveInt(field.getText()));
	}
}
