/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.common;

import net.somewhatcity.boiler.common.platform.IListenerBridge;
import net.somewhatcity.boiler.common.platform.IPlatform;
import net.somewhatcity.boiler.common.platform.IPlatformProvider;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public class Paper1202Provider implements IPlatformProvider {
    @Override
    public Optional<IPlatform<?>> tryProvide(Plugin plugin, IListenerBridge bridge) {
        if(IPlatformProvider.existsClass("org.bukkit.craftbukkit.v1_20_R2.CraftServer")) {
            return Optional.of(Paper1202StaticProvider.provide(plugin, bridge));
        }
        return Optional.empty();
    }
}

final class Paper1202StaticProvider {
    private Paper1202StaticProvider() {}
    static IPlatform<?> provide(Plugin plugin, IListenerBridge bridge) {
        return new Paper1202Platform(plugin, bridge);
    }
}