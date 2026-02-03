package com.kraken.api.service.magic;

import com.kraken.api.Context;
import com.kraken.api.service.magic.rune.Rune;
import com.kraken.api.service.magic.spellbook.Spellbook;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;

import java.util.Map;

public interface CastableSpell {
    int getAction();
    int getLevel();
    int getWidget();
    int getIndex();
    Spellbook getSpellbook();
    String getName();
    Map<Rune, Integer> getRuneRequirement();

    default boolean isCastable() {
        Context ctx = RuneLite.getInjector().getInstance(Context.class);
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            EnumComposition spellsEnum = client.getEnum(getSpellbook().getEnumCompositionIndex());
            int spellItemId = spellsEnum.getIntValue(getIndex());

            ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
            int weapon = -1;
            int shield = -1;

            if (equipment != null) {
                Item weaponItem = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
                Item shieldItem = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
                if (weaponItem != null) weapon = weaponItem.getId();
                if (shieldItem != null) shield = shieldItem.getId();
            }

            // Script checks that the spell being cast has the right runes to cast the spell
            client.runScript(2620, spellItemId, weapon, shield);
            return client.getIntStack()[client.getIntStackSize() - 1] == 1;
        });
    }
}
