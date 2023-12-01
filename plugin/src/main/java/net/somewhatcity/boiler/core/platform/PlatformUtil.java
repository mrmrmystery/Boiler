/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.platform;

import com.google.common.base.Preconditions;
import net.somewhatcity.boiler.common.platform.IListenerBridge;
import net.somewhatcity.boiler.common.platform.IPlatform;
import net.somewhatcity.boiler.common.platform.IPlatformProvider;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class PlatformUtil {

    private static List<IPlatformProvider> providers(ClassLoader loader) {
        Preconditions.checkArgument(loader instanceof URLClassLoader, "Loader is not an instance of URLClassLoader");

        List<IPlatformProvider> providers = new ArrayList<>();
        for(URL url : ((URLClassLoader) loader).getURLs()) {
            Path sourcePath;
            try {
                sourcePath = new File(url.toURI()).toPath();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            try(FileSystem fs = FileSystems.newFileSystem(sourcePath)) {
                for(Path rootDir : fs.getRootDirectories()) {
                    extractProviders(rootDir, loader, providers);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.unmodifiableList(providers);
    }

    private static void extractProviders(Path rootDir, ClassLoader loader, List<IPlatformProvider> providers) {
        try(Stream<Path> files = Files.list(rootDir)) {
            files.filter(path -> path.getFileName().toString().startsWith("provider_")).map(path -> {
                try {
                    return Files.readString(path).trim();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).map(className -> {
                try {
                    return loader.loadClass(className).getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }).map(obj -> (IPlatformProvider) obj).forEach(providers::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static IPlatform<?> getPlatform(Plugin plugin, ClassLoader loader, IListenerBridge bridge) {
        return providers(loader).stream().sorted().flatMap(provider -> provider.tryProvide(plugin, bridge).stream()).findFirst().orElseThrow(() -> new UnsupportedOperationException("Unsupported server version/software"));
    }
}
