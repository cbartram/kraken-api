package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PacketDefinition {
    private final String name;
    private final String[] writeData;
    private final String[][] writeMethods;
    private final PacketType type;

    /**
     * Retrieves a list of parameter names required for the current {@literal @}PacketType.
     * <p>
     * The specific parameters returned depend on the value of the {@literal @}PacketType
     * associated with this packet definition. Each {@literal @}PacketType corresponds
     * to a particular game action or event and requires different parameters.
     * <p>
     * For example:
     * <ul>
     *     <li>{@literal @}PacketType.RESUME_NAMEDIALOG or {@literal @}PacketType.RESUME_STRINGDIALOG require: "length", "string".</li>
     *     <li>{@literal @}PacketType.OPHELDD requires: "selectedId", "selectedChildIndex", "selectedItemId", "destId",
     *         "destChildIndex", "destItemId".</li>
     *     <li>{@literal @}PacketType.MOVE_GAMECLICK requires: "worldPointX", "worldPointY", "ctrlDown", "5".</li>
     * </ul>
     * <p>
     * Other {@literal @}PacketType values will similarly yield different parameter lists based on their associated requirements.
     * If no matching {@literal @}PacketType is set, the method will return {@code null}.
     *
     * @return A {@literal List<String>} containing the parameter names for the current {@literal @}PacketType,
     *         or {@code null} if no parameters are defined for the current type.
     */
    public List<String> getParams() {
        List<String> params = null;
        if (type == PacketType.RESUME_NAMEDIALOG || type == PacketType.RESUME_STRINGDIALOG) {
            params = List.of("length", "string");
        }
        if (type == PacketType.OPHELDD) {
            params = List.of("selectedId", "selectedChildIndex", "selectedItemId", "destId", "destChildIndex", "destItemId");
        }
        if (type == PacketType.RESUME_COUNTDIALOG || type == PacketType.RESUME_OBJDIALOG) {
            params = List.of("var0");
        }
        if (type == PacketType.RESUME_PAUSEBUTTON) {
            params = List.of("var0", "var1");
        }
        if (type == PacketType.IF_BUTTON) {
            params = List.of("widgetId", "slot", "itemId");
        }
        if (type == PacketType.IF_SUBOP) {
            params = List.of("widgetId", "slot", "itemId", "menuIndex", "subActionIndex");
        }
        if (type == PacketType.IF_BUTTONX) {
            params = List.of("widgetId", "slot", "itemId", "opCode");
        }
        if (type == PacketType.OPLOC) {
            params = List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (type == PacketType.OPNPC) {
            params = List.of("npcIndex", "ctrlDown");
        }
        if (type == PacketType.OPPLAYER) {
            params = List.of("playerIndex", "ctrlDown");
        }
        if (type == PacketType.OPOBJ) {
            params = List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (type == PacketType.OPOBJT) {
            params = List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId",
                    "ctrlDown");
        }
        if (type == PacketType.EVENT_MOUSE_CLICK) {
            params = List.of("mouseInfo", "mouseX", "mouseY", "0");
        }
        if (type == PacketType.MOVE_GAMECLICK) {
            params = List.of("worldPointX", "worldPointY", "ctrlDown", "5");
        }
        if (type == PacketType.IF_BUTTONT) {
            params = List.of("sourceWidgetId", "sourceSlot", "sourceItemId", "destinationWidgetId",
                    "destinationSlot", "destinationItemId");
        }
        if (type == PacketType.OPLOCT) {
            params = List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId",
                    "ctrlDown");
        }
        if (type == PacketType.OPPLAYERT) {
            params = List.of("playerIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        if (type == PacketType.OPNPCT) {
            params = List.of("npcIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        if (type == PacketType.SET_HEADING) {
            params = List.of("direction");
        }

        return params;
    }
}
