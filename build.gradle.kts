// Archivo: build.gradle.kts (nivel raíz)

plugins {
    // Plugins del proyecto (solo definiciones, sin aplicar)
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// Tarea para limpiar la carpeta build del proyecto
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}