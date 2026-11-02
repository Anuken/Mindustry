package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

/** Editor for the 3D space camera */
public class SpaceCameraDialog extends BaseDialog{
    private SpaceCamera camera;
    private Rules rules;
    private Planet selected;
    private PlanetParams params;
    private final Seq<Runnable> liveUpdaters = new Seq<>();
    private float lastX, lastY;

    public SpaceCameraDialog(){
        super("@spacecamera", Styles.fullDialog);
        setFillParent(true);
        margin(0f);
        shouldPause = true;
        shown(this::setup);
        addCloseListener();

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                if(camera == null || event.targetActor != SpaceCameraDialog.this) return false;
                scrollCamera(amountY);
                return true;
            }
        });
    }

    public void show(Rules rules){
        this.rules = rules;

        if(rules.spaceCamera == null){
            camera = new SpaceCamera();

            Planet body = rules.planet == null ? Planets.serpulo : rules.planet;
            camera.planet = body.name;
            camera.captureAngles(body);
        }else{
            camera = JsonIO.copy(rules.spaceCamera);

            Planet body = camera.planet();
            camera.planet = body.name;
            camera.fillAngles(body);
        }

        super.show();
    }

    private void setup(){
        clearChildren();
        liveUpdaters.clear();
        margin(0f);

        selected = camera.planet();
        camera.planet = selected.name;
        camera.fillAngles(selected);

        params = new PlanetParams();

        Element view = new Element(){
            {
                touchable = Touchable.enabled;

                addListener(new InputListener(){
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        lastX = x;
                        lastY = y;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        float dx = x - lastX, dy = y - lastY;
                        lastX = x;
                        lastY = y;

                        if(camera.attached){
                            float factor = 0.01f * camera.zoom;
                            camera.lon += dx * factor * Mathf.cos(camera.lat);
                            camera.lat = Mathf.clamp(camera.lat + dy * factor, -1.4f, 1.4f);
                        }else{
                            camera.yaw += dx * 0.005f;
                            camera.pitch = Mathf.clamp(camera.pitch + dy * 0.005f, -1.5f, 1.5f);
                        }
                    }

                });
            }

            @Override
            public void act(float delta){
                super.act(delta);
                //makes scroll not be taken by the panel when dialog is open so when mouse off
                if(scene != null && scene.getDialog() == SpaceCameraDialog.this && (scene.getHoverElement() == null || !scene.getHoverElement().isDescendantOf(e -> e instanceof ScrollPane))){
                    scene.setScrollFocus(SpaceCameraDialog.this);
                }

                //preview always advances; saved maps only advance when updateInGame is enabled
                camera.update(Math.min(delta, 0.05f));

                for(Runnable updater : liveUpdaters){
                    updater.run();
                }

                updateInput(delta);
            }

            @Override
            public void draw(){
                camera.apply(params);
                params.drawUi = true;
                params.disableAA = true;
                renderer.planets.render(params);
            }
        };

        Table panel = new Table();
        panel.table(Tex.pane, this::buildPanel).width(330f).top().left();

        Table overlay = new Table();
        overlay.touchable = Touchable.childrenOnly;
        overlay.left();
        overlay.add(new ScrollPane(panel, Styles.smallPane))
            .width(350f)
            .maxHeight(Math.max(220f, Core.graphics.getHeight() / Scl.scl(1f) - 140f))
            .pad(12f).left();

        Table bottom = new Table();
        bottom.touchable = Touchable.childrenOnly;
        bottom.bottom();
        bottom.button("@spacecamera.frame", Icon.map, this::frameSystem).size(180f, 50f).pad(4f);
        bottom.button("@spacecamera.reset", Icon.refresh, () -> {
            camera.captureAngles(selected);
            setup();
        }).size(170f, 50f).pad(4f);
        bottom.button("@spacecamera.save", Icon.ok, this::save).size(150f, 50f).pad(4f);
        bottom.button("@back", Icon.left, this::hide).size(150f, 50f).pad(4f);

        stack(view, overlay, bottom).grow();
    }

    private void buildPanel(Table t){
        t.margin(8f);
        t.left().top();
        t.add("@spacecamera.body").color(Pal.accent).left().row();

        Table bodies = new Table();
        int i = 0;
        for(Planet planet : content.planets()){
            if(!planet.visible || planet.solarSystem != selected.solarSystem) continue;

            bodies.button(planet.localizedName, Styles.flatTogglet, () -> {
                selected = planet;
                camera.planet = planet.name;
                camera.fillAngles(planet);
                setup();
            }).width(145f).height(40f).pad(2f).checked(b -> selected == planet);

            if(++i % 2 == 0){
                bodies.row();
            }
        }

        t.add(new ScrollPane(bodies, Styles.smallPane)).width(300f).height(126f).row();
        t.image().color(Pal.accent).height(2f).growX().pad(6f, 0f, 6f, 0f).row();

        t.button(camera.attached ? "@spacecamera.orbit" : "@spacecamera.free", Icon.planet, Styles.flatt, () -> {
            if(camera.attached){
                detach();
            }else{
                camera.attached = true;
            }
            setup();
        }).width(300f).row();

        t.check("@spacecamera.clouds", camera.clouds(selected), v -> camera.clouds(selected, v)).left().padTop(4f).row();

        if(camera.attached){
            addSlider(t, "@spacecamera.lon", () -> camera.lon * Mathf.radDeg, 0f, 360f, 1f, v -> camera.lon = v * Mathf.degRad);
            addSlider(t, "@spacecamera.lat", () -> camera.lat * Mathf.radDeg, -80f, 80f, 1f, v -> camera.lat = v * Mathf.degRad);
            addSlider(t, "@spacecamera.zoom", () -> camera.zoom, 0.4f, 4f, 0.05f, v -> camera.zoom = Math.max(v, 0.01f));
        }else{
            addSlider(t, "@spacecamera.x", () -> camera.position.x, -100f, 100f, 0.5f, v -> camera.position.x = v);
            addSlider(t, "@spacecamera.y", () -> camera.position.y, -100f, 100f, 0.5f, v -> camera.position.y = v);
            addSlider(t, "@spacecamera.z", () -> camera.position.z, -100f, 100f, 0.5f, v -> camera.position.z = v);
            addSlider(t, "@spacecamera.yaw", () -> camera.yaw * Mathf.radDeg, 0f, 360f, 1f, v -> camera.yaw = v * Mathf.degRad);
            addSlider(t, "@spacecamera.pitch", () -> camera.pitch * Mathf.radDeg, -85f, 85f, 1f, v -> camera.pitch = v * Mathf.degRad);
        }

        t.image().color(Pal.accent).height(2f).growX().pad(6f, 0f, 6f, 0f).row();
        t.add("@spacecamera.movement").color(Pal.accent).left().row();

        addSlider(t, "@spacecamera.orbitSpeed", () -> camera.orbitSpeed(selected), -30f, 30f, 0.1f, v -> camera.orbitSpeed(selected, v));
        addSlider(t, "@spacecamera.rotationSpeed", () -> camera.rotationSpeed(selected), -30f, 30f, 0.1f, v -> camera.rotationSpeed(selected, v));
        addSlider(t, "@spacecamera.cloudSpeed", () -> camera.cloudSpeed(selected), -4f, 4f, 0.1f, v -> camera.cloudSpeed(selected, v));

        t.check("@spacecamera.updateingame", camera.updateInGame, v -> camera.updateInGame = v).left().padTop(4f).row();

        t.image().color(Pal.accent).height(2f).growX().pad(6f, 0f, 6f, 0f).row();
        t.add("@spacecamera.bodyvalues").color(Pal.accent).left().row();

        if(selected.parent != null){
            addSlider(t, "@spacecamera.orbitAngle", () -> camera.orbitAngle(selected), 0f, 360f, 1f,
                v -> camera.orbitAngles.put(selected.name, v));
        }
        addSlider(t, "@spacecamera.rotationAngle", () -> camera.rotationAngle(selected), 0f, 360f, 1f,
            v -> camera.rotationAngles.put(selected.name, v));

        t.add("@spacecamera.hint").style(Styles.outlineLabel).wrap().width(300f).padTop(8f).row();
    }

    private void addSlider(Table table, String text, Floatp value, float min, float max, float step, Floatc changed){
        table.table(row -> {
            row.left();
            row.add(text).left().width(112f);

            Slider slider = new Slider(min, max, step, false);
            float initial = value.get();
            slider.setValue(Mathf.clamp(initial, min, max));

            TextField field = new TextField("", Styles.defaultField);
            field.setProgrammaticChangeEvents(false);
            field.setFilter((f, c) -> Character.isDigit(c) || c == '-' || c == '.' || c == '+');
            field.setValidator(s -> s.isEmpty() || s.equals("-") || s.equals(".") || s.equals("-.") || validNumber(s));
            field.setText(Strings.autoFixed(initial, 2));
            field.changed(() -> {
                String s = field.getText();
                if(validNumber(s)){
                    float v = Float.parseFloat(s);
                    changed.get(v);
                    slider.setValue(Mathf.clamp(v, min, max), false);
                }
            });

            slider.changed(() -> {
                float v = slider.getValue();
                changed.get(v);
                if(!field.hasKeyboard()){
                    field.setText(Strings.autoFixed(v, 2));
                }
            });

            row.add(slider).width(92f).padLeft(4f);
            row.add(field).width(86f).height(36f).padLeft(4f).right();

            liveUpdaters.add(() -> {
                if(!field.hasKeyboard()){
                    float v = value.get();
                    slider.setValue(Mathf.clamp(v, min, max), false);
                    String string = Strings.autoFixed(v, 2);
                    if(!field.getText().equals(string)){
                        field.setText(string);
                    }
                }
            });
        }).left().row();
    }

    private static boolean validNumber(String text){
        try{
            return Float.isFinite(Float.parseFloat(text));
        }catch(NumberFormatException e){
            return false;
        }
    }

    private void scrollCamera(float amountY){
        if(camera.attached){
            camera.zoom = Mathf.clamp(camera.zoom + amountY * 0.08f, 0.4f, 4f);
        }else{
            float cp = Mathf.cos(camera.pitch);
            float move = -amountY * 3f;
            camera.position.add(cp * Mathf.cos(camera.yaw) * move, Mathf.sin(camera.pitch) * move, cp * Mathf.sin(camera.yaw) * move);
        }
    }

    private void updateInput(float delta){
        if(camera.attached || Core.scene.getKeyboardFocus() instanceof TextField) return;

        boolean forward = input.keyDown(KeyCode.w) || input.keyDown(KeyCode.up);
        boolean back = input.keyDown(KeyCode.s) || input.keyDown(KeyCode.down);
        boolean left = input.keyDown(KeyCode.a) || input.keyDown(KeyCode.left);
        boolean right = input.keyDown(KeyCode.d) || input.keyDown(KeyCode.right);
        boolean up = input.keyDown(KeyCode.e);
        boolean down = input.keyDown(KeyCode.q);

        if(!(forward || back || left || right || up || down)) return;

        float dt = Math.min(delta, 0.05f);
        float speed = 8f * dt * (input.shift() ? 3f : 1f);

        float moveForward = (forward ? 1 : 0) - (back ? 1 : 0);
        float moveRight = (right ? 1 : 0) - (left ? 1 : 0);
        float moveUp = (up ? 1 : 0) - (down ? 1 : 0);

        float cp = Mathf.cos(camera.pitch);
        float fx = cp * Mathf.cos(camera.yaw);
        float fy = Mathf.sin(camera.pitch);
        float fz = cp * Mathf.sin(camera.yaw);
        float rx = -Mathf.sin(camera.yaw);
        float rz = Mathf.cos(camera.yaw);

        camera.position.add(
            (fx * moveForward + rx * moveRight) * speed,
            (fy * moveForward + moveUp) * speed,
            (fz * moveForward + rz * moveRight) * speed
        );
    }

    private void detach(){
        if(!camera.attached) return;

        refreshPositions();

        float distance = (selected.radius + selected.camRadius) * (2f + camera.zoom * 2f);
        float cy = Mathf.cos(camera.lat);

        camera.position.set(selected.position)
            .add(cy * Mathf.cos(camera.lon) * distance, Mathf.sin(camera.lat) * distance, cy * Mathf.sin(camera.lon) * distance);
        lookAt(selected.position);
        camera.attached = false;

        clearOverrides();
    }

    private void lookAt(Vec3 target){
        float dx = target.x - camera.position.x;
        float dy = target.y - camera.position.y;
        float dz = target.z - camera.position.z;

        camera.yaw = (float)Math.atan2(dz, dx);
        camera.pitch = (float)Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
    }

    private void frameSystem(){
        camera.attached = false;
        refreshPositions();

        Planet root = selected.solarSystem;
        Vec3 center = new Vec3(root.position);
        float radius = 4f;

        for(Planet planet : content.planets()){
            if(planet.solarSystem == root){
                radius = Math.max(radius, planet.position.dst(center) + planet.radius);
            }
        }

        camera.position.set(center.x, center.y + radius * 0.2f, center.z + radius * 2.5f);
        lookAt(center);
        clearOverrides();
        setup();
    }

    /** Recalculates body positions with this camera's saved angles. */
    private void refreshPositions(){
        Planet root = selected.solarSystem;
        for(Planet planet : content.planets()){
            if(planet.solarSystem == root){
                planet.orbitAngleOverride = camera.orbitAngles.get(planet.name);
                planet.rotationAngleOverride = camera.rotationAngles.get(planet.name);
            }
        }
        universe.updateGlobal();
    }

    private void clearOverrides(){
        Planet root = selected.solarSystem;
        for(Planet planet : content.planets()){
            if(planet.solarSystem == root){
                planet.orbitAngleOverride = null;
                planet.rotationAngleOverride = null;
            }
        }
        universe.updateGlobal();
    }

    private void save(){
        camera.planet = selected.name;
        rules.spaceCamera = camera;

        if(renderer.backgroundBuffer != null){
            renderer.backgroundBuffer.dispose();
            renderer.backgroundBuffer = null;
        }

        hide();
        ui.showInfoFade("@spacecamera.saved");
    }
}
