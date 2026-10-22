package net.somewhatcity.boiler.core.display;

import net.somewhatcity.boiler.api.util.SourceType;

/** Internal rendering mode of a source that hosts another source. */
public interface CompositeSource {
    SourceType effectiveSourceType();
}
