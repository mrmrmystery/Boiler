/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.display;

import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.ISourceManager;
import net.somewhatcity.boiler.core.BoilerPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoilerSourceManager implements ISourceManager {

    private final BoilerPlugin plugin;
    private final HashMap<String, Class<? extends IBoilerSource>> sources;

    private final Set<String> commandVisible;

    public BoilerSourceManager(BoilerPlugin plugin) {
        this.plugin = plugin;
        sources = new HashMap<>();
        commandVisible = new HashSet<>();
    }
    @Override
    public HashMap<String, Class<? extends IBoilerSource>> sources() {
        return sources;
    }

    @Override
    public Set<String> sourceNames() {
        return sources.keySet();
    }

    @Override
    public Set<String> commandVisibleSourceNames() {
        return commandVisible;
    }

    @Override
    public void register(String name, Class<? extends IBoilerSource> sourceClass, boolean showInCommand) {
        sources.put(name, sourceClass);
        if(showInCommand) commandVisible.add(name);
    }

    @Override
    public void register(String name, Class<? extends IBoilerSource> sourceClass) {
        register(name, sourceClass, true);
    }

    @Override
    public Class<? extends IBoilerSource> source(String name) {
        return sources.get(name);
    }
}
