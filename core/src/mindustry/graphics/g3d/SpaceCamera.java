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
    /** Whether planet clouds are rendered in this scene. */
    public boolean clouds = true;
    /** Orbit/rotation speeds in degrees per second. 0 freezes the body. */
    public float orbitSpeed = 0f, rotationSpeed = 0f;
    /** Cloud rotation multiplier. 1 is the planet's normal cloud speed. */
    public float cloudSpeed = 1f;
    /** Accumulated cloud rotation clock, in the same units as {@code Time.globalTime}. */
    public float cloudTime;
    /** If true, the map background keeps advancing this camera after it is saved. */
    public boolean updateInGame = false;
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
        cloudTime = Time.globalTime;

        for(Planet other : content.planets()){
            if(other.solarSystem == body.solarSystem){
                if(other.parent != null) orbitAngles.put(other.name, other.getOrbitAngle());
                rotationAngles.put(other.name, other.getRotation());
            }
        }
    }

    /** Ensures every body in the scene has a saved angle, without overwriting existing values. */
    public void fillAngles(Planet body){
        for(Planet other : content.planets()){
            if(other.solarSystem == body.solarSystem){
                if(other.parent != null && !orbitAngles.containsKey(other.name)) orbitAngles.put(other.name, other.getOrbitAngle());
                if(!rotationAngles.containsKey(other.name)) rotationAngles.put(other.name, other.getRotation());
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

    public boolean hasMovement(){
        return orbitSpeed != 0f || rotationSpeed != 0f || (clouds && cloudSpeed != 0f);
    }

    /** Advances the saved orbit/rotation angles by delta seconds. */
    public void update(float delta){
        if(delta <= 0f) return;

        cloudTime += delta * 60f * cloudSpeed;

        Planet body = planet();
        for(Planet other : content.planets()){
            if(other.solarSystem != body.solarSystem) continue;

            if(other.parent != null && orbitSpeed != 0f){
                orbitAngles.put(other.name, normalize(orbitAngle(other) + orbitSpeed * delta));
            }

            if(rotationSpeed != 0f){
                rotationAngles.put(other.name, normalize(rotationAngle(other) + rotationSpeed * delta));
            }
        }
    }

    private static float normalize(float angle){
        angle %= 360f;
        return angle < 0f ? angle + 360f : angle;
    }
}
