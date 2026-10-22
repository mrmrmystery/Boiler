package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.sources.CloneSource;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

class CloneLifecycleTest {
    @Test void detachesOnceEvenWhilePluginIsStopping() throws Exception {
        withClone((source, display, target, stopping) -> {
            JsonObject data = new JsonObject();
            data.addProperty("id", 2);
            source.load(display, data);
            verify(display.mapDisplay()).cloneGroupIds(target.mapDisplay());
            stopping.set(true);
            source.unload();
            source.unload();
            verify(display.mapDisplay(), times(1)).cutOffCloneGroupIds();
            verify(display, times(1)).renderPaused(false);
        });
    }

    @Test void cancellationBeforeInitializationNeverAttachesClone() throws Exception {
        withClone((source, display, target, stopping) -> {
            source.cancelLoad();
            JsonObject data = new JsonObject();
            data.addProperty("id", 2);
            source.load(display, data);
            source.unload();
            verify(display.mapDisplay(), never()).cloneGroupIds(any());
            verify(display, never()).renderPaused(true);
        });
    }

    @FunctionalInterface interface Check { void run(CloneSource source, IBoilerDisplay display, IBoilerDisplay target, AtomicBoolean stopping) throws Exception; }
    private void withClone(Check check) throws Exception {
        BoilerPlugin previous = BoilerPlugin.getPlugin();
        Field singleton = BoilerPlugin.class.getDeclaredField("plugin");
        singleton.setAccessible(true);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            BoilerPlugin plugin = mock(BoilerPlugin.class);
            singleton.set(null, plugin);
            AtomicBoolean stopping = new AtomicBoolean();
            when(plugin.callOnMain(any())).thenAnswer(invocation -> {
                if (stopping.get()) throw new IllegalStateException("Plugin is stopping");
                return ((Callable<?>) invocation.getArgument(0)).call();
            });
            IBoilerDisplay display = mock(IBoilerDisplay.class), target = mock(IBoilerDisplay.class);
            when(display.id()).thenReturn(1);
            when(target.id()).thenReturn(2);
            when(display.width()).thenReturn(128);
            when(display.height()).thenReturn(128);
            when(target.width()).thenReturn(128);
            when(target.height()).thenReturn(128);
            when(display.mapDisplay()).thenReturn(mock(IMapDisplay.class));
            when(target.mapDisplay()).thenReturn(mock(IMapDisplay.class));
            IDisplayManager manager = mock(IDisplayManager.class);
            when(plugin.displayManager()).thenReturn(manager);
            when(manager.display(2)).thenReturn(target);
            check.run(new CloneSource(), display, target, stopping);
        } finally { singleton.set(null, previous); }
    }
}
