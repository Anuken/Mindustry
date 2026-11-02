package mindustry.graphics.g3d;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Camera state for the campaign space scene. */
public class SpaceCamera{
    /** Name of the planet the camera is attached to, or the body used for the scene's solar system. */
    public @Nullable String planet;
    /** If true, the camera orbits {@link #planet}; otherwise it uses the free position and angles. */
    public boolean attached = true;
    /** If true, the map background keeps advancing this camera after it is saved. */
    public boolean updateInGame = false;
    /** Per-planet cloud visibility. Missing entries are visible. */
    public ObjectMap<String, Boolean> cloudVisibility = new ObjectMap<>();
    /** Per-planet orbit/rotation speeds in degrees per second. Missing entries are 0. */
    public ObjectMap<String, Float> orbitSpeeds = new ObjectMap<>();
    public ObjectMap<String, Float> rotationSpeeds = new ObjectMap<>();
    /** Per-planet cloud rotation multipliers and accumulated clocks. Missing speeds default to 1. */
    public ObjectMap<String, Float> cloudSpeeds = new ObjectMap<>();
    public ObjectMap<String, Float> cloudTimes = new ObjectMap<>();
    /** Free-camera position, in scene units. */
    public Vec3 position = new Vec3(0f, 0f, 8f);
    /** Free-camera yaw/pitch, in radians. */
    public float yaw, pitch;
    /** Attached-camera orbit angles and zoom. */
    public float lon = 0.55f, lat = 0.28f, zoom = 1f;

    /** Saved orbit angles by planet name, in degrees. */
    public ObjectMap<String, Float> orbitAngles = new ObjectMap<>();
    /** Saved rotation angles by planet name, in degrees. */
    public ObjectMap<String, Float> rotationAngles = new ObjectMap<>();

    public @Nullable Planet planet(){
        Planet found = planet == null ? null : content.planet(planet);
        return found == null ? Planets.serpulo : found;
    }

    /** Applies this camera to the supplied render parameters. */
    public void apply(PlanetParams params){
        params.planet = planet();
        params.spaceCamera = this;
        params.otherCamPos = null;

        params.uiAlpha = 1f;

        if(attached){
            params.freeCamera = false;
            params.zoom = zoom;

            float cy = Mathf.cos(lat);
            params.camPos.set(cy * Mathf.cos(lon), Mathf.sin(lat), cy * Mathf.sin(lon));
        }else{
            params.freeCamera = true;
            params.freeCamPos.set(position);
            params.freeCamYaw = yaw;
            params.freeCamPitch = pitch;
        }
    }

    public PlanetParams params(){
        PlanetParams params = new PlanetParams();
        apply(params);
        return params;
    }

    /** Snapshots the current orbit/rotation of every body in the selected solar system. */
    public void captureAngles(Planet body){
        orbitAngles.clear();
        rotationAngles.clear();
        cloudTimes.clear();

        for(Planet other : content.planets()){
            if(other.solarSystem == body.solarSystem){
                if(other.parent != null) orbitAngles.put(other.name, other.getOrbitAngle());
                rotationAngles.put(other.name, other.getRotation());
                cloudTimes.put(other.name, Time.globalTime);
            }
        }
    }

    /** Ensures every body in the scene has a saved angle, without overwriting existing values. */
    public void fillAngles(Planet body){
        for(Planet other : content.planets()){
            if(other.solarSystem == body.solarSystem){
                if(other.parent != null && !orbitAngles.containsKey(other.name)) orbitAngles.put(other.name, other.getOrbitAngle());
                if(!rotationAngles.containsKey(other.name)) rotationAngles.put(other.name, other.getRotation());
                if(!cloudTimes.containsKey(other.name)) cloudTimes.put(other.name, Time.globalTime);
            }
        }
    }

    public float orbitAngle(Planet body){
        Float value = orbitAngles.get(body.name);
        if(value != null) return value;
        return body.parent == null ? 0f : body.getOrbitAngle();
    }

    public float rotationAngle(Planet body){
        Float value = rotationAngles.get(body.name);
        return value == null ? body.getRotation() : value;
    }

    public boolean clouds(Planet body){
        Boolean value = cloudVisibility.get(body.name);
        return value == null || value;
    }

    public void clouds(Planet body, boolean value){
        cloudVisibility.put(body.name, value);
    }

    public float orbitSpeed(Planet body){
        return get(orbitSpeeds, body.name, 0f);
    }

    public void orbitSpeed(Planet body, float value){
        orbitSpeeds.put(body.name, value);
    }

    public float rotationSpeed(Planet body){
        return get(rotationSpeeds, body.name, 0f);
    }

    public void rotationSpeed(Planet body, float value){
        rotationSpeeds.put(body.name, value);
    }

    public float cloudSpeed(Planet body){
        return get(cloudSpeeds, body.name, 1f);
    }

    public void cloudSpeed(Planet body, float value){
        cloudSpeeds.put(body.name, value);
    }

    public float cloudTime(Planet body){
        return get(cloudTimes, body.name, 0f);
    }

    public boolean hasMovement(){
        Planet body = planet();
        for(Planet other : content.planets()){
            if(other.solarSystem != body.solarSystem) continue;
            if(orbitSpeed(other) != 0f || rotationSpeed(other) != 0f) return true;
            if(other.cloudMesh != null && clouds(other) && cloudSpeed(other) != 0f) return true;
        }
        return false;
    }

    /** Advances the saved orbit/rotation angles by delta seconds. */
    public void update(float delta){
        if(delta <= 0f) return;

        Planet body = planet();
        for(Planet other : content.planets()){
            if(other.solarSystem != body.solarSystem) continue;

            float orbit = orbitSpeed(other);
            if(other.parent != null && orbit != 0f){
                orbitAngles.put(other.name, normalize(orbitAngle(other) + orbit * delta));
            }

            float rotation = rotationSpeed(other);
            if(rotation != 0f){
                rotationAngles.put(other.name, normalize(rotationAngle(other) + rotation * delta));
            }

            cloudTimes.put(other.name, cloudTime(other) + delta * 60f * cloudSpeed(other));
        }
    }

    private static float get(ObjectMap<String, Float> map, String key, float def){
        Float value = map.get(key);
        return value == null ? def : value;
    }

    private static float normalize(float angle){
        angle %= 360f;
        return angle < 0f ? angle + 360f : angle;
    }
}
