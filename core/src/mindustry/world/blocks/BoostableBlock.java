package mindustry.world.blocks;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.*;


public abstract class BoostableBlock extends Block{
    public final int timerUse = timers++;
    /** Whether this block accepts coolant. */
	public boolean acceptCoolant = false;
    /** The amount of coolant a block uses. */
    public float coolantUse = 0.2f;
	/** The base efficiency of a coolant. */
	public float coolantMultiplier = 5f;
	/** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;
    /** Whether this block accepts coolant. */
    public boolean acceptItemBooster = false;
    /** The use time of an item booster. */
	public float useTime = 60f;

	public BoostableBlock(String name){
		super(name);
	}

	@Override
	public void setStats(){
        super.setStats();

        if(acceptCoolant){
            //TODO: better stats
            stats.add(BlockStat.booster, new BoosterListValue(1f, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, true, l -> consumes.liquidfilters.get(l.id)));
        }
    }

    @Override
    public void init(){
        if(acceptCoolant && !consumes.has(ConsumeType.liquid)){
            hasLiquids = true;
            consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, coolantUse)).update(false).boost();
        }

        super.init();
    }

	public class BoostableBlockBuild extends Building{
		public float currentCoolantBoost = 1f;
        public float currentItemBoost = 0f;

		@Override
        public void updateTile(){
            if(acceptItemBooster) {
                currentItemBoost = Mathf.lerpDelta(currentItemBoost, Mathf.num(cons.optionalValid()), 0.1f);
                updateItemBooster();
            }
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            if(acceptCoolant && liquids.currentAmount() <= 0.001f){
                //TODO: better events
                Events.fire(Trigger.turretCool);
            }

            super.handleLiquid(source, liquid, amount);
        }

        protected void updateItemBooster(){
            if(cons.optionalValid() && timer(timerUse, useTime) && efficiency() > 0){
                consume();
            }
        }

        protected void updateCooling(){
            float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

            Liquid liquid = liquids.current();

            float used = Math.min(liquids.get(liquid), maxUsed * Time.delta);
            currentCoolantBoost = 1f + (used * liquid.heatCapacity * coolantMultiplier);
            liquids.remove(liquid, used);

            if(Mathf.chance(0.06 * used)){
                coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
            }
        }
	}
}