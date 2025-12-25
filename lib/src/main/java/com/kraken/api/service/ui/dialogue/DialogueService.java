package com.kraken.api.service.ui.dialogue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class DialogueService {

    @Inject
    private Context ctx;

	/**
     * Retrieves the header of the current dialogue, which may indicate the speaker or context.
     *
     * @return The dialogue header as a String. Possible values include NPC names, "Player", "Select an Option", or "UNKNOWN".
     */
    public String getDialogueHeader() {
        return ctx.runOnClientThread(() -> {
            if (ctx.widgets().get(WidgetInfo.DIALOG_NPC_TEXT) != null) {
                return ctx.widgets().get(WidgetInfo.DIALOG_NPC_NAME).raw().getText();
            }
            else if (ctx.widgets().get(WidgetInfo.DIALOG_PLAYER_TEXT) != null) {
                return "Player";
            }
            else if (ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS) != null) {
                return "Select an Option";
            }
            return "UNKNOWN";
        });
    }

    /**
     * Retrieves the main text content of the current dialogue.
     *
     * @return The dialogue text as a String. If no dialogue is present, returns an empty string.
     */
    public String getDialogueText() {
        return ctx.runOnClientThread(() -> {
            if (ctx.widgets().get(WidgetInfo.DIALOG_NPC_TEXT) != null) {
                return ctx.widgets().get(WidgetInfo.DIALOG_NPC_TEXT).raw().getText();
            } else if (ctx.widgets().get(WidgetInfo.DIALOG_PLAYER_TEXT) != null) {
                return ctx.widgets().get(WidgetInfo.DIALOG_PLAYER_TEXT).raw().getText();
            } else if (ctx.widgets().get(WidgetInfo.DIALOG_SPRITE_TEXT) != null) {
                return ctx.widgets().get(WidgetInfo.DIALOG_SPRITE_TEXT).raw().getText();
            } else if (ctx.widgets().get(11,2) != null) {
                return ctx.widgets().get(11, 2).raw().getText();
            } else if (ctx.widgets().get(229, MinigameDialog.TEXT) != null) {
                return ctx.widgets().get(229, MinigameDialog.TEXT).raw().getText();
            } else if (ctx.widgets().get(229, DialogNotification.TEXT) != null) {
                return ctx.widgets().get(229, DialogNotification.TEXT).raw().getText();
            } else if(ctx.widgets().get(InterfaceID.Messagebox.TEXT) != null) {
                return ctx.widgets().get(InterfaceID.Messagebox.TEXT).raw().getText();
            }
            return "";
        });
    }



    /**
     * Checks if any dialogue is currently present on the screen.
     *
     * @return true if a dialogue is present, false otherwise.
     */
    public boolean dialoguePresent() {
        return ctx.runOnClientThread(() -> {
            if (ctx.widgets().get(WidgetID.DIALOG_NPC_GROUP_ID, DialogNPC.CONTINUE) != null) {
                return true;
            }
            if (ctx.widgets().get(633, 0) != null) {
                return true;
            }
            if (ctx.widgets().get(WidgetID.DIALOG_PLAYER_GROUP_ID, DialogPlayer.CONTINUE) != null) {
                return true;
            }
            if (ctx.widgets().get(WidgetInfo.DIALOG_SPRITE) != null) {
                return true;
            }
            if (ctx.widgets().get(11, 0) != null) {
                return true;
            }
            if (ctx.widgets().get(229, MinigameDialog.CONTINUE) != null) {
                Widget w = ctx.widgets().get(229, MinigameDialog.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            if (ctx.widgets().get(229, DialogNotification.CONTINUE) != null) {
                Widget w = ctx.widgets().get(229, DialogNotification.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            if(ctx.widgets().get(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE) != null) {
                Widget w = ctx.widgets().get(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            if(ctx.widgets().get(InterfaceID.Messagebox.CONTINUE) != null) {
                Widget w = ctx.widgets().get(InterfaceID.Messagebox.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            if(ctx.widgets().get(InterfaceID.Chatbox.MES_TEXT2) != null) {
                Widget w = ctx.widgets().get(InterfaceID.Chatbox.MES_TEXT2).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            return ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS) != null || ctx.widgets().get(WidgetInfo.DIALOG_OPTION_OPTIONS) != null;
        });
    }

    /**
     * Selects a dialogue option based on its index.
     *
     * @param option The index of the option to select (0-based).
     */
    public void selectOption(int option) {
        int id = (WidgetID.DIALOG_OPTION_GROUP_ID << 16) | DialogOption.OPTIONS;
        resumePause(id, option);
    }

    /**
     * Selects a dialogue option based on its text.
     *
     * @param option The text of the option to select. Case-insensitive and partial matches are supported.
     * @return true if the option was found and selected, false otherwise.
     */
    public boolean selectOption(String option) {
        return ctx.runOnClientThread(() -> {
            WidgetEntity widgetEntity = ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS);
            if(widgetEntity == null)
                return false;
            Widget widget = widgetEntity.raw();
            Widget[] dialogOption1kids = widget.getChildren();
            if(dialogOption1kids == null)
                return false;
            if(dialogOption1kids.length < 2)
                return false;
            int i = 0;
            for(Widget w : dialogOption1kids) {
                if(w.getText().toLowerCase().contains(option.toLowerCase())) {
                    selectOption(i);
                    return true;
                }
                i++;
            }
            return false;
        });
    }

    /**
     * Resumes an object dialogue
     *
     * @param id The dialogue object ID
     */
    public void resumeObjectDialogue(int id) {
        Client client = ctx.getClient();
        ctx.runOnClientThread(() -> {
            client.getPacketWriter().resumeObjectDialoguePacket(id);
            ClientScriptAPI.closeNumericInputDialogue();
        });
    }

    /**
     * Resumes a numeric dialogue
     *
     * @param value The number to input
     */
    public void resumeNumericDialogue(int value) {
        Client client = ctx.getClient();
         ctx.runOnClientThread(() -> {
            client.getPacketWriter().resumeCountDialoguePacket(value);
            ClientScriptAPI.closeNumericInputDialogue();
        });
    }

    /**
     * Sends a resume/pause for a specific widget and option index.
     *
     * @param widgetId    The ID of the widget to interact with.
     * @param optionIndex The index of the option to select within the widget.
     */
    public void resumePause(int widgetId, int optionIndex) {
        ctx.runOnClientThread(() -> ctx.getClient().getPacketWriter().resumePauseWidgetPacket(widgetId, optionIndex));
    }

    /**
     * Sends a "make X" dialogue input with the specified quantity.
     *
     * @param quantity The quantity to input in the "make X" dialogue.
     */
    public void makeX(int quantity) {
        ctx.runOnClientThread(() -> {
            ctx.getClient().getPacketWriter().resumePauseWidgetPacket(17694734, quantity);
        });
    }

    /**
     * Retrieves a list of available dialogue options.
     *
     * @return A list of dialogue option texts. If no options are present, returns an empty list.
     */
    public List<String> getOptions() {
        return ctx.runOnClientThread(() -> {
            List<String> options = new ArrayList<>();
            WidgetEntity widgetEntity = ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS);
            if(widgetEntity == null)
                return options;
            Widget widget = widgetEntity.raw();
            Widget[] dialogOption1kids = widget.getChildren();
            if(dialogOption1kids == null)
                return options;
            if(dialogOption1kids.length < 2)
                return options;
            boolean skipZero = true;
            for(Widget w : dialogOption1kids) {
                if(skipZero) {
                    skipZero = false;
                    continue;
                } else if(w.getText().isBlank()) {
                    continue;
                }
                options.add(w.getText());
            }
            return options;
        });
    }

     /**
     * Continues the current dialogue by clicking the "Continue" button if present.
     *
     * @return true if a continue action was performed, false otherwise.
     */
    public boolean continueDialogue() {
        Client client = ctx.getClient();
        return ctx.runOnClientThread(() -> {
            if (ctx.widgets().get(WidgetID.DIALOG_NPC_GROUP_ID, DialogNPC.CONTINUE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG_NPC_CONTINUE.getId(), -1);
                return true;
            }
            if (ctx.widgets().get(633, 0) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfo.PACK(633, 0), -1);
                return true;
            }
            if (ctx.widgets().get(WidgetID.DIALOG_PLAYER_GROUP_ID, WidgetInfoExtended.DialogPlayer.CONTINUE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG_PLAYER_CONTINUE.getId(), -1);
                return true;
            }
            if (ctx.widgets().get(WidgetInfo.DIALOG_SPRITE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfo.DIALOG_SPRITE.getId(), 0);
                return true;
            }
            if (ctx.widgets().get(11, 0) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG2_SPRITE_CONTINUE.getId(), -1);
                return true;
            }
            if (ctx.widgets().get(229, MinigameDialog.CONTINUE) != null) {
                Widget w = ctx.widgets().get(229, MinigameDialog.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.MINIGAME_DIALOG_CONTINUE.getId(), -1);
                    return true;
                }
            }
            if (ctx.widgets().get(229, DialogNotification.CONTINUE) != null) {
                Widget w = ctx.widgets().get(229, DialogNotification.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG_NOTIFICATION_CONTINUE.getId(), -1);
                    return true;
                }
            }
            if (ctx.widgets().get(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE) != null) {
                Widget w = ctx.widgets().get(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.LEVEL_UP_CONTINUE.getId(), -1);
                    return true;
                }
            }
            if(ctx.widgets().get(InterfaceID.Messagebox.CONTINUE) != null) {
                Widget w = ctx.widgets().get(InterfaceID.Messagebox.CONTINUE).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.Messagebox.CONTINUE, -1);
                    return true;
                }
            }
            if(ctx.widgets().get(InterfaceID.Chatbox.MES_TEXT2) != null) {
                Widget w = ctx.widgets().get(InterfaceID.Chatbox.MES_TEXT2).raw();
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    client.runScript(101, 1);
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Checks if a specific dialogue option is present.
     *
     * @param option The text of the option to check for. Case-insensitive and partial matches are supported.
     * @return true if the option is present, false otherwise.
     */
    public boolean optionPresent(String option) {
        List<String> options = getOptions();
        for(String s : options) {
            if(s.toLowerCase().contains(option.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    
    static class DialogOption {
        static final int OPTIONS = 1;
    }

    static class DialogNPC {
        static final int HEAD_MODEL = 2;
        static final int NAME = 4;
        static final int CONTINUE = 5;
        static final int TEXT = 6;
    }

    static class DialogPlayer {
        static final int HEAD_MODEL = 2;
        static final int NAME = 4;
        static final int CONTINUE = 5;
        static final int TEXT = 6;
    }

    static class DialogSprite2 {
        static final int SPRITE1 = 1;
        static final int TEXT = 2;
        static final int SPRITE2 = 3;
        static final int CONTINUE = 4;
    }

    static class MinigameDialog {
        static final int TEXT = 1;
        static final int CONTINUE = 2;
    }

    static class DialogNotification {
        static final int TEXT = 0;
        static final int CONTINUE = 1;
    }

    static class LevelUp {
        static final int SKILL = 1;
        static final int LEVEL = 2;
        static final int CONTINUE = 3;
    }
}
