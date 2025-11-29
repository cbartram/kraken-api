package com.kraken.api.example.tests;

import com.kraken.api.query.equipment.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;

import javax.inject.Inject;

@Slf4j
public class EquipmentServiceTest extends BaseApiTest {

    @Inject
    private EquipmentService equipmentService;

    @Override
    protected boolean runTest() {
        boolean testsPassed = true;

        int packet = Integer.parseInt(config.equipIdPacket());

        if(config.headSlotUnequip()) {
            testsPassed &= equipmentService.remove(EquipmentInventorySlot.HEAD);
        }

        testsPassed &= equipmentService.wield(packet);
        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Equipment Service";
    }
}
