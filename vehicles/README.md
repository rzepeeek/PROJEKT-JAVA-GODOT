# Modele pojazdów

Każde miejsce na mapie ma **swój plik** w `vehicles/`.

## Nazewnictwo (np. sedan1, sedan2, …)

```
vehicles/
  car.glb       ← zapasowy, gdy brak pliku z listy
  sedan1.glb
  sedan2.glb
  van1.glb
  van2.glb
  suv1.glb
  suv2.glb
  …             ← sedan3.glb, van3.glb itd. — dopisz w kodzie
```

Cyfra na końcu = **kolejna sztuka** tego typu (nie numer parkingu).  
Dwa sedany = **dwa osobne pliki** albo ten sam plik wpisany dwa razy w tablicy (patrz niżej).

## Przypisanie do miejsc (VehicleSpawner.java)

Ta sama kolejność w obu tablicach:

| Marker       | Plik        |
|-------------|-------------|
| parking_01  | sedan1.glb  |
| parking_02  | sedan2.glb  |
| parking_03  | sedan3.glb  |
| parking_04  | van1.glb    |
| parking_05  | van2.glb    |
| parking_06  | van3.glb    |
| parking_07  | suv1.glb    |
| parking_08  | suv2.glb    |
| parking_09  | suv3.glb    |

```java
private static final String[] SPOT_NAMES = {
    "parking_01", "parking_02", "parking_03",
    "parking_04", "parking_05", "parking_06",
    "parking_07", "parking_08", "parking_09"
};
private static final String[] SPOT_MODEL_FILES = {
    "sedan1.glb", "sedan2.glb", "sedan3.glb",
    "van1.glb", "van2.glb", "van3.glb",
    "suv1.glb", "suv2.glb", "suv3.glb"
};
```

**Więcej aut:** dodaj marker w Godot (patrz niżej) i dopisz nazwę + plik w obu tablicach w `VehicleSpawner.java`.

## Gdzie ustawić parkingi w Godot

Markery są w **`scenes/Game.tscn`**:

1. Otwórz **`Game.tscn`** → **World** → **ParkingMap** → **VehicleSpawner**.
2. Zaznacz **`VehicleSpawner`**.
3. **Prawy przycisk → Add Child Node → `Marker3D`**.
4. Zmień nazwę na dokładnie **`parking_10`** (wzorzec: `parking_01`, `parking_02`, …).
5. W widoku **3D** (klawisz **W** = przesuwanie) ustaw marker **na asfalcie**:
   - **X / Z** — miejsce na parkingu  
   - **Y** — lekko nad powierzchnią (np. `0.05`) — gra i tak dosunie auto do podłoża przy starcie.
6. W **`VehicleSpawner.java`** dopisz w **`SPOT_NAMES`** i **`SPOT_MODEL_FILES`** (ta sama kolejność).
7. `.\gradlew.bat build` → **F5**.

**Ważne:** Markery muszą być pod **`ParkingMap/VehicleSpawner`** (jadą razem z mapą).

**Nowe miejsce bez kodu:** nie zadziała — nazwa markera musi być na liście `SPOT_NAMES`.

**Ten sam model na dwóch miejscach:** wpisz ten sam plik dwa razy, np. `"sedan1.glb", "sedan1.glb"`.

## Po dodaniu plików

1. Otwórz projekt w Godot (import `.glb`).
2. `.\gradlew.bat build`
3. **F5** — przesuń markery `parking_XX` na mapie, jeśli trzeba.

## Format

- `.glb` zalecane, przód auta wzdłuż osi **Z**, długość ~3–5 m.
