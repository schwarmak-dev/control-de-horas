# Guía de Migración — Control de Horas
## com.example → com.schwarmakdev.controldehoras

---

## Qué cambió y por qué

| # | Problema | Solución aplicada |
|---|----------|-------------------|
| 1 | `MainActivity.kt` de 2 439 líneas | Dividido en 5 archivos: `TrackingScreen`, `DashboardScreen`, `CalendarScreen`, `ConfigurationScreen`, `UiUtils` |
| 2 | Namespace `com.example` | Renombrado a `com.schwarmakdev.controldehoras` en todos los archivos |
| 3 | Migración de datos frágil (strings hardcodeados) | Proyectos por defecto declarados limpiamente en el repositorio; migración de Room `MIGRATION_1_2` para el nuevo campo |
| 4 | `OfflineQueue` en memoria (se pierde al cerrar la app) | Nueva entidad `OfflineAction` persistida en Room; nuevo DAO + `OfflineActionDao` |
| 5 | `POST_NOTIFICATIONS` ausente del Manifest | Ya estaba presente; se aseguró que el permiso se solicita en runtime para Android 13+ (TIRAMISU) en `ConfigurationScreen` |
| Extra | ID del temporizador activo hardcodeado como string | Extraído como `TemporizadorActivo.ACTIVE_TIMER_ID` (constante compartida) |

---

## Pasos para aplicar en Android Studio

### 1. Crear el nuevo paquete
En Android Studio, clic derecho sobre `app/src/main/java` →  
**New → Package** → `com.schwarmakdev.controldehoras`

Crea los subpaquetes:
```
com.schwarmakdev.controldehoras
├── data/
│   ├── dao/
│   ├── database/
│   ├── entity/
│   └── repository/
├── ui/
│   ├── screens/
│   ├── theme/
│   └── viewmodel/
```

### 2. Copiar los archivos entregados
Copia cada `.kt` a su carpeta correspondiente dentro del nuevo paquete.

### 3. Actualizar `build.gradle.kts`
Reemplaza el archivo `app/build.gradle.kts` con el entregado.  
Luego haz **Sync Project with Gradle Files**.

### 4. Actualizar `AndroidManifest.xml`
Reemplaza el Manifest con el entregado (ya incluye `package` y `POST_NOTIFICATIONS`).

### 5. Base de datos — migración automática
El `AppDatabase` pasa de versión 1 a versión 2.  
La migración `MIGRATION_1_2` crea la tabla `offline_actions` sin tocar los datos existentes.  
**No se pierden proyectos ni sesiones.**

> ⚠️ Si usabas `fallbackToDestructiveMigration()` en la versión anterior,
> asegúrate de que ya **no** esté en la nueva `AppDatabase`. El archivo entregado lo elimina.

### 6. Eliminar los archivos viejos
Una vez verificado que la app compila y funciona, elimina:
- Toda la carpeta `com/example/`

### 7. Verificar el ID de aplicación en Google Play (si aplica)
El `applicationId` cambió de `com.example` a `com.schwarmakdev.controldehoras`.  
Si la app ya está publicada, esto implica una app nueva en la tienda.  
Para actualizaciones de una app existente, **mantén el applicationId anterior** y solo cambia el namespace.

---

## Estructura final de archivos

```
com/schwarmakdev/controldehoras/
├── MainActivity.kt                        (~120 líneas)
├── NotificationHelper.kt
├── data/
│   ├── dao/DAOs.kt                        (ProjectDao, TimeSessionDao, ActiveTimerDao, OfflineActionDao)
│   ├── database/AppDatabase.kt            (versión 2, MIGRATION_1_2)
│   ├── entity/
│   │   ├── Proyecto.kt
│   │   ├── SesionTiempo.kt
│   │   ├── TemporizadorActivo.kt
│   │   └── OfflineAction.kt               ← NUEVO
│   └── repository/TimeTrackerRepository.kt
└── ui/
    ├── screens/
    │   ├── UiUtils.kt                     (formatters + ProjectTimeDonutChart)
    │   ├── TrackingScreen.kt
    │   ├── DashboardScreen.kt
    │   ├── CalendarScreen.kt
    │   └── ConfigurationScreen.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    └── viewmodel/TimeTrackerViewModel.kt
```
