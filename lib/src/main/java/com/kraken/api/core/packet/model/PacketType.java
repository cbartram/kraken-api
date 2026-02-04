package com.kraken.api.core.packet.model;

/**
 * Enum containing references to various packet types sent by the game client.
 * <p>
 * - IF Button types are sent when any of the normal buttons on newer interfaces are clicked
 * - Resume pause are sent when a player interacts with a Dialogue like talking to an NPC "Click here to Continue"
 * - Resume counts are sent when the dialogue asks for a number like in clue scroll steps
 * - Resume string is sent when the dialogue asks for a string like "whats your clan name"?
 * - Move game click is the packet that the client sends upon clicking on a game square to move towards it.
 * - Event Mouse click packets are written whenever the player clicks anywhere on their client, whether it be dead space, or any entity in-game
 */
public enum PacketType {
    OPHELDD,
    RESUME_COUNTDIALOG,
    RESUME_PAUSEBUTTON,
    RESUME_NAMEDIALOG,
    RESUME_STRINGDIALOG,
    RESUME_OBJDIALOG,
    IF_BUTTON,
    IF_SUBOP,
    IF_BUTTONX,
    OPNPC,
    OPPLAYER,
    OPOBJ,
    OPLOC,
    MOVE_GAMECLICK,
    EVENT_MOUSE_CLICK,
    IF_BUTTONT,
    OPNPCT,
    OPPLAYERT,
    OPOBJT,
    OPLOCT,
    SET_HEADING
}
