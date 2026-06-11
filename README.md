# 🎮 CVVL Simulator

CVVL Simulator to gra symulacyjna stworzona w silniku Godot, w której gracz wciela się w kontrolera parkingowego odpowiedzialnego za kontrolę pojazdów oraz wystawianie mandatów za wykroczenia parkingowe.

Projekt został wykonany w ramach przedmiotu **Programowanie Obiektowe II** na Akademii Nauk Stosowanych w Elblągu.

---

## 📌 Autorzy

- Adrian Czarnota (21442)
- Dawid Rzepka (21299)

---

# 📖 Opis gry

Zadaniem gracza jest patrolowanie parkingów znajdujących się na terenie miasta i kontrolowanie poprawności parkowania pojazdów.

Podczas kontroli gracz analizuje:

- czas postoju,
- rodzaj biletu parkingowego,
- lokalizację pojazdu,
- ewentualne naruszenia regulaminu.

Na podstawie zebranych informacji podejmuje decyzję o:

- wystawieniu mandatu,
- udzieleniu ostrzeżenia,
- zignorowaniu pojazdu.

Dodatkowo gracz rozwija swoją reputację, zdobywa środki finansowe oraz realizuje cele związane z wykonywaną pracą kontrolera parkingowego.

---

# ✨ Główne funkcjonalności

## 🚗 System pojazdów

- losowe generowanie samochodów,
- różne modele pojazdów,
- unikalne tablice rejestracyjne,
- indywidualne dane parkingowe,
- przypisanie wykroczeń do konkretnych pojazdów.

Każda nowa rozgrywka generuje inny układ pojazdów na parkingach.

---

## 🎫 System mandatów

Możliwość wystawiania mandatów za:

- brak biletu parkingowego,
- przekroczenie czasu postoju,
- nieprawidłowe parkowanie.

Dostępne są również:

- ostrzeżenia,
- ignorowanie pojazdu.

---

## 🔒 Zabezpieczenie przed wielokrotnym mandatem

Po wystawieniu mandatu pojazd zostaje oznaczony jako obsłużony.

Dzięki temu:

- nie można ponownie ukarać tego samego pojazdu,
- nie można wielokrotnie zdobywać pieniędzy za ten sam samochód,
- zachowana zostaje poprawność rozgrywki.

---

## 💾 System zapisu gry

Gra posiada trzy niezależne sloty zapisu.

Zapisywane są:

- pozycja gracza,
- dzień gry,
- aktualna godzina,
- saldo,
- reputacja,
- liczba mandatów,
- poziom trudności,
- stan wszystkich pojazdów.

Po wczytaniu gry:

- gracz pojawia się dokładnie w miejscu zapisu,
- odtwarzane są te same pojazdy,
- przywracany jest pełny stan rozgrywki.

---

## 🎯 Poziomy trudności

### Łatwy

- dokładny czas postoju,
- podpowiedź dotycząca wykroczenia,
- możliwość anulowania błędnej decyzji.

### Normalny

- standardowy tryb gry,
- samodzielna analiza sytuacji.

### Trudny

- ograniczona liczba informacji,
- większy nacisk na samodzielną ocenę sytuacji,
- dodatkowe cele do wykonania.

---

## 📈 System reputacji

Reputacja gracza zależy od poprawności podejmowanych decyzji.

Poprawne działania:

- zwiększają reputację,
- nagradzają gracza.

Błędne decyzje:

- obniżają reputację,
- utrudniają osiąganie wysokich wyników.

---

# 🗺️ Mapa

Na potrzeby projektu została stworzona autorska mapa miasta.

Mapa zawiera:

- supermarket Ledl,
- sklep Stonka,
- market Lick Lurc,
- osiedle mieszkaniowe,
- parkingi,
- drogi,
- przejścia dla pieszych,
- tereny zielone.

Świat gry został wykonany z wykorzystaniem własnych modeli 3D oraz zasobów przygotowanych specjalnie na potrzeby projektu.

---

# 🏗️ Historia projektu

Projekt przechodził przez trzy etapy technologiczne.

## Etap 1 – LWJGL 3

Pierwsza wersja projektu została stworzona przy użyciu biblioteki LWJGL 3.

Napotkano jednak problemy związane z obsługą modeli GLB eksportowanych z programu Blender.

Repozytorium:

https://github.com/rzepeeek/Projekt-JAVA-lwjgl3

---

## Etap 2 – jMonkeyEngine

Następnie projekt został przeniesiony do jMonkeyEngine.

Silnik poprawnie obsługiwał modele GLB, jednak pojawiły się problemy z obsługą nowoczesnych materiałów i tekstur eksportowanych z Blendera.

Repozytorium:

https://github.com/rzepeeek/Projekt-JAVA-jmonkey

---

## Etap 3 – Godot Engine

