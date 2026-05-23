# 🕐 Control de Horas

> App Android nativa para el registro preciso de horas de trabajo, práctica profesional y actividades académicas.

<p align="center">
  <img src="https://img.shields.io/badge/Android-26%2B-brightgreen?logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-blueviolet?logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-✓-blue?logo=jetpack-compose" />
  <img src="https://img.shields.io/badge/License-MIT-yellow" />
</p>

---

## 📱 Descarga directa

> **[⬇️ Descargar APK (v1.2.0)](https://github.com/schwarmak-dev/control-de-horas/releases/latest)**
>
> Compatible con Android 8.0 (Oreo) o superior.  
> No requiere Google Play — instala directamente en tu dispositivo.
> Pronto existira una app en Playstore

---

## ✨ Funcionalidades

| Característica | Descripción |
|---|---|
| ⏱️ **Temporizador activo** | Inicia, pausa y reanuda sesiones con un toque. Protección anti-olvido a las 12 h |
| 📝 **Registro manual retroactivo** | Agrega sesiones pasadas con DatePicker y TimePicker nativos |
| ✏️ **Edición de sesiones** | Corrige cualquier sesión ya guardada sin borrarla y recrearla |
| 📊 **Dashboard de estadísticas** | Progreso hacia tu meta, gráfico de distribución y historial completo |
| 📅 **Calendario mensual** | Vista de actividad con puntos en los días con horas registradas |
| 📁 **Exportación CSV y PDF** | CSV para Excel y PDF imprimible con firma para supervisores |
| 🔔 **Notificaciones anti-olvido** | Alerta configurable si el temporizador lleva demasiado tiempo activo |
| 🌙 **Modo oscuro / claro** | Tema persistido automáticamente entre sesiones |
| 🎨 **6 colores de acento** | Personalización visual guardada con DataStore |
| 📶 **Modo offline real** | Detecta la red automáticamente y sincroniza la cola al reconectar |

---

## 🛠️ Tecnologías

- **Kotlin** — lenguaje principal
- **Jetpack Compose** — UI declarativa 100% nativa
- **Room** — base de datos local con migraciones versionadas
- **DataStore Preferences** — persistencia de preferencias de usuario
- **ViewModel + StateFlow** — arquitectura MVVM
- **ConnectivityManager** — detección automática de red
- **PdfDocument** — generación de PDF nativa sin dependencias externas
- **Material 3** — componentes y sistema de diseño

---

## 📦 Instalación del APK

1. Descarga el APK desde la sección [**Releases**](https://github.com/schwarmak-dev/control-de-horas/releases/latest).
2. En tu Android, ve a **Ajustes → Seguridad → Instalar apps de fuentes desconocidas** y actívalo (o permite la instalación cuando el sistema lo pregunte).
3. Abre el archivo `.apk` descargado y toca **Instalar**.
4. ¡Listo! La app aparece en tu menú de aplicaciones.

---

## 🧑‍💻 Compilar desde el código fuente

### Requisitos

- Android Studio Hedgehog o superior
- JDK 11
- Android SDK 35

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/schwarmak-dev/control-de-horas.git

# 2. Abrir en Android Studio
# File → Open → selecciona la carpeta clonada

# 3. Sincronizar Gradle
# Android Studio lo hace automáticamente al abrir

# 4. Ejecutar en emulador o dispositivo físico
# Presiona el botón ▶ Run
```

Para generar el APK de release: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

---

## 🗂️ Estructura del proyecto

```
app/src/main/java/com/schwarmakdev/controldehoras/
├── MainActivity.kt
├── NetworkMonitor.kt          # Detección automática de red
├── NotificationHelper.kt
├── PdfExporter.kt             # Generación de PDF nativo
├── data/
│   ├── dao/                   # Acceso a base de datos (Room)
│   ├── database/              # AppDatabase + migraciones
│   ├── entity/                # Modelos: Proyecto, SesionTiempo, etc.
│   └── repository/            # TimeTrackerRepository, PreferencesRepository
└── ui/
    ├── screens/               # TrackingScreen, DashboardScreen, CalendarScreen, ConfigurationScreen
    ├── theme/                 # Colores, tipografía, tema Material 3
    └── viewmodel/             # TimeTrackerViewModel
```

---

## 📄 Licencia

```
MIT License

Copyright (c) 2026 schwarmak-dev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<p align="center">Lo hice porque me daba paja anotar las horas de practica en un excel, asique mejor hice esto, por <a href="https://github.com/schwarmak-dev">schwarmak-dev</a></p>