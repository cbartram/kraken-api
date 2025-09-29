package com.kraken.api.interaction.reflect;

import com.kraken.api.core.AbstractService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
public class ReflectionService extends AbstractService {

    @Setter
    private static String invokeMenuActionClassName;

    @Setter
    private static String invokeMenuActionMethodName;

    @Setter
    private static int invokeMenuActionJunkValue;

    @Setter
    @Getter
    private static String sceneSelectedXClassName;

    @Setter
    @Getter
    private static String sceneSelectedXFieldName;

    @Setter
    @Getter
    private static String sceneSelectedYClassName;

    @Setter
    @Getter
    private static String sceneSelectedYFieldName;

    @Setter
    @Getter
    private static String checkClickClassName;

    @Setter
    @Getter
    private static String checkClickFieldName;

    @Setter
    @Getter
    private static String viewportWalkingClassName;

    @Setter
    @Getter
    private static String viewportWalkingFieldName;

    @Setter
    private static String selectedSpellWidgetClassName;

    @Setter
    private static String selectedSpellWidgetFieldName;

    @Setter
    private static int selectedSpellWidgetMultiplier;

    @Setter
    private static String selectedSpellChildIndexClassName;

    @Setter
    private static String selectedSpellChildIndexFieldName;

    @Setter
    private static int selectedSpellChildIndexMultiplier;

    @Setter
    private static String selectedSpellItemIDClassName;

    @Setter
    private static String selectedSpellItemIDFieldName;

    @Setter
    private static int selectedSpellItemIDMultiplier;

    @Setter
    private static String menuOptionsCountClassName;

    @Setter
    private static String menuOptionsCountFieldName;

    @Setter
    private static int menuOptionsCountMultiplier;

    @Setter
    private static String menuIdentifiersClassName;

    @Setter
    private static String menuIdentifiersFieldName;

    @Setter
    private static String menuItemIdsClassName;

    @Setter
    private static String menuItemIdsFieldName;

    @Setter
    private static String menuOptionsClassName;

    @Setter
    private static String menuOptionsFieldName;

    @Setter
    private static String menuParam0ClassName;

    @Setter
    private static String menuParam0FieldName;

    @Setter
    private static String menuParam1ClassName;

    @Setter
    private static String menuParam1FieldName;

    @Setter
    private static String menuTargetsClassName;

    @Setter
    private static String menuTargetsFieldName;

    @Setter
    private static String menuTypesClassName;

    @Setter
    private static String menuTypesFieldName;

    public Class<?> getClass(String className) {
        Class<?> clazz;

        try {
            clazz = client.getClass().getClassLoader().loadClass(className);
        } catch (Exception e) {
            log.error("Unable to load class \"{}\". Check if obfuscated class name is correct.", className, e);
            return null;
        }

        return clazz;
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        Field field;

        if (clazz == null) {
            return null;
        }

        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            log.error("Unable to get declared field \"{}\". Check if obfuscated field name is correct.", fieldName, e);
            return null;
        }