Ostatecznie projekt został przeniesiony do Godot Engine.

Godot zapewnił:

- poprawny import modeli,
- poprawny import tekstur,
- wygodne tworzenie interfejsu użytkownika,
- prostsze zarządzanie scenami,
- łatwiejszą implementację mechanik gry.

Aktualna wersja projektu została wykonana w Godot Engine.

---

# 📂 Struktura projektu

```text
src/main/java/cvvl/simulator
│
├── data/
│   ├── SavedVehicleCodec.java
│   ├── SavedVehicleData.java
│   └── SaveSlotData.java
│
├── player/
│   ├── FpsPlayer.java
│   └── PlayerInteraction.java
│
├── systems/
│   ├── DifficultyLevel.java
│   ├── InputActions.java
│   ├── SaveManager.java
│   └── SettingsManager.java
│
├── ui/
│   ├── DifficultySelectController.java
│   ├── DispatchMenuButton.java
│   ├── DispatchUi.java
│   ├── HudController.java
│   ├── MainMenuController.java
│   ├── OptionsMenuController.java
│   ├── PauseMenuController.java
│   ├── SaveMenuController.java
│   ├── SubmenuController.java
│   └── TicketPanelController.java
│
├── vehicles/
│   ├── ParkingViolation.java
│   ├── TicketType.java
│   ├── Vehicle.java
│   ├── VehicleModelHelper.java
│   └── VehicleSpawner.java
│
├── world/
│   ├── ParkingMapBootstrap.java
│   └── PlayerSpawnHelper.java
│
├── DispatchColors.java
├── GameState.java
├── GameWorldController.java
└── ScenePaths.java
```

---

# 🧩 Architektura projektu

## Player

Odpowiada za:

- sterowanie graczem,
- ruch FPS,
- obsługę kamery,
- interakcję z pojazdami.

### Klasy

- FpsPlayer
- PlayerInteraction

---

## Vehicles

Odpowiada za:

- generowanie pojazdów,
- przechowywanie danych pojazdów,
- wykroczenia parkingowe,
- modele samochodów.

### Klasy

- Vehicle
- VehicleSpawner
- VehicleModelHelper
- ParkingViolation
- TicketType

---

## Save System

Odpowiada za:

- zapis stanu gry,
- odczyt stanu gry,
- serializację danych pojazdów.

### Klasy

- SaveManager
- SaveSlotData
- SavedVehicleData
- SavedVehicleCodec

---

## Systems

Odpowiada za:

- poziomy trudności,
- konfigurację sterowania,
- ustawienia gry.

### Klasy

- DifficultyLevel
- InputActions
- SettingsManager

---

## User Interface

Obsługuje wszystkie elementy interfejsu użytkownika.

### Klasy

- MainMenuController
- DifficultySelectController
- HudController
- PauseMenuController
- SaveMenuController
- TicketPanelController
- OptionsMenuController

---

## World

Odpowiada za:

- inicjalizację świata gry,
- rozmieszczanie obiektów,
- miejsce startu gracza.

### Klasy

- ParkingMapBootstrap
- PlayerSpawnHelper

---

# 🎮 Sterowanie

| Klawisz | Akcja |
|----------|----------|
| W | Ruch do przodu |
| S | Ruch do tyłu |
| A | Ruch w lewo |
| D | Ruch w prawo |
| Shift | Sprint |
| E | Interakcja z pojazdem |
| Esc | Menu pauzy |

---

# 🔄 Przebieg rozgrywki

1. Uruchomienie gry.
2. Wybór poziomu trudności.
3. Wygenerowanie pojazdów na parkingach.
4. Patrolowanie miasta.
5. Kontrola pojazdów.
6. Podejmowanie decyzji:
   - mandat,
   - ostrzeżenie,
   - ignorowanie.
7. Zdobywanie środków finansowych.
8. Rozwijanie reputacji.
9. Zapisywanie postępu.
10. Kontynuowanie rozgrywki od miejsca zapisu.

---

# 🚀 Uruchomienie projektu

## Uruchomienie projektu w Godot za pomoca githuba

1. Pobierz Godot Engine 4.x
2. Sklonuj repozytorium:

```bash
git clone https://github.com/rzepeeek/PROJEKT-JAVA-GODOT.git
```

3. Otwórz folder projektu w Godot.
4. Uruchom scenę główną.

---

## Uruchomienie projektu w Godot za pomoca mega.nz

1. Pobierz zip'a ze strony
2. W godocie importuj projekt
3. Zbuduj projekt (prawy górny róg w godocie)
4. I odpal grę F5

---

# 📚 Technologie

- Godot Engine
- Java
- Blender
- Git
- GitHub

---

# 📄 Licencja

Projekt edukacyjny wykonany w ramach przedmiotu Programowanie Obiektowe II.

Wszelkie prawa do projektu należą do autorów.
