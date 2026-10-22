# Boiler
Extension Plugin for MapEngine that allows you to show different types of content on a map display

Download:
https://modrinth.com/plugin/boiler

All docs have been moved to the wiki tab
https://github.com/mrmrmystery/Boiler/wiki

## Bundled CEF browser source

The `browser` source runs Chromium off-screen on the server and sends its framebuffer to the map display. Create one with the normal source command and a URL, for example `browser https://example.com`. Click, cursor, scroll, and text events are forwarded to the page. Browser PCM is routed through Boiler's existing voice-chat speaker path.

The release JAR contains CEF 128.4.12, its Chromium runtime/resources, Boiler's JNI bridge, and Linux support libraries. On first use the matching platform bundle is checksum-verified and extracted under `plugins/Boiler/cef/128.4.12/<platform>`; the server does not download or install anything. Windows x64 and Linux x64 are supported.

CEF is shared by all browser sources and shuts down with the plugin. A full server restart is required after unloading/reloading Boiler because native libraries cannot be safely unloaded from the JVM.

Native sources and their CMake build are in `native/cef`. The bundled CEF and Linux-library license notices are included with the runtime resources. Linux requires a glibc-based x86_64 distribution and Java 21, but no optional desktop, audio, or browser packages.
