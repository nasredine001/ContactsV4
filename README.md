# TP Android — Contacts V4 (SQLite + Préférences + Fragments + Google Maps)

Kotlin, interfaces XML, une seule activité (`MainActivity`) et des fragments.
Persistance : `SQLiteOpenHelper` natif (base `contacts.db`, pas de Room). Préférences : `SharedPreferences` via AndroidX Preference.

## Lancer le projet
1. Android Studio (Koala 2024.1 ou plus récent, JDK 17) → **File ▸ Open** → dossier `ContactsV4` → attendre la synchronisation Gradle.
2. Clé Google Maps : copier `local.properties.example` vers `local.properties` (fichier ignoré par Git) et remplacer
   `MAPS_API_KEY=...` par votre clé (activer « Maps SDK for Android » dans Google Cloud Console).
   Sans clé, tout fonctionne sauf l’affichage des tuiles de la carte.
3. Lancer sur un appareil / émulateur Google APIs (API 24+).

## Structure
```
com.example.contacts
├── MainActivity                 # hôte des fragments + menu Paramètres
├── data/  Contact, ContactContract, ContactDbHelper (singleton), ContactDao (seul SQL)
├── ui/    ListeContactsFragment, DetailContactFragment, CarteContactFragment,
│          ParametresFragment, ContactViewModel (partagé), ContactAdapter
└── util/  PreferencesManager, ContactValidator, PhotoUtils, IntentUtils
```

## Tests

Le projet contient 23 tests automatisés :

```bash
./gradlew testDebugUnitTest          # 7 tests JVM (validation des champs/coordonnées)
./gradlew connectedDebugAndroidTest  # 16 tests instrumentés (nécessite un émulateur/appareil)
```

Les tests instrumentés couvrent : l’insertion unique des contacts de démonstration, le CRUD complet,
les apostrophes et jokers SQL (`'`, `%`, `_`), les colonnes NULL, le tri `COLLATE NOCASE`,
la lecture/conversion des préférences, le parcours UI (filtre, création, modification,
suppression avec confirmation, taille de police et bouton Appeler appliqués immédiatement),
ainsi que la rotation paysage/portrait de la liste, d’une saisie en cours, d’une fiche
existante et de l’écran Paramètres (état conservé, pas de fragment dupliqué).

> Sur un émulateur, désactivez les animations pour les tests Espresso :
> `adb shell settings put global window_animation_scale 0` (et
> `transition_animation_scale`, `animator_duration_scale`).
