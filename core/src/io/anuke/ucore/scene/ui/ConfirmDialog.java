package io.anuke.ucore.scene.ui;

import io.anuke.ucore.function.Listenable;

public class ConfirmDialog extends Dialog{
	Listenable confirm;

	public ConfirmDialog(String title, String text, Listenable confirm) {
		super(title, "dialog");
		this.confirm = confirm;
		content().add(text);
		buttons().addButton("Ok", ()->{
			confirm.listen();
			hide();
		});
		buttons().addButton("Cancel", ()-> hide());
	}

}
