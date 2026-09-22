pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

include(":app")

// ---------------------------------------------------------------------------
// Garde-fou : local.properties est un fichier local (gitignoré), absent d'un
// clone "à froid" (ZIP GitHub, machine neuve, CI). Sans lui le build plante
// immédiatement :
//   - Android Gradle Plugin : "SDK location not found"
//   - plugin secrets (mapsplatform) : local.properties introuvable
//
// On le crée donc ici (au démarrage de Gradle, avant toute configuration du
// projet) s'il n'existe pas, avec le meilleur chemin SDK connu :
//   1) ANDROID_HOME ou ANDROID_SDK_ROOT (si définis)
//   2) emplacements standards (Android Studio sur Windows, command-line
//      tools sur Linux/macOS)
// S'il existe déjà (ex: créé par Android Studio), on ne le modifie pas.
//
// SDK ailleurs ? Éditer local.properties :
//   sdk.dir=C:/chemin/vers/le/Android/Sdk
// (chemin visible dans Android Studio -> Tools -> SDK Manager).
// Voir aussi local.properties.example.
// ---------------------------------------------------------------------------
val localPropertiesFile = File(rootDir, "local.properties")
if (!localPropertiesFile.exists()) {
  val home = System.getProperty("user.home")
  val sdkCandidates = listOfNotNull(
    System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() },
    System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() },
    home?.let { "$it/AppData/Local/Android/Sdk" }, // Android Studio (Windows)
    home?.let { "$it/Android/Sdk" },               // command-line tools (Linux / macOS)
  )
  val sdkDirRaw = sdkCandidates.firstOrNull { File(it).isDirectory }
    ?: sdkCandidates.firstOrNull()
    ?: home?.let { "$it/AppData/Local/Android/Sdk" }
    ?: "C:/Android/Sdk"
  val sdkDir = sdkDirRaw.replace('\\', '/')
  localPropertiesFile.writeText("sdk.dir=$sdkDir\n")
  println("MBOA AGRI - local.properties absent : créé automatiquement (sdk.dir=$sdkDir).")
  if (!File(sdkDirRaw).isDirectory) {
    println("MBOA AGRI - ATTENTION : le dossier SDK '$sdkDir' n'existe pas. Corriger sdk.dir dans local.properties (Android Studio -> Tools -> SDK Manager).")
  }
}
