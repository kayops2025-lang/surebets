# ⚡ Plataforma EV Pro: Arbitraje Deportivo & Casino Advantage Play

Suite integral de monetización matemática de apuestas deportivas y juego con ventaja (+EV), disponible en versión **Móvil Nativa (Android Jetpack Compose)**, **Plataforma Web (Hub Unificado)** y **Herramientas de Escritorio (Python OCR)**.

---

## 📁 Arquitectura del Repositorio

```text
/
├── README.md                      # Documentación maestra del proyecto
├── index.html                     # Hub Central Web (Conmuta Deportes y Casino)
│
├── deportes/                      # [FASE 1: Arbitraje Deportivo]
│   └── index.html                 # The Odds API, Surebets, Parlays y Matched Betting
│
├── casino/                        # [FASE 2: Casino Advantage Play]
│   ├── index.html                 # Panel de Bonos, Slots y Blackjack Web
│   ├── app.js                     # Controlador del simulador de rollover
│   ├── modelo.js                  # Motor matemático (Monte Carlo, EV y BJ)
│   ├── datos.js                   # Base de datos de slots (FanDuel NJ, DraftKings)
│   ├── observaciones.js           # Gestor de persistencia local de sesiones
│   └── estilos.css                # Estilos visuales dark mode
│
├── tools/                         # [Herramientas de Escritorio]
│   ├── lector_demo.py             # Lector OCR de saldo en pantalla (Windows)
│   ├── lector_demo.bat            # Lanzador para Windows
│   ├── pruebas_lector.py          # Pruebas del lector OCR
│   └── pruebas.js                 # Tests del motor matemático
│
└── app/                           # [Aplicación Android Nativa]
    ├── build.gradle.kts           # Configuración Gradle (Namespace y ApplicationId)
    └── src/main/java/com/example/
        ├── MainActivity.kt        # Navegación con 4 pestañas en Compose
        ├── model/
        │   └── SurebetModels.kt   # Modelos de datos de arbitraje y cuotas
        ├── util/
        │   └── ArbitrageMath.kt   # Motor de cálculo y conversión de cuotas
        ├── data/
        │   └── MockOddsDataSource.kt # Proveedor de cuotas en vivo
        └── ui/
            ├── SurebetViewModel.kt     # Estado reactivo global
            └── screens/
                ├── DashboardScreen.kt      # Alertas de Surebets en vivo
                ├── CalculatorScreen.kt     # Calculadora de Stakes (2 y 3 vías)
                ├── CasinoRolloverScreen.kt # Simulador de Rollover y Bonos EV
                └── BlackjackScreen.kt      # Asesor de Estrategia Básica de Blackjack
```

---

## 📱 Módulos de la Aplicación Android (Jetpack Compose)

1. **Surebets (Pestaña 1)**:
   - Scanner de discrepancias de cuotas en vivo con cálculo de ROI%.
   - Filtros por deporte (Fútbol, Baloncesto, Tenis, NFL, MLB, MMA).
   - Filtros por porcentaje de ganancia mínima (>1%, >2%, >3%, >5%) y por casa de apuestas.
   - Botón directo para transferir la alerta a la calculadora.

2. **Calculadora de Stakes (Pestaña 2)**:
   - Modos 2 Vías (Head-to-Head) y 3 Vías (Fútbol 1X2).
   - Conversión automática entre cuotas Decimales y Americanas.
   - **Redondeo inteligente de stakes**: Ajusta las apuestas a valores enteros para evitar que los algoritmos de las casas limiten tu cuenta.
   - Desglose de cobro garantizado y beneficio neto en cualquier escenario.

3. **Casino Rollover EV (Pestaña 3)**:
   - Simula la liberación de bonos de depósito de casinos (FanDuel, DraftKings, etc.).
   - Entradas: Monto del bono, depósito, rollover (veces), contribución (%) y RTP de la slot.
   - Salidas: Requisito total, giros requeridos, costo de la casa (edge drag), **EV Neto ($)**, tiempo estimado y ganancia por hora.

4. **Asesor Profesional de Blackjack (Pestaña 4)**:
   - Motor de estrategia básica (Reglas S17, 8 Mazos, Doblar tras split - DAS).
   - Cartas físicas visibles de tamaño calibrado (la carta K y las figuras nunca se cortan).
   - Selector táctil de carta del crupier y cartas del jugador.
   - Respuesta instantánea y justificación estadística: **PEDIR (HIT)**, **PLANTARSE (STAND)**, **DOBLAR (DOUBLE)** o **SEPARAR (SPLIT)**.

---

## 🌐 Módulos Web

- **`/index.html`**: Hub principal que te permite alternar sin recargar entre Deportes y Casino.
- **`/deportes/index.html`**: Integración con *The Odds API* para buscar partidos reales por API Key.
- **`/casino/index.html`**: Simulador Monte Carlo de slots con gráfico Canvas y tabla de estrategia.

---

## 🛠️ Herramientas de Escritorio (`/tools/`)

- **`lector_demo.py`**: Utiliza Windows OCR para leer el saldo en pantalla de slots en modo demo y registrar spins, aciertos, multiplicadores y bonos en archivos CSV/JSON.

---

## 🚀 Próximas Mejoras Sugeridas

1. **Notificaciones Push en Android**: Alertas de audio y vibración cuando aparezca una Surebet superior al 3% de ROI.
2. **WebSockets en Tiempo Real**: Conexión a feeds de Betfair Exchange o Pinnacle para arbitrajes en vivo (*In-Play*).
3. **Persistencia Room Database**: Almacenar el historial de todas las apuestas y bonos completados para llevar contabilidad personal.
