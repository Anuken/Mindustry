package io.anuke.ucore.scene.ui;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;

import io.anuke.ucore.core.Musics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.function.StringProcessor;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class SettingsDialog extends Dialog{
	public SettingsTable main;

	public SettingsDialog() {
		super(Bundles.get("text.settings", "Settings"));
		addCloseButton();

		main = new SettingsTable();

		content().add(main);
		content().row();
		content().addButton(Bundles.get("text.settings.reset", "Reset to Defaults"), ()->{
			for(SettingsTable.Setting setting : main.list){
				Settings.put(setting.name, Settings.getDefault(setting.name));
				Settings.save();
			}
			main.rebuild();
		}).pad(4).left();
	}

	public static class SettingsTable extends Table{
		protected Array<Setting> list = new Array<>();
		protected Consumer<SettingsTable> rebuilt;

		public SettingsTable(){
			left();
		}

		public SettingsTable(Consumer<SettingsTable>  rebuilt){
			this.rebuilt = rebuilt;
			left();
		}

		public Array<Setting> getSettings() {
			return list;
		}

		public void screenshakePref() {
			sliderPref("screenshake", Bundles.get("setting.screenshake.name", "Screen Shake"), 4, 0, 8, i -> (i / 4f) + "x");
		}

		public void volumePrefs() {

			sliderPref("musicvol", Bundles.get("setting.musicvol.name", "Music Volume"), 10, 0, 10, 1, i -> {
				Musics.updateVolume();
				return (int) (i * 10) + "%";
			});
			checkPref("mutemusic", Bundles.get("setting.mutemusic.name", "Music Music"), false, Musics::setMuted);

			sliderPref("sfxvol", Bundles.get("setting.sfxvol.name", "SFX Volume"), 10, 0, 10, 1, i -> (int) (i * 10) + "%");
			checkPref("mutesound", Bundles.get("setting.mutesound.name", "Mute Sound"), false, Sounds::setMuted);

			Musics.setMuted(Settings.getBool("mutemusic"));
			Sounds.setMuted(Settings.getBool("mutesound"));
		}

		public void sliderPref(String name, String title, int def, int min, int max, StringProcessor s) {
			sliderPref(name, title, def, min, max, 1, s);
		}

		public void sliderPref(String name, String title, int def, int min, int max, int step, StringProcessor s) {
			list.add(new SliderSetting(name, title, def, min, max, step, s));
			Settings.defaults(name, def);
			rebuild();
		}

		public void sliderPref(String name, int def, int min, int max, StringProcessor s) {
			sliderPref(name, def, min, max, 1, s);
		}

		public void sliderPref(String name, int def, int min, int max, int step, StringProcessor s) {
			list.add(new SliderSetting(name, Bundles.get("setting."+name+".name"), def, min, max, step, s));
			Settings.defaults(name, def);
			rebuild();
		}

		public void checkPref(String name, String title, boolean def) {
			list.add(new CheckSetting(name, title, def, null));
			Settings.defaults(name, def);
			rebuild();
		}

		public void checkPref(String name, String title, boolean def, Consumer<Boolean> changed) {
			list.add(new CheckSetting(name, title, def, changed));
			Settings.defaults(name, def);
			rebuild();
		}

		/**Localized title.*/
		public void checkPref(String name, boolean def) {
			list.add(new CheckSetting(name, Bundles.get("setting."+name+".name"), def, null));
			Settings.defaults(name, def);
			rebuild();
		}

		/**Localized title.*/
		public void checkPref(String name, boolean def, Consumer<Boolean> changed) {
			list.add(new CheckSetting(name, Bundles.get("setting."+name+".name"), def, changed));
			Settings.defaults(name, def);
			rebuild();
		}

		void rebuild() {
			clearChildren();

			for (Setting setting : list) {
				setting.add(this);
			}

			addButton(Bundles.get("text.settings.reset", "Reset to Defaults"), ()->{
				for(SettingsTable.Setting setting : list){
					Settings.put(setting.name, Settings.getDefault(setting.name));
					Settings.save();
				}
				rebuild();
			}).margin(16).pad(6).left();

			if(rebuilt != null) rebuilt.accept(this);
		}

		public abstract class Setting {
			public String name;
			public String title;

			abstract void add(SettingsTable table);
		}

		public class CheckSetting extends Setting {
			boolean def;
			Consumer<Boolean> changed;

			CheckSetting(String name, String title, boolean def, Consumer<Boolean> changed) {
				this.name = name;
				this.title = title;
				this.def = def;
				this.changed = changed;
			}

			@Override
			void add(SettingsTable table) {
				CheckBox box = new CheckBox(title);

				box.setChecked(Settings.getBool(name));

				box.changed(() -> {
					Settings.putBool(name, box.isChecked);
					Settings.save();
					if (changed != null) {
						changed.accept(box.isChecked);
					}
				});

				box.left();
				table.add(box).minWidth(box.getPrefWidth() + 50).left().padTop(3f);
				table.add().grow();
				table.row();
			}
		}

		public class SliderSetting extends Setting {
			int def;
			int min;
			int max;
			int step;
			StringProcessor sp;

			SliderSetting(String name, String title, int def, int min, int max, int step, StringProcessor s) {
				this.name = name;
				this.title = title;
				this.def = def;
				this.min = min;
				this.max = max;
				this.step = step;
				this.sp = s;
			}

			@Override
			void add(SettingsTable table) {
				Slider slider = new Slider(min, max, step, false);

				slider.setValue(Settings.getInt(name));

				Label label = new Label(title);
				slider.changed(() -> {
					Settings.putInt(name, (int) slider.getValue());
					Settings.save();
					label.setText(title + ": " + sp.get((int) slider.getValue()));
				});

				slider.change();

				table.add(label).minWidth(label.getPrefWidth() + 50).left().padTop(3f);
				table.add(slider).width(180).padTop(3f);
				table.row();
			}
		}

	}

}