        return field;
    }

    public Field getField(String className, String fieldName) {
        Class<?> clazz = getClass(className);
        return getField(clazz, fieldName);
    }

    public static Object getFieldObjectValue(Field field, Object objectWithField, String errorMsg) {
        if (field == null || objectWithField == null) {
            return null;
        }

        try {
            field.setAccessible(true);
            Object obj = field.get(objectWithField);
            field.setAccessible(false);
            return obj;
        } catch (Exception e) {
            log.error(errorMsg, e);
            return null;
        }
    }

    public static int getFieldIntValue(Field field, Object objectWithField, int multiplier, String errorMsg) {
        if (field == null || objectWithField == null) {
            return -1;
        }

        try {
            field.setAccessible(true);
            int value = field.getInt(objectWithField) * multiplier;
            field.setAccessible(false);
            return value;
        } catch (Exception e) {
            log.error(errorMsg, e);
            return -1;
        }
    }

    public static void setFieldIntValue(Field field, Object objectWithField, int valueToSet, String errorMsg) {
        if (field == null || objectWithField == null) {
            return;
        }

        try {
            field.setAccessible(true);
            field.setInt(objectWithField, valueToSet);
            field.setAccessible(false);
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    public static void setFieldBooleanValue(Field field, Object objectWithField, boolean valueToSet, String errorMsg) {
        if (field == null) {
            return;
        }

        try {
            field.setAccessible(true);
            field.setBoolean(objectWithField, valueToSet);
            field.setAccessible(false);
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    public static void setFieldIntArrayValue(Field field, Object objectWithField, int index, int valueToSet, String errorMsg) {
        if (field == null) {
            return;
        }

        try {
            field.setAccessible(true);
            Object fieldArray = field.get(objectWithField);
            Array.setInt(fieldArray, index, valueToSet);
            field.set(objectWithField, fieldArray);
            field.setAccessible(false);
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    public static void setFieldObjectArrayValue(Field field, Object objectWithField, int index, Object valueToSet, String errorMsg) {
        if (field == null) {
            return;
        }

        try {
            field.setAccessible(true);
            Object optionsArray = field.get(objectWithField);
            Array.set(optionsArray, index, valueToSet);
            field.set(objectWithField, optionsArray);
            field.setAccessible(false);
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    /**
     * Invokes a menu action like toggling a prayer on/off or interacting with something via a MenuEntry using reflection. Jagex
     * includes a "Junk Value" parameter in their methods to make reflection harder. Without this parameter's exact value we cannot invoke a method correctly and
     * this junk value changes each client update. The junk value is determined based on the json value from the hooks when the client starts.
     * @param param0 Parameter 0 for the menu action. This can be the X coordinate of the mouse or a different value depending on what action is taken.
     * @param param1 Parameter 1 for the menu action. This can be the Y coordinate of the mouse or a different value depending on what action is taken.
     * @param opcode This is the operation code for the action you are trying to perform. Each action has a different opcode. For example, the opcode for "Use" is 38, "Cast" is 25, "Drop" is 16, etc.
     *               These opcodes come from the {@code MenuAction} class. For example: MenuAction.GROUND_ITEM_FOURTH_OPTION;
     * @param identifier  Usually the same as the item id
     * @param worldViewId The world view id is usually -1. This is the id for the WorldView.
     * @param itemId The item id is the id of the item you are interacting with. This can be the id of an item, NPC, player, or location.
     * @param option Sometimes known as the "action". This is the text that appears in the menu option i.e. "Take", "Examine", "Cast", "Drop", etc.
     * @param target The target is the name of the object you are interacting with. This can be the name of an item, NPC, player, or location.
     * @param x The x coordinate for the invocation
     * @param y The y coordinate for the invocation
     */
    public void invokeMenuAction(int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int x, int y) {
        Class<?> clazz = getClass(invokeMenuActionClassName);
        Method method;
        boolean isJunkValueAByte = invokeMenuActionJunkValue < 128 && invokeMenuActionJunkValue >= -128;
        boolean isJunkValueShort = invokeMenuActionJunkValue < 32767 && invokeMenuActionJunkValue >= -32767;

        if (clazz == null) {
            return;
        }

        try {
            if (isJunkValueAByte) {
                method = clazz.getDeclaredMethod(invokeMenuActionMethodName, int.class, int.class, int.class, int.class, int.class, int.class, String.class, String.class,
                        int.class, int.class, byte.class);
            } else if (isJunkValueShort) {
                method = clazz.getDeclaredMethod(invokeMenuActionMethodName, int.class, int.class, int.class, int.class, int.class, int.class, String.class, String.class,
                        int.class, int.class, short.class);
            } else {
                method = clazz.getDeclaredMethod(invokeMenuActionMethodName, int.class, int.class, int.class, int.class, int.class, int.class, String.class, String.class,
                        int.class, int.class, int.class);
            }
        } catch (Exception e) {
            log.error("Unable to find invokeMenuAction method \"{}\". Check if obfuscated method name is correct.", invokeMenuActionMethodName, e);
            return;
        }

        clientThread.invoke(() -> {
            try {
                method.setAccessible(true);
                if (isJunkValueAByte) {
                    //-1 is the id for the WorldView.
                    method.invoke(null, param0, param1, opcode, identifier, itemId, worldViewId, option, target, x, y, (byte) invokeMenuActionJunkValue);
                } else if (isJunkValueShort) {
                    //-1 is the id for the WorldView.
                    method.invoke(null, param0, param1, opcode, identifier, itemId, worldViewId, option, target, x, y, (short) invokeMenuActionJunkValue);
                } else {
                    //-1 is the id for the WorldView.
                    method.invoke(null, param0, param1, opcode, identifier, itemId, worldViewId, option, target, x, y, invokeMenuActionJunkValue);
                }
                method.setAccessible(false);
            }
            catch (Exception e) {
                log.error("Unable to invoke the method invokeMenuAction.", e);
            }
        });
    }

    /**
     * Invokes a menu action like toggling a prayer on/off or interacting with something via a MenuEntry using reflection.
     * @param param0 Parameter 0 for the menu action. This can be the X coordinate of the mouse or a different value depending on what action is taken.
     * @param param1 Parameter 1 for the menu action. This can be the Y coordinate of the mouse or a different value depending on what action is taken.
     * @param opcode This is the operation code for the action you are trying to perform. Each action has a different opcode. For example, the opcode for "Use" is 38, "Cast" is 25, "Drop" is 16, etc.
     *               These opcodes come from the {@code MenuAction} class. For example: MenuAction.GROUND_ITEM_FOURTH_OPTION;
     * @param identifier  Usually the same as the item id
     * @param itemId The item id is the id of the item you are interacting with. This can be the id of an item, NPC, player, or location.
     */
    public void invokeMenuAction(int param0, int param1, int opcode, int identifier, int itemId) {
        invokeMenuAction(param0, param1, opcode, identifier, itemId, "", "");
    }

    /**
     * Invokes a menu action like toggling a prayer on/off or interacting with something via a MenuEntry using reflection.
     * @param param0 Parameter 0 for the menu action. This can be the X coordinate of the mouse or a different value depending on what action is taken.
     * @param param1 Parameter 1 for the menu action. This can be the Y coordinate of the mouse or a different value depending on what action is taken.
     * @param opcode This is the operation code for the action you are trying to perform. Each action has a different opcode. For example, the opcode for "Use" is 38, "Cast" is 25, "Drop" is 16, etc.
     *               These opcodes come from the {@code MenuAction} class. For example: MenuAction.GROUND_ITEM_FOURTH_OPTION;
     * @param identifier  Usually the same as the item id
     * @param itemId The item id is the id of the item you are interacting with. This can be the id of an item, NPC, player, or location.
     * @param option Sometimes known as the "action". This is the text that appears in the menu option i.e. "Take", "Examine", "Cast", "Drop", etc.
     * @param target The target is the name of the object you are interacting with. This can be the name of an item, NPC, player, or location.
     */
    public void invokeMenuAction(int param0, int param1, int opcode, int identifier, int itemId, String option, String target) {
        invokeMenuAction(param0, param1, opcode, identifier, itemId, -1, option, target, -1, -1);
    }

    // Spell Widget is just any selected widget's packed id.
    private void setSelectedSpellWidget(int widgetPackedId) {
        Class<?> clazz = getClass(selectedSpellWidgetClassName);
        Field spellWidget = getField(clazz, selectedSpellWidgetFieldName);
        String errorMsg = "Unable to set selected spell widget.";
        int value = widgetPackedId * selectedSpellWidgetMultiplier;
        setFieldIntValue(spellWidget, clazz, value, errorMsg);
    }

    /*
        SpellChildIndex is actually the Widget index in its parent's children array. This can be gotten from Widget.getIndex().
        This needs to be set to -1 when you are trying to cast an actual spell because interacting with other widgets could set this value to not -1.
        For example, interacting with an item in the inventory would set this value to the index in the inventory array.
     */
    private void setSelectedSpellChildIndex(int index) {
        Field spellChild = getField(selectedSpellChildIndexClassName, selectedSpellChildIndexFieldName);
        String errorMsg = "Unable to set selected spell child index.";
        int value = index * selectedSpellChildIndexMultiplier;
        setFieldIntValue(spellChild, client, value, errorMsg);
    }

    /*
        SpellItemId is actually the item ID displayed by a widget. This can be gotten from Widget.getItemId().
        This needs to be set to -1 when you are trying to cast an actual spell because interacting with other widgets could set this value to not -1.
        For example, interacting with an item in the inventory (Use -> Target) would set this value to the item id of the item you interacted with.
     */
    private void setSelectedSpellItemId(int itemId) {
        Field spellItem = getField(selectedSpellItemIDClassName, selectedSpellItemIDFieldName);
        String errorMsg = "Unable to set selected spell item id.";
        int value = itemId * selectedSpellItemIDMultiplier;
        setFieldIntValue(spellItem, client, value, errorMsg);
    }

    //As explained above, you need to set spellChildIndex and spellItemId to -1 if you want to cast a spell.
    public void setSelectedSpell(int spellWidgetId) {
        setSelectedWidgetHooks(spellWidgetId, -1, -1);
    }

    public void setSelectedWidgetHooks(int spellWidgetId, int spellChildIndex, int spellItemId) {
        clientThread.invoke(() -> {
            setSelectedSpellWidget(spellWidgetId);
            setSelectedSpellChildIndex(spellChildIndex);
            setSelectedSpellItemId(spellItemId);
        });
    }

    //Menus Hook Methods
    public int getMenuOptionsCount() {
        Field optionsCount = getField(menuOptionsCountClassName, menuOptionsCountFieldName);
        String errorMsg = "Failed to get menu options count.";
        return getFieldIntValue(optionsCount, client.getMenu(), menuOptionsCountMultiplier, errorMsg);
    }

    public int getTopMenuEntryIndex() {
        return getMenuOptionsCount() - 1;
    }

    private void setMenuIdentifier(int index, int value) {
        Field menuIdentifiers = getField(menuIdentifiersClassName, menuIdentifiersFieldName);
        String errorMsg = "Failed to set menu identifier \"" + value + "\" in menu index \"" + index + "\".";
        setFieldIntArrayValue(menuIdentifiers, client.getMenu(), index, value, errorMsg);
    }

    public void setMenuItemId(int index, int value) {
        Field menuItemIds = getField(menuItemIdsClassName, menuItemIdsFieldName);
        String errorMsg = "Failed to set menu item id \"" + value + "\" in menu index \"" + index + "\".";
        setFieldIntArrayValue(menuItemIds, client.getMenu(), index, value, errorMsg);
    }

    private void setMenuOption(int index, String value) {
        Field menuOptions = getField(menuOptionsClassName, menuOptionsFieldName);
        String errorMsg = "Failed to set menu option \"" + value + "\" in menu index \"" + index + "\".";
        setFieldObjectArrayValue(menuOptions, client.getMenu(), index, value, errorMsg);
    }

    private void setMenuParam0(int index, int value) {
        Field menuParam0s = getField(menuParam0ClassName, menuParam0FieldName);
        String errorMsg = "Failed to set menu param0 \"" + value + "\" in menu index \"" + index + "\".";
        setFieldIntArrayValue(menuParam0s, client.getMenu(), index, value, errorMsg);
    }

    private void setMenuParam1(int index, int value) {
        Field menuParam1s = getField(menuParam1ClassName, menuParam1FieldName);
        String errorMsg = "Failed to set menu param1 \"" + value + "\" in menu index \"" + index + "\".";
        setFieldIntArrayValue(menuParam1s, client.getMenu(), index, value, errorMsg);
    }

    private void setMenuTarget(int index, String value) {
        Field menuTargets = getField(menuTargetsClassName, menuTargetsFieldName);
        String errorMsg = "Failed to set menu target \"" + value + "\" in menu index \"" + index + "\".";
        setFieldObjectArrayValue(menuTargets, client.getMenu(), index, value, errorMsg);
    }

    private void setMenuOpcode(int index, int value) {
        Field menuOpcodes = getField(menuTypesClassName, menuTypesFieldName);
        String errorMsg = "Failed to set menu option \"" + value + "\" in menu index \"" + index + "\".";
        setFieldIntArrayValue(menuOpcodes, client.getMenu(), index, value, errorMsg);
    }

    public void insertMenuEntry(int param0, int param1, int index, int opcode, int id, int itemId, String option, String target) {
        clientThread.invoke(() -> {
            setMenuOption(index, option);
            setMenuTarget(index, target);
            setMenuOpcode(index, opcode);
            setMenuIdentifier(index, id);
            setMenuParam0(index, param0);
            setMenuParam1(index, param1);
            setMenuItemId(index, itemId);
        });
    }
}
