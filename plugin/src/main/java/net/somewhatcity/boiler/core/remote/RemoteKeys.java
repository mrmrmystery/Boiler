package net.somewhatcity.boiler.core.remote;

import net.somewhatcity.boiler.api.util.Key;
import org.bukkit.Input;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

/** Converts input state changes to the existing, press-only source input API. */
public final class RemoteKeys {
    private final EnumMap<Key, Long> repeatAt = new EnumMap<>(Key.class);
    private Set<Key> held = EnumSet.noneOf(Key.class);

    public static Set<Key> keys(Input input) {
        EnumSet<Key> keys = EnumSet.noneOf(Key.class);
        if (input.isForward()) keys.add(Key.W);
        if (input.isLeft()) keys.add(Key.A);
        if (input.isBackward()) keys.add(Key.S);
        if (input.isRight()) keys.add(Key.D);
        if (input.isJump()) keys.add(Key.SPACE);
        if (input.isSneak()) keys.add(Key.SHIFT);
        return keys;
    }

    public void update(Set<Key> keys, long tick, Consumer<String> send) {
        Set<Key> previous = held;
        held = keys.isEmpty() ? EnumSet.noneOf(Key.class) : EnumSet.copyOf(keys);
        repeatAt.keySet().retainAll(held);
        for (Key key : held) if (!previous.contains(key)) {
            repeatAt.put(key, tick + 8);
            send.accept(key.name());
        }
    }

    public void tick(long tick, Consumer<String> send) {
        for (Key key : held) {
            if (key == Key.SPACE || key == Key.SHIFT || tick < repeatAt.get(key)) continue;
            repeatAt.put(key, tick + 2);
            send.accept(key.name());
        }
    }
}
