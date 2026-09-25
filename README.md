# UPS Monitor Android

Cliente Android moderno e intuitivo para monitorear el estado de Sistemas de Alimentación Ininterrumpida (UPS / SAI) conectados a un servidor de monitoreo NUT (Network UPS Tools). Desarrollado con **Kotlin**, **Jetpack Compose** y **Material Design 3**.

---

## 🚀 Características

- **Panel de Control en Tiempo Real**:
  - Estado general del UPS (`Online`, `On Battery`, `Low Battery`).
  - Nivel de carga de la batería (%) con colores dinámicos según el nivel.
  - Carga actual de consumo (`Load` %).
  - Tiempo de autonomía estimado restante (`Runtime Remaining`).
  - Voltaje de entrada y voltaje nominal de red eléctrica.
  - Voltaje de batería y voltaje nominal.
  - Información del fabricante, modelo y número de serie del equipo.
- **Historial e Incidentes**:
  - Registro del último corte o incidente reportado con fecha y hora local formateada.
  - Historial detallado de eventos de cambio de estado.
- **Notificaciones Push (FCM)**:
  - Alertas instantáneas en segundo plano cuando ocurre un evento crítico o corte de energía.
  - Auto-registro transparente del token FCM en el servidor (`POST /api/register`).
- **Diseño Moderno & Gestos**:
  - UI construida 100% en Jetpack Compose.
  - Soporte para refresco mediante gesto **Pull-to-Refresh**.
  - Temas claro y oscuro basados en Material 3.
- **Persistencia de Configuración**:
  - Configuración sencilla de la URL del servidor backend mediante Jetpack DataStore Preferences.

---

## 🛠️ Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI Toolkit**: Jetpack Compose & Material 3 (BOM)
- **Arquitectura**: MVVM (Model-View-ViewModel) + StateFlow / Coroutines
- **Networking**: Retrofit 2 + Moshi + OkHttp Logging Interceptor
- **Notificaciones Push**: Firebase Cloud Messaging (FCM)
- **Persistencia local**: Jetpack DataStore Preferences
- **SDK Target**: Android 14+ / SDK 36 (Min SDK: Android 8.0 Oreo / SDK 26)

---

## 📡 API Requerida del Servidor

La aplicación se comunica con una API HTTP (generalmente un backend que consulta NUT) que expone los siguientes endpoints:

### 1. `GET /api/status`
Retorna el estado y las métricas actuales del UPS.

**Ejemplo de respuesta JSON:**
```json
{
  "manufacturer": "APC",
  "model": "Smart-UPS 1500",
  "serial": "AS1234567890",
  "status": "Online",
  "battery_charge": "100",
  "battery_voltage": "27.4",
  "battery_voltage_nominal": "24.0",
  "input_voltage": "220.5",
  "input_voltage_nominal": "220.0",
  "ups_load": "25",
  "runtime_seconds": "3600",
  "runtime_formatted": "1h 00m",
  "last_incident": {
    "id": 12,
    "status": "On Battery",
    "description": "Power failure detected",
    "changed_at": "2026-09-25T14:30:00Z"
  }
}
```

### 2. `GET /api/status-history`
Retorna la lista de los últimos eventos o cambios de estado registrados.

**Ejemplo de respuesta JSON:**
```json
[
  {
    "id": 15,
    "status": "Online",
    "description": "Mains restored",
    "changed_at": "2026-09-25T14:35:10Z"
  },
  {
    "id": 14,
    "status": "On Battery",
    "description": "Power failure",
    "changed_at": "2026-09-25T14:30:00Z"
  }
]
```

### 3. `POST /api/register`
Registra el dispositivo móvil para el envío de notificaciones push vía Firebase Cloud Messaging.

**Cuerpo de la petición:**
```json
{
  "device_token": "<FCM_DEVICE_TOKEN>",
  "device_name": "Google Pixel 8",
  "device_id": "a1b2c3d4-e5f6-..."
}
```

---

## ⚙️ Configuración y Compilación

### Requisitos previos
- Android Studio Ladybug / Meerkat o superior con JDK 11 o posterior.
- Proyecto en Firebase con una aplicación Android registrada (`ar.net.dahool.upsmonitor`).
- Archivo `google-services.json` ubicado en la raíz del módulo `app/`.

### Pasos para compilar
1. Clona el repositorio:
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd UPSMonitor
   ```
2. Asegúrate de tener tu archivo `google-services.json` en la carpeta `app/`:
   ```bash
   app/google-services.json
   ```
3. Compila el APK de depuración usando Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
4. O compila el APK de release:
   ```bash
   ./gradlew assembleRelease
   ```
   El APK resultante estará disponible en `app/build/outputs/apk/`.

---

## 📱 Uso de la Aplicación

1. Al abrir la app por primera vez, verás la pantalla indicando que el servidor no está configurado.
2. Toca en **"Open Settings"** (o el icono de engranaje en la barra superior).
3. Ingresa la URL base de tu servidor (ej. `http://192.168.1.50:3000` o `https://ups.lan`).
4. Presiona **"Save"**.
5. La aplicación registrará automáticamente tu dispositivo para notificaciones push y cargará de inmediato las métricas en el Dashboard.
6. Puedes deslizar hacia abajo en cualquier momento para actualizar los datos manualmente (**Pull-to-Refresh**).

---

## 📄 Licencia

Este proyecto está bajo la licencia [MIT](LICENSE) (o la que corresponda al repositorio).
