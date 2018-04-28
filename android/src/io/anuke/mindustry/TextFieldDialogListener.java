package io.anuke.mindustry;


import android.text.InputType;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import io.anuke.ucore.scene.event.ChangeListener;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.TextField;

public class TextFieldDialogListener extends ClickListener{
	private TextField field;
	private int type;
	private int max;

	public static void add(TextField field, int type, int max){
		field.addListener(new TextFieldDialogListener(field, type, max));
		field.addListener(new InputListener(){
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				Gdx.input.setOnscreenKeyboardVisible(false);
				return false;
			}
		});
	}

	public static void add(TextField field){
		add(field, 0, 16);
	}

	//type - 0 is text, 1 is numbers, 2 is decimals
	public TextFieldDialogListener(TextField field, int type, int max){
		this.field = field;
		this.type = type;
		this.max = max;
	}

	public void clicked(final InputEvent event, float x, float y){
		
		if(Gdx.app.getType() == ApplicationType.Desktop) return;
		
		AndroidTextFieldDialog dialog = new AndroidTextFieldDialog();

		dialog.setTextPromptListener(text -> {
            field.clearText();
            field.appendText(text);
            field.fire(new ChangeListener.ChangeEvent());
            Gdx.graphics.requestRendering();
        });

		if(type == 0){
			dialog.setInputType(InputType.TYPE_CLASS_TEXT);
		}else if(type == 1){
			dialog.setInputType(InputType.TYPE_CLASS_NUMBER);
		}else if(type == 2){
			dialog.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
		}

		dialog.setConfirmButtonLabel("OK").setText(field.getText());
		dialog.setCancelButtonLabel("Cancel");
		dialog.setMaxLength(max);
		dialog.show();
		event.cancel();

	}
}
