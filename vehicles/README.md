# Modele pojazdów

Wrzuć tutaj swój model auta, żeby gra go ładowała zamiast szarego prostopadłościanu.

## Gdzie wrzucić plik

```
cvvlsimulator-3d/
  vehicles/
    car.glb    ← główny model (nazwa musi być dokładnie car.glb)
```

Możesz też użyć `car.gltf` — zmień ścieżkę w `VehicleSpawner.java` (`CUSTOM_VEHICLE_MODEL`).

## Zalecany format

- **`.glb`** (GLTF binary) — najlepszy w Godot 4
- ewentualnie `.gltf` + pliki `.bin` / tekstury w tym samym folderze

Po skopiowaniu pliku **otwórz projekt w edytorze Godot** — importer sam wygeneruje `car.glb.import`.

## Wymagania modelu

1. Auto skierowane przodem (oś Z) lub popraw skalę w Imporcie (zakładka **Scene** → Scale).
2. Rozsądna skala: ok. długość 3–5 m (w edytorze porównaj z placeholderem na parkingu).
3. Kolizja: gra **automatycznie** dopasowuje prostokątny collider do rozmiaru modelu i skaluje bardzo duże pliki (np. z Blendera) do ~4 m długości. Kolizje z GLB są wyłączane — używany jest jeden collider na pojeździe.

## Po dodaniu pliku

```powershell
.\gradlew.bat build
```

Uruchom grę (F5) — na miejscach `parking_01`–`parking_03` pojawi się Twój model z losową tablicą i danymi (mandaty działają jak wcześniej, **[E]**).

## Więcej niż jedno auto

Na razie wszystkie 3 miejsca używają tego samego `car.glb`. Osobne modele per slot — do zrobienia w kodzie (np. `car_01.glb`, `car_02.glb`).
