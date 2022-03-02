package mindustry.entities;

import mindustry.gen.*;

public interface DamageSource{
    DamageSource Unknown = new DamageSource(){
    };
    DamageSource Fire = new DamageSource(){
    };
    DamageSource LiquidReaction = new DamageSource(){
    };
    DamageSource GeneratorExplode = new DamageSource(){
    };
    DamageSource SpawnShockwave = new DamageSource(){
    };

    class Explosion implements DamageSource{
        public Explosion(DamageSource realSource){
            this.realSource = realSource;
        }

        public final DamageSource realSource;
    }

    class UnitCrash implements DamageSource{
        public UnitCrash(Unit unit){
            this.unit = unit;
        }

        public final Unit unit;
    }
}
