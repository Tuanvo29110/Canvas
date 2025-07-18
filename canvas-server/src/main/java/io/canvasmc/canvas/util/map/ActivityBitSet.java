package io.canvasmc.canvas.util.map;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.NotNull;

public final class ActivityBitSet extends AbstractCollection<Activity> implements Set<Activity> {
    public static int BITS = -1;

    public int bitset = 0;

    public ActivityBitSet() {
        if (BITS == -1) {
            BITS = BuiltInRegistries.ACTIVITY.size() - 1;
        }
    }

    private static @NotNull Activity map(int i) {
        return BuiltInRegistries.ACTIVITY.byIdOrThrow(i);
    }

    @Override
    public boolean add(Activity activity) {
        int mask = 1 << activity.id;
        if ((bitset & mask) != 0) return false;
        bitset |= mask;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Activity activity) {
            int mask = 1 << activity.id;
            if ((bitset & mask) != 0) {
                bitset &= ~mask;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return (o instanceof Activity activity) && ((bitset & (1 << activity.id)) != 0);
    }

    @Override
    public @NotNull Iterator<Activity> iterator() {
        return new Iterator<>() {
            private int index = 0;

            private void advance() {
                while (index < BITS && (bitset & (1 << index)) == 0) index++;
            }
            {
                advance();
            }

            @Override
            public boolean hasNext() {
                return index < BITS;
            }

            @Override
            public Activity next() {
                if (!hasNext()) throw new NoSuchElementException();
                Activity act = map(index++);
                advance();
                return act;
            }
        };
    }

    @Override
    public int size() {
        return Integer.bitCount(bitset);
    }

    @Override
    public void clear() {
        bitset = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Set<?> s)) return false;
        if (s.size() != size()) return false;
        return containsAll(s);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < BITS; i++) {
            if ((bitset & (1 << i)) != 0) {
                hash += map(i).hashCode();
            }
        }
        return hash;
    }
}
