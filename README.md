# CVVL Simulator 3D

Symulator patrolu parkingowego w stylu **police dispatch terminal** (Godot 4.6 + Java przez [Godot Kotlin/JVM](https://godot-kotl.in/)).

## Wymagania

1. **Godot Kotlin/JVM Editor** 0.15.0-4.6 (nie standardowy Godot) — [releases](https://github.com/utopia-rise/godot-kotlin-jvm/releases)
2. **JDK 17+**
3. Gradle (wrapper w projekcie: `gradlew.bat`)

## Uruchomienie

```powershell
# 1. Skompiluj skrypty Java i wygeneruj pliki .gdj
.\gradlew.bat build

# 2. Otwórz projekt w edytorze Godot Kotlin/JVM i naciśnij F5
```

## Sterowanie (gra)

| Akcja | Klawisz |
|--------|---------|
| Ruch | WASD |
| Sprint | Shift |
| Skok | Spacja |
| Celownik / interakcja | E (inspekcja pojazdu) |
| Pauza | ESC |

## Struktura projektu

```
src/main/java/cvvl/simulator/   # logika w Javie
gdj/                            # wygenerowane rejestracje (po build)
scenes/                         # sceny Godot
assets/                           # tła, fonty, UI
vehicles/                         # modele aut .glb
maps/                             # mapa świata: parking_map.glb
scenes/world/                     # ParkingMap.tscn (instancja mapy w Game)
systems/                          # rozszerzenia (zapis, opcje — wkrótce)
```

## Zaimplementowane (MVP)

- **Main Menu** — menu po lewej, styl dispatch, animacje hover
- **FPS** — WASD, mysz, sprint, grawitacja, kolizje
- **HUD** — godzina, dzień, saldo, reputacja, mandaty, pojazd
- **Pojazdy** — spawn na `parking_01`–`parking_03`, dane losowe (tablica, bilet, limit)
- **Interakcja** — raycast z kamery, **E** otwiera panel mandatu
- **Ticket Panel** — mandat / ostrzeżenie / ignoruj + podmenu powodów
- **Pauza** — ESC, przyciemnienie UI

## Placeholdery

- `OptionsMenu`, `SaveMenu`, `ControlsMenu` — sceny szkieletowe
- Pojazdy jako proste prostopadłościany (podmień na `.glb` w `vehicles/`)

## Modele GLB

1. Umieść pliki w `vehicles/`
2. W edytorze: Import → przypisz do sceny zamiast `BoxMesh` w `VehicleSpawner` (rozszerzenie kodu)

## Kompilacja po zmianach w Javie

Po każdej edycji plików `.java` uruchom ponownie:

```powershell
.\gradlew.bat build
```

Edytor przeładuje klasy automatycznie, jeśli jest otwarty.
