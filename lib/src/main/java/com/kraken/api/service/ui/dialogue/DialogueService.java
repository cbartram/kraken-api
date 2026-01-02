package com.kraken.api.service.ui.dialogue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A service class intended for managing and interacting with various types of dialogues in the game client.
 *
 * <p>The {@code DialogueService} class provides utility methods for detecting dialogues, selecting options,
 * resuming dialogues, handling text inputs, and extracting dialogue properties such as options, headers,
 * and message content.</p>
 *
 * <p>Using this service, developers can interface with different dialogue widgets within the game client,
 * enabling automated interaction, data extraction, and execution of player actions. The methods in this class
 * operate on the client thread and ensure safe synchronization with the game's UI components.</p>
 *
 * <p>Features of this class include:</p>
 *
 * <ul>
 *   <li>Detection of active dialogues.</li>
 *   <li>Selection of dialogue options based on index or text.</li>
 *   <li>Handling of object-based and numeric-based dialogues.</li>
 *   <li>Support for "Make X" quantity-based operations.</li>
 *   <li>Retrieval of dialogue options, text content, and headers.</li>
 *   <li>Synchronization with game's client thread for secure widget interaction.</li>
 * </ul>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@literal ctx}: Context or environment within which the service operates.</li>
 *   <li>{@literal widgetPackets}: Utility responsible for interfacing with widget packets for actions.</li>
 *   <li>{@literal log}: Logging utility for debugging and information output.</li>
 * </ul>
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>Detection and validation of dialogues through {@code isDialoguePresent()}.</li>
 *   <li>Option selection by index or text with {@code selectOption(int option)} and {@code selectOption(String option)}.</li>
 *   <li>Progression of specific dialogues with {@code continueObjectDialogue(int id)} and {@code continueNumericDialogue(int value)}.</li>
 *   <li>Management of "Make X" operations via {@code makeX(int quantity)}.</li>
 *   <li>Extraction of dialogue options through {@code getDialogueOptions()}.</li>
 *   <li>Fetching header and text content with {@code getDialogueHeader()} and retrieving widget text methods.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class DialogueService {

    @Inject
    private Context ctx;

    @Inject
    private WidgetPackets widgetPackets;

    /**
     * Checks if any type of dialogue is currently present in the game client.
     * <p>
     * This method scans various widget IDs associated with different dialogue types
     * such as NPC dialogue, player dialogue, level-up messages, notifications, and more,
     * to determine if a relevant dialogue is displayed.
     * </p>
     *
     * <ul>
     * <li>Includes checks for NPC and player dialogues.</li>
     * <li>Handles level-up and notification dialogues.</li>
     * <li>Supports minigame-related and chatbox dialogues.</li>
     * <li>Accounts for "Click here to continue" prompts.</li>
     * <li>Detects option dialogues for user decisions.</li>
     * </ul>
     *
     * @return {@literal true} if any dialogue or clickable interaction is active,
     *         {@literal false} otherwise.
     */
    public boolean isDialoguePresent() {
        return ctx.runOnClientThread(() -> {
            // When the player talks
            if (ctx.getClient().getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, DialogPlayer.CONTINUE) != null) {
                return true;
            }

            if (ctx.getClient().getWidget(11, 0) != null) {
                return true;
            }

            // When an NPC talks
            if (ctx.getClient().getWidget(WidgetID.DIALOG_NPC_GROUP_ID, DialogNPC.CONTINUE) != null) {
                return true;
            }

            // When a sprite is shown
            if (ctx.getClient().getWidget(WidgetInfo.DIALOG_SPRITE) != null) {
                return true;
            }

            if (ctx.getClient().getWidget(633, 0) != null) {
                return true;
            }

            if (ctx.getClient().getWidget(229, DialogNotification.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(229, DialogNotification.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    return true;
                }
            }
            if (ctx.getClient().getWidget(229, MinigameDialog.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(229, MinigameDialog.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    return true;
                }
            }
            if(ctx.getClient().getWidget(InterfaceID.Messagebox.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(InterfaceID.Messagebox.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    return true;
                }
            }

            if(ctx.getClient().getWidget(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    return true;
                }
            }
            if(ctx.getClient().getWidget(InterfaceID.Chatbox.MES_TEXT2) != null) {
                Widget w = ctx.getClient().getWidget(InterfaceID.Chatbox.MES_TEXT2);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    return true;
                }
            }

            return ctx.getClient().getWidget(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS) != null || ctx.getClient().getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS) != null;
        });
    }

    /**
     * Selects a specific option in a dialog option group.
     *
     * <p>This method is executed on the client thread to select the desired option
     * in the user interface dialog by sending the appropriate packet to the server.</p>
     *
     * @param option the index of the option to select, typically starting from 0 for the first option.
     */
    public void selectOption(int option) {
        ctx.runOnClientThread(() -> widgetPackets.queueResumePause(UIService.pack(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS), option));
    }

    /**
     * Attempts to select an option from a dialog interface based on the provided option text.
     * This method works on a client thread to interact with widget components and identify
     * the dialog options available.
     *
     * <p>This method checks if the dialog interface exists and contains child widgets,
     * iterates over them to match the provided option text (case-insensitive), and selects
     * the desired option if found.</p>
     *
     * @param option the text of the option to be selected from the dialog.
     *               This is case-insensitive and matched against the text of the dialog options.
     * @return {@code true} if the option was successfully found and selected;
     *         {@code false} if the dialog interface does not exist, the dialog options
     *         are unavailable or empty, or if the desired option cannot be found.
     */
    public boolean selectOption(final String option) {
        return ctx.runOnClientThread(() -> {
            WidgetEntity widgetEntity = ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS);
            if(widgetEntity == null) return false;

            Widget widget = widgetEntity.raw();
            Widget[] dialogueOpt = widget.getChildren();
            if(dialogueOpt == null || dialogueOpt.length < 2) return false;

            int i = 0;
            for(Widget wid : dialogueOpt) {
                if(wid.getText().toLowerCase().contains(option.toLowerCase())) {
                    selectOption(i);
                    return true;
                }
                i++;
            }
            return false;
        });
    }

    /**
     * Resumes an object-based dialogue in the game by interacting with relevant widgets
     * and invoking a client-side script to continue the process.
     *
     * <p>
     * This method is specifically designed to handle dialogues involving object IDs and
     * ensures that the appropriate actions are executed to progress through the dialogue.
     * It queues a RESUME_OBJDIALOG packet with the given object ID and checks for specific
     * chatbox input widgets to determine whether a client script needs to be executed.
     * If either of the relevant widgets is detected, a predefined client script is invoked.
     * </p>
     *
     * <p>
     * The execution occurs on the client thread to ensure proper synchronization with the
     * game's UI and safe interaction with client methods and widgets.
     * </p>
     *
     * <ul>
     *     <li>Queues the RESUME_OBJDIALOG packet using {@literal ctx.widgetPackets.queueResumeObj}.</li>
     *     <li>Checks {@literal WidgetInfo.CHATBOX_INPUT} and {@literal WidgetInfo.CHATBOX_FULL_INPUT} for presence.</li>
     *     <li>Executes a client script ({@literal 138}) if either widget is detected.</li>
     * </ul>
     *
     * @param id The ID of the object used in the dialogue. This ID represents the object or
     *           item referenced by the dialogue option and ensures its selection and continuation.
     */
    public void continueObjectDialogue(int id) {
        ctx.runOnClientThread(() -> {
            widgetPackets.queueResumeObj(id);
            WidgetEntity widgetOne = ctx.widgets().get(WidgetInfo.CHATBOX_INPUT);
            WidgetEntity widgetTwo = ctx.widgets().get(WidgetInfo.CHATBOX_FULL_INPUT);
            if(widgetOne != null || widgetTwo != null) {
                ctx.getClient().runScript(138);
            }
        });
    }

    /**
     * Resumes a numeric dialogue in the game by interacting with the appropriate widgets and invoking
     * client-side scripts to continue the process.
     *
     * <p>
     * This method is designed to handle numeric input dialogues and ensure the correct actions are
     * executed in response. It queues a RESUME_COUNTDIALOG packet with the provided value and
     * checks for specific chatbox input widgets to determine if a client script needs to be executed.
     * If the relevant widgets are found, it invokes a client script to resume the numeric dialogue.
     * </p>
     *
     * <p>
     * The execution occurs on the client thread to maintain proper synchronization with the game's
     * UI and ensure safe interaction with client methods and widgets.
     * </p>
     *
     * <ul>
     *     <li>Queues the RESUME_COUNTDIALOG packet using {@literal ctx.widgetPackets.queueResumeCount}.</li>
     *     <li>Checks {@literal WidgetInfo.CHATBOX_INPUT} and {@literal WidgetInfo.CHATBOX_FULL_INPUT} for presence.</li>
     *     <li>Executes a client script ({@literal 138}) if either widget is detected.</li>
     * </ul>
     *
     * @param value The numeric input to resume the dialogue with. This value is typically entered by
     *              the player in a dialogue box and represents the amount or quantity the player has specified.
     */
    public void continueNumericDialogue(int value) {
         ctx.runOnClientThread(() -> {
            widgetPackets.queueResumeCount(value);
             WidgetEntity widgetOne = ctx.widgets().get(WidgetInfo.CHATBOX_INPUT);
             WidgetEntity widgetTwo = ctx.widgets().get(WidgetInfo.CHATBOX_FULL_INPUT);
             if(widgetOne != null || widgetTwo != null) {
                 ctx.getClient().runScript(138);
             }
        });
    }

    /**
     * Executes the "Make X" operation for a specified quantity.
     * <p>
     * This method interacts with the game's client thread to queue a resume/pause
     * packet for completing a quantity-based action, such as creating multiple
     * items in a crafting or production interface.
     * </p>
     * <p>
     * The operation is carried out by sending a specific widget interaction request
     * using {@code widgetPackets.queueResumePause}. The widget ID used is hardcoded
     * in the method and corresponds to a predefined interface element within the game.
     * </p>
     *
     * @param quantity The number of items or actions to perform. Must be a positive integer
     *                 representing the desired quantity for the "Make X" operation.
     */
    public void makeX(int quantity) {
        ctx.runOnClientThread(() -> widgetPackets.queueResumePause(17694734, quantity));
    }

    /**
     * Retrieves a list of dialogue options currently available in the dialogue interface.
     * <p>
     * This method interacts with the client thread to extract options from the
     * appropriate dialogue widget. It filters and returns only the non-blank options.
     * </p>
     *
     * <p>
     * The options are extracted from a pre-defined widget group and are returned
     * as a list of strings. If no options are available or if there is an issue
     * accessing the widget, an empty list is returned.
     * </p>
     *
     * @return A {@link List} of {@link String} containing the text of available dialogue options.
     *         If no options are available, the list will be empty.
     */
    public List<String> getDialogueOptions() {
        return ctx.runOnClientThread(() -> {
            List<String> options = new ArrayList<>();
            WidgetEntity widgetEntity = ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS);
            if(widgetEntity == null) return options;

            Widget widget = widgetEntity.raw();
            Widget[] dialogueOpt = widget.getChildren();
            if(dialogueOpt == null) return options;
            if(dialogueOpt.length < 2) return options;

            boolean skipZeroOpt = true;
            for(Widget w : dialogueOpt) {
                if(skipZeroOpt) {
                    skipZeroOpt = false;
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
     * Retrieves the dialogue header text currently displayed in the widget interface.
     * This method determines the source of the dialogue and provides an appropriate header.
     * <p>
     * The header may represent:
     * <ul>
     *   <li>The NPC's name if an NPC dialogue is active.</li>
     *   <li>"Player" if a player dialogue is active.</li>
     *   <li>"Select an Option" if a dialogue option selection is active.</li>
     *   <li>"unknown" if no known dialogue state is detected.</li>
     * </ul>
     *
     * @return The dialogue header text as a {@literal @}String, which identifies the current dialogue source.
     */
    public String getDialogueHeader() {
        return ctx.runOnClientThread(() -> {
            if (ctx.widgets().get(WidgetInfo.DIALOG_NPC_TEXT) != null) {
                return ctx.widgets().get(WidgetInfo.DIALOG_NPC_NAME).raw().getText();
            } else if (ctx.widgets().get(WidgetInfo.DIALOG_PLAYER_TEXT) != null) {
                return "Player";
            }
            else if (ctx.widgets().get(WidgetID.DIALOG_OPTION_GROUP_ID, DialogOption.OPTIONS) != null) {
                return "Select an Option";
            }
            return "Unknown";
        });
    }

    /**
     * Retrieves the text from the currently active dialogue widget if available. This method checks multiple
     * possible widgets for dialogue text, including NPC text, player text, sprite-based text, and other specific
     * dialogue-related interfaces.
     *
     * <p>The method will return the first non-null and non-empty text identified from the following widgets:
     * <ul>
     *   <li>{@literal WidgetInfo.DIALOG_NPC_TEXT}</li>
     *   <li>{@literal WidgetInfo.DIALOG_PLAYER_TEXT}</li>
     *   <li>{@literal WidgetInfo.DIALOG_SPRITE_TEXT}</li>
     *   <li>{@literal ctx.widgets().get(11, 2)}</li>
     *   <li>{@literal ctx.widgets().get(229, MinigameDialog.TEXT)}</li>
     *   <li>{@literal ctx.widgets().get(229, DialogNotification.TEXT)}</li>
     *   <li>{@literal ctx.widgets().get(InterfaceID.Messagebox.TEXT)}</li>
     * </ul>
     *
     * <p>If no dialogue text is found across any of these widgets, an empty string will be returned.</p>
     *
     * @return A {@literal String} containing the dialogue text from the active widget, or an empty string if no text is available.
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
     * Attempts to continue an active dialogue in the game by interacting with various dialogue widgets.
     * <p>
     * This method works by checking for the presence of specific dialogue widgets representing NPC dialogues,
     * player dialogues, notifications, level-up screens, chatbox dialogues, and other message boxes. If any
     * such widget is found and recognized, the method sends a resume or pause command to the appropriate widget
     * to continue the dialogue. The logic prioritizes widgets in a specific order to handle varying dialogue types.
     * </p>
     *
     * <p>
     * The method is executed on the client thread to ensure proper interaction with the game's UI.
     * It returns {@code true} if a valid dialogue widget was found and the "continue" action was successfully triggered.
     * </p>
     *
     * <ul>
     *     <li>If an NPC dialogue widget is detected in the {@code WidgetID.DIALOG_NPC_GROUP_ID} group, it triggers the "continue" action.</li>
     *     <li>Handles player dialogues, sprite dialogues, level-up dialogues, and other known specific IDs.</li>
     *     <li>Checks text values, such as "Click here to continue," in certain widgets to verify the necessity of sending a command.</li>
     * </ul>
     *
     * @return {@code true} if a valid dialogue widget was interacted with to continue the dialogue,
     *         {@code false} if no applicable dialogue widget was found or interacted with.
     */
    public boolean continueDialogue() {
        Client client = ctx.getClient();
        return ctx.runOnClientThread(() -> {
            // When an NPC is speaking
            if (ctx.getClient().getWidget(WidgetID.DIALOG_NPC_GROUP_ID, DialogNPC.CONTINUE) != null) {
                widgetPackets.queueResumePause(UIService.pack(WidgetID.DIALOG_NPC_GROUP_ID, DialogNPC.CONTINUE), -1);
                return true;
            }

            if (ctx.getClient().getWidget(633, 0) != null) {
                widgetPackets.queueResumePause(UIService.pack(633, 0), -1);
                return true;
            }

            // When the player speaks
            if (ctx.getClient().getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, DialogPlayer.CONTINUE) != null) {
                int id = UIService.pack(WidgetID.DIALOG_PLAYER_GROUP_ID, DialogPlayer.CONTINUE);
                widgetPackets.queueResumePause(id, -1);
                return true;
            }

            // When a sprite is shown
            if (ctx.getClient().getWidget(WidgetInfo.DIALOG_SPRITE) != null) {
                int id = UIService.pack(11, 0);
                widgetPackets.queueResumePause(id, 0);
                return true;
            }

            if (ctx.getClient().getWidget(11, 0) != null) {
                int id = UIService.pack(11, DialogSprite2.CONTINUE);
                widgetPackets.queueResumePause(id, -1);
                return true;
            }
            if (ctx.getClient().getWidget(229, MinigameDialog.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(229, MinigameDialog.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    int id = UIService.pack(229, MinigameDialog.CONTINUE);
                    widgetPackets.queueResumePause(id, -1);
                    return true;
                }
            }

            if (ctx.getClient().getWidget(229, DialogNotification.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(229, DialogNotification.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    int id = UIService.pack(229, DialogNotification.CONTINUE);
                    widgetPackets.queueResumePause(id, -1);
                    return true;
                }
            }

            if (ctx.getClient().getWidget(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    int id = UIService.pack(WidgetID.LEVEL_UP_GROUP_ID, LevelUp.CONTINUE);
                    widgetPackets.queueResumePause(id, -1);
                    return true;
                }
            }

            if(ctx.getClient().getWidget(InterfaceID.Messagebox.CONTINUE) != null) {
                Widget w = ctx.getClient().getWidget(InterfaceID.Messagebox.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    widgetPackets.queueResumePause(InterfaceID.Messagebox.CONTINUE, -1);
                    return true;
                }
            }
            if(ctx.getClient().getWidget(InterfaceID.Chatbox.MES_TEXT2) != null) {
                Widget w = ctx.getClient().getWidget(InterfaceID.Chatbox.MES_TEXT2);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue")) {
                    client.runScript(101, 1);
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Checks if a specific dialogue option is present in the list of available options.
     * <p>
     * The search is case-insensitive and supports partial matches. If the provided option
     * text matches any part of any dialogue option, the method will return true.
     * </p>
     *
     * @param option The text of the dialogue option to search for.
     *               This parameter is case-insensitive and partial matches are supported.
     * @return {@code true} if the option is found in the list of available dialogue options,
     *         {@code false} otherwise.
     */
    public boolean isOptionPresent(String option) {
        for(String opt : getDialogueOptions()) {
            if(opt.toLowerCase().contains(option.toLowerCase())) {
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
