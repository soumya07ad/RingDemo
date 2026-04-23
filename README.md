# InnerPulse — Smart Ring Fitness App

> Android health & fitness application powered by the **JMRing SDK** for real-time BLE communication with smart rings.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [How the Ring Connects](#how-the-ring-connects)
4. [How Data Flows (Ring → Screen)](#how-data-flows-ring--screen)
5. [Data Parsing Deep Dive](#data-parsing-deep-dive)
6. [Key Classes Reference](#key-classes-reference)
7. [Measurement System](#measurement-system)
8. [Background Sync](#background-sync)
9. [Database Schema](#database-schema)
10. [Build & Dependencies](#build--dependencies)

---

## Architecture Overview

The app uses **Clean Architecture + MVVM** with three distinct layers:

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                   │
│  Jetpack Compose UI  ←  ViewModels  ←  UI State Models  │
├─────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                       │
│    Use Cases  ←  Repository Interfaces  ←  Models       │
├─────────────────────────────────────────────────────────┤
│                       DATA LAYER                        │
│  JMRingManager (SDK)  →  RingRepositoryImpl  →  Room DB │
└─────────────────────────────────────────────────────────┘
```

**Why this matters:** The UI never talks to Bluetooth directly. Every BLE event flows through `JMRingManager → RingRepositoryImpl → ViewModel → Compose UI`. This isolation means a bug in the SDK layer cannot crash the UI.

---

## Project Structure

```
com.dkgs.innerpulse/
│
├── FitnessApplication.kt        # App entry: initializes SDK + DI + WorkManager
├── MainActivity.kt              # Single-activity host for Compose navigation
│
├── ble/                          # BLE state models (shared vocabulary)
│   └── BleState.kt              # BleConnectionState, RingData, MeasurementTimer
│
├── core/
│   └── di/
│       ├── AppContainer.kt      # Manual DI container (no Hilt/Dagger)
│       └── AppViewModelFactory.kt
│
├── data/
│   ├── ble/
│   │   └── JMRingManager.kt     # ★ CORE: SDK wrapper — all hardware communication
│   ├── repository/
│   │   ├── RingRepositoryImpl.kt # Maps SDK data → domain models
│   │   ├── SleepRepositoryImpl.kt
│   │   ├── StepRepository.kt
│   │   └── ...
│   ├── local/
│   │   ├── db/AppDatabase.kt    # Room database (v7)
│   │   ├── dao/                 # SleepDao, MoodDao, JournalDao, etc.
│   │   └── entity/              # Room entities
│   └── source/
│       ├── PhoneStepDataSource.kt
│       └── HealthConnectManager.kt
│
├── domain/
│   ├── model/
│   │   ├── Ring.kt              # Device identity (MAC, name, RSSI)
│   │   ├── ConnectionStatus.kt  # Sealed class: Connected/Connecting/Disconnected
│   │   ├── RingHealthData.kt    # HR, SpO2, steps, sleep, stress, battery
│   │   └── ScanStatus.kt
│   ├── repository/
│   │   └── IRingRepository.kt   # Interface — the contract between layers
│   └── usecase/
│       ├── ConnectRingUseCase.kt # Validates MAC + delegates to repository
│       ├── ScanDevicesUseCase.kt
│       └── GetRingDataUseCase.kt
│
├── presentation/
│   ├── ring/
│   │   ├── RingViewModel.kt     # Scanning, connecting, measuring
│   │   ├── RingUiState.kt       # Single state object for Ring Setup screen
│   │   └── screens/RingSetupScreen.kt
│   ├── dashboard/
│   │   ├── DashboardViewModel.kt     # Merges ring + fitness data
│   │   ├── SmartRingViewModel.kt     # Simplified connection state for dashboard
│   │   ├── DashboardUiState.kt
│   │   └── screens/DashboardScreen.kt
│   ├── auth/                    # Login / Registration
│   ├── coach/                   # AI Coach feature
│   ├── meditation/              # Guided meditation
│   ├── wellness/                # Mood + Journal tracking
│   └── settings/                # App preferences
│
└── network/
    ├── api/                     # Retrofit API interface
    ├── client/RetrofitClient.kt # HTTP client with token management
    ├── sync/BackendSyncWorker.kt# WorkManager periodic sync (every 15 min)
    └── models/                  # API request/response DTOs
```

---

## How the Ring Connects

The connection is a **5-stage process**:

```
User taps "Connect"
        │
        ▼
┌─── STAGE 1: PERMISSION CHECK ───┐
│ RingViewModel.checkPermissions() │
│ • BLUETOOTH_SCAN                 │
│ • BLUETOOTH_CONNECT              │
│ • ACCESS_FINE_LOCATION           │
└──────────┬───────────────────────┘
           ▼
┌─── STAGE 2: MAC VALIDATION ─────┐
│ ConnectRingUseCase.invoke()      │
│ • Strips colons/spaces           │
│ • Validates 12 hex chars         │
│ • Calls repository.connect()     │
└──────────┬───────────────────────┘
           ▼
┌─── STAGE 3: SDK CONFIGURATION ──┐
│ JMRingManager.connectRing()      │
│ • RingBleUtils.setRingData(      │
│     userId, mac, sn, ringType=2) │
│ • setupListeners() ← RE-REGISTER│
│   (SDK swaps internal manager    │
│    based on ring type)           │
└──────────┬───────────────────────┘
           ▼
┌─── STAGE 4: BLE HANDSHAKE ──────┐
│ manager.onConnect()              │
│ • Android BLE scan + GATT open   │
│ • MTU negotiation                │
│ • SDK authentication (auth ok)   │
│ • onConnectSuccess() callback    │
│   OR safety trigger on battery   │
└──────────┬───────────────────────┘
           ▼
┌─── STAGE 5: DATA FETCH ─────────┐
│ Auto-fetch after connection:     │
│ • getActivityHealthData()        │
│ • getActivitySleepData()         │
│ • getActivityStressData()        │
│ • handlerCacheRing() (cached)    │
└──────────────────────────────────┘
```

### Connection Retry Logic

If `onConnectFail()` fires, the SDK automatically retries up to **5 times** before giving up and emitting `BleConnectionState.Error`.

### Safety Trigger

Some ring firmware (especially Xiaoqi/Type 2) doesn't always fire `onConnectSuccess()`. The app has a fallback: if battery or health data arrives, it forces the state to `Connected`.

```kotlin
// In JMRingManager.onBatteryListener():
if (_connectionState.value !is BleConnectionState.Connected) {
    _connectionState.value = BleConnectionState.Connected(ring)
}
```

---

## How Data Flows (Ring → Screen)

```
Physical Ring (BLE Hardware)
    │
    │  Raw Bluetooth packets (hex bytes)
    ▼
JMRing SDK (com.gps.track.jmring)
    │
    │  Parsed into SDK "Bean" objects:
    │  • JMHealthAllBean (HR, SpO2, steps, calories, distance)
    │  • JMStressBean (stress index)
    │  • JMSleepBean (sleep stages)
    │  • Battery callback (charge %, charging state)
    ▼
JMRingManager (data/ble/JMRingManager.kt)
    │
    │  Maps beans → RingData (our internal model)
    │  Emits via: _ringData: MutableStateFlow<RingData>
    ▼
RingRepositoryImpl (data/repository/RingRepositoryImpl.kt)
    │
    │  Maps RingData → RingHealthData (domain model)
    │  Maps BleConnectionState → ConnectionStatus
    │  Emits via: ringData: StateFlow<RingHealthData>
    │             connectionStatus: StateFlow<ConnectionStatus>
    ▼
ViewModel (DashboardViewModel / RingViewModel)
    │
    │  Collects flows, merges into single UI state
    │  Emits via: uiState: StateFlow<DashboardUiState>
    ▼
Jetpack Compose UI
    │
    │  Observes uiState, renders cards/charts
    ▼
User sees: ❤️ 72 bpm  |  🫁 98% SpO2  |  🚶 4,523 steps
```

---

## Data Parsing Deep Dive

### Heart Rate & SpO2 (from `JMHealthAllBean`)

```kotlin
// JMRingManager.onHealthAllBeanListener()
val bean = list.last()  // Take the most recent reading

_ringData.value = _ringData.value.copy(
    heartRate = bean.dailyHeartRate?.toInt(),  // e.g., 72
    spO2 = bean.spo2?.toFloat(),              // e.g., 98.5
    steps = bean.stepDiff?.toInt(),            // e.g., 4523
    calories = bean.caloriesDiff?.toInt(),     // e.g., 187
    distance = bean.distanceDiff?.toInt()      // e.g., 3200 (meters)
)
```

### Stress (from `JMStressBean`)

```kotlin
// JMRingManager.onStressBeanListener()
val bean = list.last()
_ringData.value = _ringData.value.copy(
    stress = bean.pressureIndex?.toInt()  // 0-100 scale
)
```

### Sleep (from `JMSleepBean`)

Sleep is parsed by iterating through minute-by-minute sleep stage records:

```kotlin
// JMRingManager.onSleepAllBeanListener()
val details = bean.mergeSleepDetails()  // List of per-minute entries

for (detail in details) {
    when (detail.sleepMode) {
        1 -> lightMinutes++   // Light sleep
        2 -> deepMinutes++    // Deep sleep
        // Other modes = awake/REM
    }
}
val totalMinutes = lightMinutes + deepMinutes
// Result: "7h 23m" displayed on dashboard
```

### Battery (from `onBatteryListener`)

```kotlin
// Direct callback — no bean parsing needed
override fun onBatteryListener(isCharge: Boolean, electricity: Int?) {
    _ringData.value = _ringData.value.copy(
        battery = electricity,     // e.g., 88
        isCharging = isCharge      // true when on charging dock
    )
}
```

### Real-Time Measurement (from `onMeasureResult`)

When user taps "Measure HR" on screen:
1. `DashboardViewModel.startHeartRateMeasurement()` is called
2. → `IRingRepository.startHeartRateMeasurement()`
3. → `JMRingManager.startMeasurement(type=1)` sends opcode to ring
4. Ring streams readings for ~60 seconds
5. `onMeasureResult(type=1, isSuccess=true, healthBean=...)` fires with final value

**Measurement type codes:**
| Type | Measurement |
|------|-------------|
| 1    | Heart Rate  |
| 2    | SpO2        |
| 7    | Stress      |

---

## Key Classes Reference

| Class | File | Role |
|-------|------|------|
| `FitnessApplication` | `FitnessApplication.kt` | App-wide init: SDK + DI + WorkManager |
| `AppContainer` | `core/di/AppContainer.kt` | Manual DI (singleton repositories, use cases) |
| `JMRingManager` | `data/ble/JMRingManager.kt` | **Core SDK wrapper** — all BLE communication |
| `RingRepositoryImpl` | `data/repository/RingRepositoryImpl.kt` | Translates SDK → domain models |
| `IRingRepository` | `domain/repository/IRingRepository.kt` | Interface contract for ring operations |
| `ConnectRingUseCase` | `domain/usecase/ConnectRingUseCase.kt` | MAC validation + connect delegation |
| `RingViewModel` | `presentation/ring/RingViewModel.kt` | Ring setup screen state management |
| `DashboardViewModel` | `presentation/dashboard/DashboardViewModel.kt` | Dashboard screen state management |
| `RingData` | `ble/BleState.kt` | Internal data model (SDK-adjacent) |
| `RingHealthData` | `domain/model/RingHealthData.kt` | Domain data model (UI-facing) |
| `ConnectionStatus` | `domain/model/ConnectionStatus.kt` | Sealed class for connection states |

---

## Measurement System

```
User taps "Measure"
    │
    ▼
ViewModel.startHeartRateMeasurement()
    │
    ▼
IRingRepository.startHeartRateMeasurement()
    │
    ▼
JMRingManager.startMeasurement(type=1)
    ├── Sets heartRateMeasuring = true (UI shows spinner)
    ├── Starts 65-second safety timeout
    └── Calls SDK: manager.startStopMeasurement(1)
            │
            │  Ring LED turns on, starts sensor
            │  Streams data for ~60 seconds
            ▼
    onMeasureResult(type=1, success=true, healthBean=...)
    ├── Clears heartRateMeasuring = false (UI hides spinner)
    ├── Updates heartRate value in RingData
    └── Triggers sleep score recalculation
```

---

## Background Sync

**BackendSyncWorker** runs every 15 minutes via Android WorkManager:

```
WorkManager (every 15 min, requires network)
    │
    ▼
BackendSyncWorker.doWork()
    ├── 1. Reads latest health record from Room DB
    │      (DailyFitnessRecord: steps, distance, calories)
    │
    ├── 2. POST /health/sync → Backend API
    │
    ├── 3. Reads all journal entries from Room DB
    │
    └── 4. POST /journal/sync → Backend API
```

---

## Database Schema

**Room Database v7** with 6 tables:

| Entity | DAO | Purpose |
|--------|-----|---------|
| `SleepEntry` | `SleepDao` | Daily sleep duration from ring |
| `DailyFitnessRecord` | `DailyFitnessDao` | Steps, distance, calories per day |
| `MoodEntry` | `MoodDao` | User mood tracking |
| `JournalEntry` | `JournalDao` | Wellness journal entries |
| `StreakEntry` | `StreakDao` | Activity streak tracking |
| `CoachMessageEntity` | `CoachDao` | AI coach conversation history |

---

## Build & Dependencies

### SDK Dependencies (local maven repo in `sdkrepo/`)

| Library | Version | Purpose |
|---------|---------|---------|
| `JMRing` | 1.0.0_13 | Main SDK — ring management facade |
| `ycbtsdk-release` | 4.0.6 | Xiaoqi (小七) ring BLE protocol |
| `jl_rcsp` | 0.5.5 | JieLi RCSP Bluetooth protocol |
| `JL_Watch` | 1.10.3 | JieLi watch/ring BLE controller |
| `jl_bt_ota` | 1.9.6 | Over-the-air firmware updates |
| `aizo_sdk_debug` | 1.1.5 | Aizo cloud authentication |
| `aizo_serversdk_release` | 2_2.1.14 | Aizo server communication |

### App Dependencies

| Category | Library | Version |
|----------|---------|---------|
| UI | Jetpack Compose + Material3 | 1.6.8 / 1.2.1 |
| Navigation | Navigation Compose | 2.7.7 |
| Database | Room | 2.6.1 |
| Network | Retrofit + OkHttp | 2.10.0 / 4.12.0 |
| Auth | Firebase Auth + Google Sign-In | BOM 32.8.0 |
| Background | WorkManager | 2.9.1 |
| Health | Health Connect | 1.1.0-alpha07 |

### Build Config

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Kotlin:** 2.1.0
- **Java:** 21
- **Package:** `com.dkgs.innerpulse`

---

## Ring Type Reference

| Type | Name | Protocol | Notes |
|------|------|----------|-------|
| 1 | 研强 (YanQiang) | AIZO Cloud Auth | Requires server validation (blocked for our package) |
| 2 | 小七 (Xiaoqi) | YCBT BLE | **Default** — works without cloud auth |

> **Important:** The app hardcodes `ringType = 2` because Type 1 triggers an AIZO cloud authentication request that rejects our package name (`com.dkgs.innerpulse`), causing an infinite retry loop.

---

## Quick Start

1. Clone the repo and open in Android Studio
2. Sync Gradle (SDK dependencies are in the local `sdkrepo/` folder)
3. Build and deploy to a physical Android device (BLE requires real hardware)
4. Grant Bluetooth + Location permissions when prompted
5. Navigate to Ring Setup → Scan or enter MAC address → Connect
6. Dashboard will display live health metrics once connected
