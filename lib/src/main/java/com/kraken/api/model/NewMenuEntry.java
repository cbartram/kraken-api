package com.kraken.api.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class NewMenuEntry implements MenuEntry {

    @Getter
    private String option;

    @Getter
    private String target;

    @Getter
    private int identifier;

    @Getter
    private MenuAction type;

    @Getter
    private int param0;

    @Getter
    private int param1;

    @Getter
    private boolean forceLeftClick;

    @Getter
    private int itemId;

    @Getter
    private int itemOp;

    @Getter
    private TileObject gameObject;

    private Actor actor;

    private NewMenuEntry(int param0, int param1, MenuAction type, int identifier) {
        this.param0 = param0;
        this.param1 = param1;
        this.type = type;
        this.identifier = identifier;
    }

    private NewMenuEntry(int param0, int param1, int opcode, int identifier) {
        this(param0, param1, MenuAction.of(opcode), identifier);
    }

    public NewMenuEntry(String option, int param0, int param1, int opcode, int identifier, int itemId, String target) {
        this(param0, param1, opcode, identifier);
        this.option = option;
        this.target = target;
        this.forceLeftClick = false;
        this.itemId = itemId;
    }

    public NewMenuEntry(int param0, int param1, int opcode, int identifier, int itemId, String option) {
        this.param0 = param0;
        this.param1 = param1;
        this.type = MenuAction.of(opcode);
        this.identifier = identifier;
        this.itemId = itemId;
        this.target = "";
        this.option = option;
        this.forceLeftClick = false;
    }

    public NewMenuEntry(int param0, int param1, int opcode, int identifier, int itemId, String option, String target, TileObject gameObject) {
        this.option = "Use";
        this.target = target;
        this.identifier = identifier;
        this.type = MenuAction.of(opcode);
        this.param0 = param0;
        this.param1 = param1;
        this.forceLeftClick = false;
        this.itemId = itemId;
        this.option = option;
        this.gameObject = gameObject;
    }

    public NewMenuEntry(String option, int param0, int param1, int opcode, int identifier, int itemId, int itemOp, String target) {
        this.option = option;
        this.param0 = param0;
        this.param1 = param1;
        this.type = MenuAction.of(opcode);
        this.identifier = identifier;
        this.itemId = itemId;
        this.target = target;
        this.itemOp = itemOp;
        this.forceLeftClick = false;
    }

    public NewMenuEntry(int param0, int param1, int opcode, int identifier, int itemId, String target, Actor actor, String option) {
        this(param0, param1, opcode, identifier);
        this.option = option;
        this.target = target;
        this.forceLeftClick = false;
        this.itemId = itemId;
        this.actor = actor;
    }

    public MenuEntry setOption(String option) {
        this.option = option;
        return this;
    }

    public MenuEntry setTarget(String target) {
        this.target = target;
        return this;
    }

    public MenuEntry setIdentifier(int identifier) {
        this.identifier = identifier;
        return this;
    }

    public MenuEntry setType(MenuAction type) {
        this.type = type;
        return this;
    }

    public MenuEntry setParam0(int param0) {
        this.param0 = param0;
        return this;
    }

    public MenuEntry setParam1(int param1) {
        this.param1 = param1;
        return this;
    }

    public MenuEntry setForceLeftClick(boolean forceLeftClick) {
        this.forceLeftClick = forceLeftClick;
        return this;
    }

    @Override
    public int getWorldViewId() {
        return 0;
    }

    @Override
    public MenuEntry setWorldViewId(int worldViewId) {
        return null;
    }

    public boolean isDeprioritized() {
        return false;
    }

    public MenuEntry setDeprioritized(boolean deprioritized) {
        return this;
    }

    public MenuEntry onClick(Consumer<MenuEntry> callback) {
        return this;
    }

    @Override
    public Consumer<MenuEntry> onClick() {
        return null;
    }

    public boolean isItemOp() {
        return false;
    }

    @Override
    public MenuEntry setItemId(int itemId) {
        this.itemId = itemId;
        return this;
    }

    @Nullable
    public Widget getWidget() {
        return null;
    }

    @Nullable
    public NPC getNpc() {
        return null;
    }

    @Nullable
    public Player getPlayer() {
        return null;
    }

    @Nullable
    public Actor getActor() {
        return null;
    }

    @Nullable
    @Override
    public Menu getSubMenu() {
        return null;
    }

    @Override
    public Menu createSubMenu() {
        return null;
    }

    @Override
    public void deleteSubMenu() {}
}
