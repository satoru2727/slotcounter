# Room and Compose ship their own consumer rules. Keep this file for the few
# reflective entry points F-Droid's R8 build needs when minification is on.
-keepattributes *Annotation*

# The foreground service is started by name from the manifest; R8 keeps
# manifest-referenced classes, but keep the class name stable for debugging.
-keep class io.github.satoru2727.slotcounter.service.CountingService { *; }
