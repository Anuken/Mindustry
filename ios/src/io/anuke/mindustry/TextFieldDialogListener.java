package io.anuke.mindustry;

import com.badlogic.gdx.Gdx;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.TextField;
import org.robovm.apple.foundation.NSRange;
import org.robovm.apple.uikit.*;
import org.robovm.rt.bro.annotation.ByVal;

public class TextFieldDialogListener {

    public static void add(TextField field, int maxLength){
        field.addListener(new ClickListener(){
            public void clicked(final InputEvent event, float x, float y){
                show(field, maxLength);
                event.cancel();
            }
        });
        field.addListener(new InputListener(){
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                Gdx.input.setOnscreenKeyboardVisible(false);
                return false;
            }
        });
    }

    private static void show(TextField field, int maxLength){

        UIAlertViewDelegateAdapter delegate = new UIAlertViewDelegateAdapter() {

            @Override
            public void didDismiss(UIAlertView alertView, long buttonIndex) {
                if (buttonIndex == 1) {
                    UITextField textField = alertView.getTextField(0);
                    final String result = textField.getText();

                    Gdx.app.postRunnable(() -> {
                        field.setText(result);
                        field.change();
                    });
                }
            }

            @Override
            public void clicked(UIAlertView alertView, long buttonIndex) {

            }

            @Override
            public void cancel(UIAlertView alertView) {

            }

            @Override
            public void willPresent(UIAlertView alertView) {

            }

            @Override
            public void didPresent(UIAlertView alertView) {

            }

            @Override
            public void willDismiss(UIAlertView alertView, long buttonIndex) {

            }

            @Override
            public boolean shouldEnableFirstOtherButton(UIAlertView alertView) {
                return false;
            }
        };

        String[] otherButtons = new String[1];
        otherButtons[0] = "OK";

        UIAlertView alertView = new UIAlertView("", "", delegate, "Cancel", otherButtons);

        alertView.setAlertViewStyle(UIAlertViewStyle.PlainTextInput);

        UITextField uiTextField = alertView.getTextField(0);
        uiTextField.setText(field.getText());

        uiTextField.setDelegate(new UITextFieldDelegateAdapter() {
            @Override
            public boolean shouldChangeCharacters(UITextField textField, @ByVal NSRange nsRange, String additionalText) {

                if (textField.getText().length() + additionalText.length() > maxLength) {
                    String oldText = textField.getText();
                    String newText = oldText + additionalText;
                    textField.setText(newText.substring(0, maxLength));
                    return false;
                }
                return true;
            }
        });

        alertView.show();

    }
}
