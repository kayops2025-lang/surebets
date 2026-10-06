(function (raiz) {
  "use strict";

  // Desviación típica por giro (en múltiplos de la apuesta) para cada nivel de volatilidad.
  // Son aproximaciones: los proveedores publican la volatilidad como escala, no como número.
  const SIGMA_POR_VOLATILIDAD = { 1: 4, 2: 6, 3: 9, 4: 13, 5: 20 };
  const NOMBRES_VOLATILIDAD = { 1: "Baja", 2: "Media-baja", 3: "Media", 4: "Alta", 5: "Muy alta" };
  const HIT_POR_DEFECTO = 30;
  const VOLATILIDAD_POR_DEFECTO = 3;
  // hit = fracción de giros con cualquier premio > 0. No es la frecuencia de bonos.
  // Tiempo y giros internos de bono son estimaciones del modelo, no datos oficiales del slot.
  const SEGUNDOS_PAUSA_BONO = 2;
  const SEGUNDOS_BONO_BASE = 12;
  const LIMITE_GIROS_SESION = 1e6;

  const MULTIPLICADORES_BASE = [
    0.2, 0.4, 0.6, 0.8, 1, 1.5, 2, 3, 5, 8, 10, 15, 25, 50, 100, 200, 500, 1000, 2500, 5000,
  ];

  function tablaParaAlfa(alfa, hit, rtp, maxWin) {
    let base = MULTIPLICADORES_BASE;
    if (maxWin > 0) base = base.filter((m) => m <= maxWin);
    const pesos = base.map((m) => Math.pow(m, -alfa));
    const suma = pesos.reduce((a, b) => a + b, 0);
    const probs = pesos.map((p) => p / suma);
    const media = base.reduce((a, m, i) => a + m * probs[i], 0);
    const escala = rtp / (hit * media);
    const pagos = base.map((m) => m * escala);
    const segundo = pagos.reduce((a, m, i) => a + m * m * probs[i], 0) * hit;
    const sigma = Math.sqrt(Math.max(0, segundo - rtp * rtp));
    return { pagos, probs, sigma };
  }

  // Construye una tabla de pagos aproximada que respeta el retorno y la frecuencia de acierto,
  // y cuya desviación por giro se acerca a la del nivel de volatilidad.
  function construirSlot(opts) {
    opts = opts || {};
    const r = (opts.rtp) / 100;
    const h = (opts.hit || HIT_POR_DEFECTO) / 100;
    const vol = opts.volatilidad || VOLATILIDAD_POR_DEFECTO;
    const maxWin = opts.maxWin;
    const objetivo = SIGMA_POR_VOLATILIDAD[vol];
    let bajo = 0.3;
    let alto = 4;
    for (let i = 0; i < 60; i++) {
      const medio = (bajo + alto) / 2;
      if (tablaParaAlfa(medio, h, r, maxWin).sigma > objetivo) bajo = medio;
      else alto = medio;
    }
    const tabla = tablaParaAlfa((bajo + alto) / 2, h, r, maxWin);
    const acumulada = [];
    let acum = 1 - h;
    for (const p of tabla.probs) {
      acum += p * h;
      acumulada.push(acum);
    }
    acumulada[acumulada.length - 1] = 1;
    const bono = estimarBono(tabla.pagos, tabla.probs, h, {
      frecuenciaBono: opts.frecuenciaBono,
      tipoDatoBono: opts.tipoDatoBono,
    });
    const duracionBono = opts.duracionBono > 0 ? opts.duracionBono : null;
    const spinsBono = opts.spinsBono > 0 ? opts.spinsBono : null;
    const tipoSiDato = (valor, tipo) => (valor == null ? "estimado" : tipo === "observado" ? "observado" : "real");
    return {
      rtp: r,
      hit: h,
      pagos: tabla.pagos,
      acumulada,
      fallo: 1 - h,
      sigma: tabla.sigma,
      sigmaObjetivo: objetivo,
      umbralBono: bono.umbralBono,
      pBonoEstimada: bono.pBono,
      pBono: bono.pBono,
      tipoDatoBono: bono.tipoDatoBono,
      objetivoBono: bono.objetivoBono,
      fuenteBono: bono.tipoDatoBono === "estimado"
        ? "estimado del modelo (cola de la tabla sintética; no es la frecuencia real del slot)"
        : bono.tipoDatoBono === "observado"
          ? "observado en sesiones"
          : "dato real/oficial del slot",
      duracionBono,
      spinsBono,
      tipoDuracionBono: tipoSiDato(duracionBono, opts.tipoDatoBono),
      tipoSpinsBono: tipoSiDato(spinsBono, opts.tipoDatoBono),
      rtpBase: opts.rtpBase > 0 ? opts.rtpBase : null,
      rtpBono: opts.rtpBono > 0 ? opts.rtpBono : null,
      fuenteDatos: opts.fuenteDatos || "",
      fechaDatos: opts.fechaDatos || "",
      version: opts.version || "",
    };
  }

  // Si hay frecuenciaBono (porcentaje de giros), se usa. Si no, fallback interno del modelo.
  function estimarBono(pagos, probs, hit, opts) {
    opts = opts || {};
    const tieneFreq = opts.frecuenciaBono != null && opts.frecuenciaBono > 0;
    const tipoDatoBono = !tieneFreq ? "estimado" : opts.tipoDatoBono === "observado" ? "observado" : "real";
    const objetivo = tieneFreq
      ? opts.frecuenciaBono / 100
      : Math.min(0.012, Math.max(hit * 0.03, 0.001));
    let masa = 0;
    let umbral = pagos[pagos.length - 1];
    for (let i = pagos.length - 1; i >= 0; i--) {
      masa += probs[i] * hit;
      umbral = pagos[i];
      if (masa >= objetivo) break;
    }
    return { umbralBono: umbral, pBono: masa, tipoDatoBono, objetivoBono: objetivo };
  }

  function girar(slot, aleatorio) {
    const u = aleatorio();
    if (u < slot.fallo) return 0;
    const acum = slot.acumulada;
    let lo = 0;
    let hi = acum.length - 1;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (u < acum[mid]) hi = mid;
      else lo = mid + 1;
    }
    return slot.pagos[lo];
  }

  function resultadoGiro(slot, aleatorio) {
    const mult = girar(slot, aleatorio);
    if (mult <= 0) return { tipo: "fallo", mult: 0 };
    if (slot.umbralBono > 0 && mult + 1e-12 >= slot.umbralBono) return { tipo: "bono", mult };
    return { tipo: "premio", mult };
  }

  function tiempoDeBono(slot, resultadoBono, p) {
    slot = slot || {};
    p = p || {};
    const mult = resultadoBono && Number.isFinite(resultadoBono.mult) ? resultadoBono.mult : 0;
    const duracion = slot.duracionBono > 0 ? slot.duracionBono : p.duracionBono > 0 ? p.duracionBono : null;
    const spins = slot.spinsBono > 0 ? slot.spinsBono : p.spinsBono > 0 ? p.spinsBono : null;

    let pausa;
    let juego;
    let tipoDuracion;
    if (duracion != null) {
      pausa = 0;
      juego = duracion;
      tipoDuracion = slot.tipoDuracionBono || (slot.tipoDatoBono === "observado" ? "observado" : "real");
    } else {
      pausa = p.segundosPausaBono > 0 ? p.segundosPausaBono : SEGUNDOS_PAUSA_BONO;
      const base = p.segundosBono > 0 ? p.segundosBono : SEGUNDOS_BONO_BASE;
      const extra = Math.min(30, Math.log2(1 + Math.max(0, mult)) * 1.5);
      juego = base + extra;
      tipoDuracion = "estimado";
    }

    let spinsBonus;
    let tipoSpins;
    if (spins != null) {
      spinsBonus = spins;
      tipoSpins = slot.tipoSpinsBono || (slot.tipoDatoBono === "observado" ? "observado" : "real");
    } else {
      spinsBonus = Math.max(6, Math.min(25, Math.round(juego / 1.5)));
      tipoSpins = "estimado";
    }
    return { pausa, juego, spinsBonus, tiempo: pausa + juego, tipoDuracion, tipoSpins };
  }

  function tiempoBonoEsperado(slot, p) {
    const muestra = tiempoDeBono(slot, { mult: 0 }, p);
    if ((slot && slot.duracionBono > 0) || (p && p.duracionBono > 0)) {
      return { ...muestra, tiempoPorBono: muestra.tiempo, spinsPorBono: muestra.spinsBonus };
    }
    let sumaT = 0;
    let sumaS = 0;
    let masa = 0;
    let prev = slot.fallo;
    for (let i = 0; i < slot.pagos.length; i++) {
      const pi = slot.acumulada[i] - prev;
      prev = slot.acumulada[i];
      if (slot.pagos[i] + 1e-12 >= slot.umbralBono) {
        const t = tiempoDeBono(slot, { tipo: "bono", mult: slot.pagos[i] }, p);
        sumaT += pi * t.tiempo;
        sumaS += pi * t.spinsBonus;
        masa += pi;
      }
    }
    if (masa <= 0) return { ...muestra, tiempoPorBono: muestra.tiempo, spinsPorBono: muestra.spinsBonus };
    return {
      pausa: muestra.pausa,
      juego: sumaT / masa - muestra.pausa,
      tiempo: sumaT / masa,
      tiempoPorBono: sumaT / masa,
      spinsBonus: sumaS / masa,
      spinsPorBono: sumaS / masa,
      tipoDuracion: "estimado",
      tipoSpins: muestra.tipoSpins,
    };
  }

  function requisitoTotal(p) {
    const base = p.baseRequisito === "bono+deposito" ? p.bono + p.deposito : p.bono;
    return base * p.requisito;
  }

  function avancePorGiro(p) {
    const cuenta = p.apuesta + (p.extraCuenta ? p.extra : 0);
    return cuenta * (p.contribucion / 100);
  }

  // Cuenta rápida: ignora la quiebra, el tope de retiro y los bonos no retirables.
  function evAnalitico(p) {
    const requisito = requisitoTotal(p);
    const avance = avancePorGiro(p);
    const giros = avance > 0 ? requisito / avance : Infinity;
    const costoJuego = Number.isFinite(giros) ? giros * p.apuesta * (1 - p.rtp / 100) : Infinity;
    const costoExtra = Number.isFinite(giros) ? giros * p.extra * (1 - p.retornoExtra / 100) : Infinity;
    const apostado = Number.isFinite(giros) ? giros * (p.apuesta + p.extra) : Infinity;
    const ev = p.bono - costoJuego - costoExtra;
    const slot = construirSlot(p);
    const segundos = p.segundos > 0 ? p.segundos : 0;
    const tBono = tiempoBonoEsperado(slot, p);
    const tiempoSpins = Number.isFinite(giros) ? giros * segundos : Infinity;
    const tiempoBonuses = Number.isFinite(giros) ? giros * slot.pBonoEstimada * tBono.tiempoPorBono : Infinity;
    return {
      requisito,
      giros,
      apostado,
      costoJuego,
      costoExtra,
      ev,
      pBonoEstimada: slot.pBonoEstimada,
      tipoDatoBono: slot.tipoDatoBono,
      tipoDuracionBono: slot.tipoDuracionBono,
      tipoSpinsBono: slot.tipoSpinsBono,
      tiempoPorBono: tBono.tiempoPorBono,
      tiempoSpins,
      tiempoBonuses,
      tiempoTotal: tiempoSpins + tiempoBonuses,
    };
  }

  const PROB_JACKPOT = 1e-5;

  function simularUna(p, slot, aleatorio) {
    const requisito = requisitoTotal(p);
    const avance = avancePorGiro(p);
    const costoGiro = p.apuesta + p.extra;
    const premioJackpot = p.extra > 0 ? (p.extra * (p.retornoExtra / 100)) / PROB_JACKPOT : 0;
    const segundos = p.segundos > 0 ? p.segundos : 0;
    let saldo = p.bono + p.deposito;
    let avanzado = 0;
    let girosNormales = 0;
    let bonusesJugados = 0;
    let spinsBonus = 0;
    let apostadoBase = 0;
    let apostadoExtra = 0;
    let premiosBase = 0;
    let premiosBonus = 0;
    let premiosJackpot = 0;
    let tiempoSpins = 0;
    let tiempoBonuses = 0;
    let quiebra = false;

    while (avanzado < requisito - 1e-9 && girosNormales < LIMITE_GIROS_SESION) {
      if (saldo < costoGiro - 1e-9) {
        quiebra = true;
        break;
      }
      saldo -= costoGiro;
      apostadoBase += p.apuesta;
      apostadoExtra += p.extra;
      girosNormales += 1;
      tiempoSpins += segundos;
      avanzado += avance;

      const r = resultadoGiro(slot, aleatorio);
      if (r.tipo === "bono") {
        bonusesJugados += 1;
        const tb = tiempoDeBono(slot, r, p);
        tiempoBonuses += tb.tiempo;
        spinsBonus += tb.spinsBonus;
        const premio = p.apuesta * r.mult;
        saldo += premio;
        premiosBonus += premio;
      } else if (r.mult > 0) {
        const premio = p.apuesta * r.mult;
        saldo += premio;
        premiosBase += premio;
      }

      if (premioJackpot > 0 && aleatorio() < PROB_JACKPOT) {
        saldo += premioJackpot;
        premiosJackpot += premioJackpot;
      }
    }

    if (!quiebra && avanzado < requisito - 1e-9) quiebra = true;

    let saldoRetirable = saldo;
    if (!p.retirable) saldoRetirable = Math.max(0, saldo - p.bono);
    if (p.topeRetiro > 0) saldoRetirable = Math.min(saldoRetirable, p.topeRetiro);
    if (saldo < 0) saldo = 0;
    if (saldoRetirable < 0) saldoRetirable = 0;

    return {
      ganancia: saldoRetirable - p.deposito,
      giros: girosNormales,
      girosNormales,
      bonusesJugados,
      spinsBonus,
      quiebra,
      completo: !quiebra,
      saldoFinal: saldo,
      saldoRetirable,
      apostadoBase,
      apostadoExtra,
      apostadoTotal: apostadoBase + apostadoExtra,
      volumenQueCuenta: avanzado,
      volumenPendiente: Math.max(0, requisito - avanzado),
      tiempoSpins,
      tiempoBonuses,
      tiempoTotal: tiempoSpins + tiempoBonuses,
      premiosBase,
      premiosBonus,
      premiosJackpot,
    };
  }

  function nuevaSimulacion(p, aleatorio) {
    const slot = construirSlot(p);
    const rnd = aleatorio || Math.random;
    const ganancias = [];
    let quiebras = 0;
    let sumaGiros = 0;
    let sumaBonos = 0;
    let sumaSpinsBonus = 0;
    let sumaTiempoSpins = 0;
    let sumaTiempoBonuses = 0;
    let sumaApostado = 0;
    let sumaApostadoBase = 0;
    let sumaApostadoExtra = 0;
    let sumaVolumen = 0;
    let sumaSaldo = 0;
    return {
      slot,
      correr(n) {
        for (let i = 0; i < n; i++) {
          const r = simularUna(p, slot, rnd);
          ganancias.push(r.ganancia);
          sumaGiros += r.girosNormales;
          sumaBonos += r.bonusesJugados;
          sumaSpinsBonus += r.spinsBonus;
          sumaTiempoSpins += r.tiempoSpins;
          sumaTiempoBonuses += r.tiempoBonuses;
          sumaApostado += r.apostadoTotal;
          sumaApostadoBase += r.apostadoBase;
          sumaApostadoExtra += r.apostadoExtra;
          sumaVolumen += r.volumenQueCuenta;
          sumaSaldo += r.saldoRetirable;
          if (r.quiebra) quiebras++;
        }
      },
      hechas() {
        return ganancias.length;
      },
      resumen() {
        const n = ganancias.length;
        if (!n) {
          return {
            n: 0, media: 0, desviacion: 0, errorMedia: 0, quiebra: 0, completado: 0, positivas: 0,
            girosPromedio: 0, bonusesPromedio: 0, spinsBonusPromedio: 0,
            p10: 0, p25: 0, p50: 0, p75: 0, p90: 0, min: 0, max: 0,
            apostadoPromedio: 0, apostadoBasePromedio: 0, apostadoExtraPromedio: 0,
            volumenPromedio: 0, tiempoSpinsPromedio: 0, tiempoBonusesPromedio: 0, tiempoPromedio: 0,
            saldoFinalPromedio: 0, costeRollover: 0, ganancias: new Float64Array(0),
            pBonoEstimada: slot.pBonoEstimada,
            tipoDatoBono: slot.tipoDatoBono,
          };
        }
        const ordenadas = Float64Array.from(ganancias).sort();
        const media = ordenadas.reduce((a, b) => a + b, 0) / n;
        const varianza = ordenadas.reduce((a, b) => a + (b - media) * (b - media), 0) / n;
        const percentil = (q) => ordenadas[Math.min(n - 1, Math.max(0, Math.floor(q * n)))];
        const positivas = ordenadas.filter((g) => g > 0).length;
        return {
          n,
          media,
          desviacion: Math.sqrt(varianza),
          errorMedia: Math.sqrt(varianza / n),
          quiebra: quiebras / n,
          completado: (n - quiebras) / n,
          positivas: positivas / n,
          girosPromedio: sumaGiros / n,
          bonusesPromedio: sumaBonos / n,
          spinsBonusPromedio: sumaSpinsBonus / n,
          p10: percentil(0.1),
          p25: percentil(0.25),
          p50: percentil(0.5),
          p75: percentil(0.75),
          p90: percentil(0.9),
          min: ordenadas[0],
          max: ordenadas[n - 1],
          apostadoPromedio: sumaApostado / n,
          apostadoBasePromedio: sumaApostadoBase / n,
          apostadoExtraPromedio: sumaApostadoExtra / n,
          volumenPromedio: sumaVolumen / n,
          tiempoSpinsPromedio: sumaTiempoSpins / n,
          tiempoBonusesPromedio: sumaTiempoBonuses / n,
          tiempoPromedio: (sumaTiempoSpins + sumaTiempoBonuses) / n,
          saldoFinalPromedio: sumaSaldo / n,
          costeRollover: p.bono - media,
          ganancias: ordenadas,
          pBonoEstimada: slot.pBonoEstimada,
          tipoDatoBono: slot.tipoDatoBono,
          tipoDuracionBono: slot.tipoDuracionBono,
          tipoSpinsBono: slot.tipoSpinsBono,
          tiempoPorBono: tiempoBonoEsperado(slot, p).tiempoPorBono,
        };
      },
    };
  }

  // Estrategia básica: 8 mazos, el crupier se planta en 17 suave, doblar con dos cartas y tras separar.
  const CRUPIER = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "A"];
  const DURAS = {
    8: "HHHHHHHHHH",
    9: "HDDDDHHHHH",
    10: "DDDDDDDDHH",
    11: "DDDDDDDDDH",
    12: "HHSSSHHHHH",
    13: "SSSSSHHHHH",
    14: "SSSSSHHHHH",
    15: "SSSSSHHHHH",
    16: "SSSSSHHHHH",
    17: "SSSSSSSSSS",
  };
  const BLANDAS = {
    13: "HHHDDHHHHH",
    14: "HHHDDHHHHH",
    15: "HHDDDHHHHH",
    16: "HHDDDHHHHH",
    17: "HDDDDHHHHH",
    18: "SXXXXSSHHH",
    19: "SSSSSSSSSS",
    20: "SSSSSSSSSS",
    21: "SSSSSSSSSS",
  };
  const PARES = {
    2: "PPPPPPHHHH",
    3: "PPPPPPHHHH",
    4: "HHHPPHHHHH",
    6: "PPPPPHHHHH",
    7: "PPPPPPHHHH",
    8: "PPPPPPPPPP",
    9: "PPPPPSPPSS",
    10: "SSSSSSSSSS",
    A: "PPPPPPPPPP",
  };

  function valorCarta(c) {
    if (c === "A") return 11;
    if (c === "J" || c === "Q" || c === "K") return 10;
    return Number(c);
  }

  function evaluarMano(cartas) {
    let total = 0;
    let ases = 0;
    for (const c of cartas) {
      total += valorCarta(c);
      if (c === "A") ases++;
    }
    while (total > 21 && ases > 0) {
      total -= 10;
      ases--;
    }
    return { total, blanda: ases > 0 };
  }

  function codigoTabla(tipo, clave, crupier) {
    const col = CRUPIER.indexOf(crupier);
    if (tipo === "par") return PARES[clave][col];
    if (tipo === "blanda") return BLANDAS[clave][col];
    const t = Math.min(17, Math.max(8, clave));
    return DURAS[t][col];
  }

  function estrategiaBasica(cartas, crupier) {
    const normal = cartas.map((c) => (c === "J" || c === "Q" || c === "K" ? "10" : c));
    const { total, blanda } = evaluarMano(normal);
    const dosCartas = normal.length === 2;
    if (total > 21) return { total, blanda, tipo: "pasada", accion: "PASADO" };
    if (dosCartas && total === 21) return { total, blanda, tipo: "blackjack", accion: "BLACKJACK" };
    let tipo;
    let clave;
    if (dosCartas && normal[0] === normal[1] && normal[0] !== "5") {
      tipo = "par";
      clave = normal[0];
    } else if (blanda && total >= 13) {
      tipo = "blanda";
      clave = total;
    } else {
      tipo = "dura";
      clave = total;
    }
    const codigo = codigoTabla(tipo, clave, crupier);
    let accion;
    if (codigo === "H") accion = "PEDIR";
    else if (codigo === "S") accion = "PLANTARSE";
    else if (codigo === "P") accion = "SEPARAR";
    else if (codigo === "D") accion = dosCartas ? "DOBLAR" : "PEDIR";
    else accion = dosCartas ? "DOBLAR" : "PLANTARSE";
    return { total, blanda, tipo, clave, codigo, accion };
  }

  const api = {
    SIGMA_POR_VOLATILIDAD,
    NOMBRES_VOLATILIDAD,
    HIT_POR_DEFECTO,
    VOLATILIDAD_POR_DEFECTO,
    SEGUNDOS_PAUSA_BONO,
    SEGUNDOS_BONO_BASE,
    CRUPIER,
    DURAS,
    BLANDAS,
    PARES,
    construirSlot,
    girar,
    resultadoGiro,
    tiempoDeBono,
    tiempoBonoEsperado,
    evAnalitico,
    simularUna,
    nuevaSimulacion,
    estrategiaBasica,
  };

  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else raiz.Modelo = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
