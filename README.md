# Pad Link — Manette Bluetooth hors-ligne

Transforme ton téléphone Android en vraie manette Bluetooth (HID), sans
connexion internet, reconnue nativement par un PC Windows, un Mac ou un
autre appareil Android — comme une manette USB/Bluetooth classique,
sans logiciel à installer côté ordinateur.

## Comment ça marche

Le téléphone s'enregistre auprès de son propre adaptateur Bluetooth
comme **périphérique HID** (`BluetoothHidDevice`, API Android officielle).
Il apparaît alors dans les réglages Bluetooth de l'ordinateur sous le nom
**"Pad Link Gamepad"**, exactement comme une manette physique.
Aucune donnée ne transite par internet : tout passe par le Bluetooth local.

## ⚠️ Limitation à connaître avant de commencer

Le rôle "HID Device" dépend de la puce Bluetooth du téléphone.
La majorité des téléphones Android 9+ le supportent, mais ce n'est pas
garanti à 100 %. Au premier lancement, l'app te dira si ton téléphone
le supporte ou non (message "Ce téléphone ne supporte pas le mode
manette Bluetooth").

## Compilation (à faire une seule fois)

1. Installe [Android Studio](https://developer.android.com/studio) (gratuit).
2. `Fichier > Ouvrir` → sélectionne le dossier `android/` de ce projet.
3. Laisse Android Studio synchroniser Gradle (il télécharge le nécessaire
   automatiquement, connexion internet requise **uniquement pour cette
   étape de compilation**, pas pour l'usage de l'app ensuite).
4. Branche ton téléphone en USB (mode débogage USB activé dans les
   options développeur), clique sur ▶ **Run**.
5. L'app "Pad Link" est installée sur le téléphone. Tu peux ensuite
   débrancher le câble — elle fonctionne 100 % hors-ligne.

## Utilisation

1. Active le Bluetooth sur le téléphone ET sur l'ordinateur/console cible.
2. Lance l'app Pad Link sur le téléphone.
3. Sur le PC/Mac/Android hôte : va dans les réglages Bluetooth, cherche
   et appaire **"Pad Link Gamepad"**.
4. Une fois connecté, la manette tactile pilote directement les jeux/apps
   comme une vraie manette PS3 : Triangle / Carré / Rond / Croix, L1/L2/R1/R2,
   Select/Start/PS, D-pad, et les deux joysticks (avec clic L3/R3).

## Compatibilité par plateforme

| Plateforme | Fonctionne | Remarque |
|---|---|---|
| Windows (10/11) | ✅ | Reconnu comme manette générique dès l'appairage |
| Mac | ✅ | Idem, via Bluetooth natif |
| Android (comme hôte) | ✅ | Idem, marche pour jouer sur une tablette/TV Android |
| PS5 / Xbox | ❌ | Ces consoles refusent les manettes Bluetooth génériques |
| Nintendo Switch | ❌ | Protocole Bluetooth propriétaire, non standard |

## Fichiers du projet

- `app/src/main/java/com/padlink/gamepad/MainActivity.kt` — logique
  Bluetooth HID (descripteur de manette, envoi des rapports)
- `app/src/main/assets/controller.html` — interface tactile (joystick,
  boutons), chargée dans une WebView locale
