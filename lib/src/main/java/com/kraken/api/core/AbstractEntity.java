package com.kraken.api.core;

import com.kraken.api.Context;
import java.util.Objects;

// R = Raw RuneLite type (NPC, TileObject, Widget)
public abstract class AbstractEntity<R> implements Interactable {

    protected final Context ctx;
    protected final R raw;

    public AbstractEntity(Context ctx, R raw) {
        this.ctx = ctx;
        this.raw = raw;
    }

    public R raw() {
        return raw;
    }

    @Override
    public boolean isNull() {
        return raw == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractEntity<?> that = (AbstractEntity<?>) o;
        return Objects.equals(raw, that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }
}