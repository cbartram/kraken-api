package com.kraken.api.core.packet.debug;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.model.PacketDefFactory;
import com.kraken.api.core.packet.model.PacketDefinition;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class PacketSpy {

    @Inject
    private PacketDefFactory packetDefFactory;

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        int param0 = event.getParam0();
        int param1 = event.getParam1();
        int id = event.getId();
        int itemId = event.getItemId();
        int itemOp = event.getItemOp();
        String target = event.getMenuTarget();
        String option = event.getMenuOption();
        MenuAction action = event.getMenuAction();

        log.info("[PacketSpy] Opt: '{}' Tgt: '{}' | Action: {} | ID: {} | P0: {} | P1: {} | ItemID: {} | ItemOp: {}",
                option, target, action, id, param0, param1, itemId, itemOp);

        PacketDefinition def = getDefinitionForAction(action, packetDefFactory);
        if (def != null) {
            log.info("[PacketSpy] Packet Definition: {} (Type: {})", def.getName(), def.getType());
        } else {
            log.debug("[PacketSpy] No mapping found for action: {}", action);
        }
    }

    public PacketDefinition getDefinitionForAction(MenuAction action, PacketDefFactory factory) {
        switch (action) {
            // -------------------------------------------------
            // 1. GAME OBJECTS (Trees, Banks, Doors) -> OPLOC
            // -------------------------------------------------
            case GAME_OBJECT_FIRST_OPTION:  return factory.getOpLoc(1);
            case GAME_OBJECT_SECOND_OPTION: return factory.getOpLoc(2);
            case GAME_OBJECT_THIRD_OPTION:  return factory.getOpLoc(3);
            case GAME_OBJECT_FOURTH_OPTION: return factory.getOpLoc(4);
            case GAME_OBJECT_FIFTH_OPTION:  return factory.getOpLoc(5);

            // -------------------------------------------------
            // 2. GROUND ITEMS (Loot on floor) -> OPOBJ
            // -------------------------------------------------
            case GROUND_ITEM_FIRST_OPTION:  return factory.getOpObj(1);
            case GROUND_ITEM_SECOND_OPTION: return factory.getOpObj(2);
            case GROUND_ITEM_THIRD_OPTION:  return factory.getOpObj(3);
            case GROUND_ITEM_FOURTH_OPTION: return factory.getOpObj(4);
            case GROUND_ITEM_FIFTH_OPTION:  return factory.getOpObj(5);

            // -------------------------------------------------
            // 3. NPCS (Monsters, Bankers) -> OPNPC
            // -------------------------------------------------
            case NPC_FIRST_OPTION:  return factory.getOpNpc(1);
            case NPC_SECOND_OPTION: return factory.getOpNpc(2);
            case NPC_THIRD_OPTION:  return factory.getOpNpc(3);
            case NPC_FOURTH_OPTION: return factory.getOpNpc(4);
            case NPC_FIFTH_OPTION:  return factory.getOpNpc(5);

            // -------------------------------------------------
            // 4. PLAYERS (Trade, Follow, Attack) -> OPPLAYER
            // -------------------------------------------------
            case PLAYER_FIRST_OPTION:   return factory.getOpPlayer(1);
            case PLAYER_SECOND_OPTION:  return factory.getOpPlayer(2);
            case PLAYER_THIRD_OPTION:   return factory.getOpPlayer(3);
            case PLAYER_FOURTH_OPTION:  return factory.getOpPlayer(4);
            case PLAYER_FIFTH_OPTION:   return factory.getOpPlayer(5);
            case PLAYER_SIXTH_OPTION:   return factory.getOpPlayer(6);
            case PLAYER_SEVENTH_OPTION: return factory.getOpPlayer(7);
            case PLAYER_EIGHTH_OPTION:  return factory.getOpPlayer(8);

            // -------------------------------------------------
            // 5. TARGETED ACTIONS (Using Item/Spell on X) -> OPT...
            // -------------------------------------------------
            case WIDGET_TARGET_ON_GAME_OBJECT: return factory.getOpLocT();
            case WIDGET_TARGET_ON_NPC:         return factory.getOpNpcT();
            case WIDGET_TARGET_ON_PLAYER:      return factory.getOpPlayerT();
            case WIDGET_TARGET_ON_GROUND_ITEM: return factory.getOpObjT();
            case WIDGET_TARGET_ON_WIDGET:      return factory.getIfButtonT();

            // -------------------------------------------------
            // 6. WIDGET/INTERFACE ACTIONS
            // -------------------------------------------------
            case CC_OP:
            case CC_OP_LOW_PRIORITY:
            case WIDGET_TYPE_1:
                return factory.getIfButtonX();

            // -------------------------------------------------
            // 7. MOVEMENT & SPECIAL
            // -------------------------------------------------
            case WALK:
                return factory.getMoveGameClick();

            // -------------------------------------------------
            // 8. DIALOGUES
            // -------------------------------------------------
            case WIDGET_CONTINUE:
                return factory.getResumePausebutton();

            default:
                return null;
        }
    }
}
