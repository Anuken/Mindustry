package io.anuke.mindustry;


import com.badlogic.gdx.Gdx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

public class AndroidTextFieldDialog{
	private Activity activity;
	private EditText userInput;
	private AlertDialog.Builder builder;
	private TextPromptListener listener;
	private boolean isBuild;

	public AndroidTextFieldDialog() {
		this.activity = (Activity)Gdx.app;
		load();
	}

	public AndroidTextFieldDialog show() {

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Gdx.app.error("Android Dialogs", AndroidTextFieldDialog.class.getSimpleName() + " now shown.");
				AlertDialog dialog = builder.create();
				
				dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				
				dialog.show();
				
			}
		});

		return this;
	}

	private AndroidTextFieldDialog load() {

		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
				LayoutInflater li = LayoutInflater.from(activity);

				View promptsView = li.inflate(getResourceId("gdxdialogs_inputtext", "layout"), null);
				
				alertDialogBuilder.setView(promptsView);

				userInput = (EditText) promptsView.findViewById(getResourceId("gdxDialogsEditTextInput", "id"));

				alertDialogBuilder.setCancelable(false);
				builder = alertDialogBuilder;

				isBuild = true;
			}
		});

		// Wait till TextPrompt is built.
		while (!isBuild) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

		return this;
	}

	public int getResourceId(String pVariableName, String pVariableType) {
		try {
			return activity.getResources().getIdentifier(pVariableName, pVariableType, activity.getPackageName());
		} catch (Exception e) {
			Gdx.app.error("Android Dialogs", "Cannot find resouce with name: " + pVariableName
					+ " Did you copy the layouts to /res/layouts and /res/layouts_v14 ?");
			e.printStackTrace();
			return -1;
		}
	}

	public AndroidTextFieldDialog setText(CharSequence value) {
		userInput.append(value);
		return this;
	}

	public AndroidTextFieldDialog setCancelButtonLabel(CharSequence label) {
		builder.setNegativeButton(label, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		return this;
	}

	public AndroidTextFieldDialog setConfirmButtonLabel(CharSequence label) {
		builder.setPositiveButton(label, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (listener != null && !userInput.getText().toString().isEmpty()) {
					listener.confirm(userInput.getText().toString());
				}
				
			}
		});
		return this;
	}

	public AndroidTextFieldDialog setTextPromptListener(TextPromptListener listener) {
		this.listener = listener;
		return this;
	}

	public AndroidTextFieldDialog setInputType(int type) {
		userInput.setInputType(type);
		return this;
	}

	public AndroidTextFieldDialog setMaxLength(int length) {
		userInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(length) });
		return this;
	}
	
	public static interface TextPromptListener{
		public void confirm(String text);
	}

}
