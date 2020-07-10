package power;

/** Tests for direct power consumers. */
public class DirectConsumerTests extends PowerTestFixture{
    //TODO reimplement
/*
    @Test
    void noPowerRequestedWithNoItems(){
        testUnitFactory(0, 0, 0.08f, 0.08f, 1f);
    }

    @Test
    void noPowerRequestedWithInsufficientItems(){
        testUnitFactory(30, 0, 0.08f, 0.08f, 1f);
        testUnitFactory(0, 30, 0.08f, 0.08f, 1f);
    }

    @Test
    void powerRequestedWithSufficientItems(){
        testUnitFactory(30, 30, 0.08f, 0.08f, 1.0f);
    }

    void testUnitFactory(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction){
        Tile consumerTile = createFakeTile(0, 0, new UnitFactory("fakefactory"){{
            entityType = UnitFactoryEntity::new;
            unitType = UnitTypes.spirit;
            produceTime = 60;
            consumes.power(requestedPower);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30));
        }});
        consumerTile.entity.items.add(Items.silicon, siliconAmount);
        consumerTile.entity.items.add(Items.lead, leadAmount);

        Tile producerTile = createFakeTile(2, 0, createFakeProducerBlock(producedPower));
        producerTile.<PowerGenerator.GeneratorEntity>ent().productionEfficiency = 1f;

        PowerGraph graph = new PowerGraph();
        graph.add(producerTile.entity);
        graph.add(consumerTile.entity);

        consumerTile.entity.update();
        graph.update();

        assertEquals(expectedSatisfaction, consumerTile.entity.power.status);
    }*/
}
