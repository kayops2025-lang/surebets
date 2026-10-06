"""Registra giros de un slot en modo demo leyendo el saldo en pantalla.

Solo observa: no pulsa nada ni sugiere jugadas. Cada vez que el saldo baja lo que vale
el coste del giro (apuesta base + extra jackpot) cuenta un giro pagado; lo que sube
después se anota como premio de ese giro.

Los free spins no se detectan solos. Hay que marcar BONUS INICIO y BONUS FIN a mano.

Uso:
    python lector_demo.py --juego "Wolf It Up" --apuesta-base 0.10 --extra-jackpot 0.20
"""

import argparse
import csv
import ctypes
import json
import math
import re
import sys
import time
from datetime import datetime
from pathlib import Path

CARPETA = Path(__file__).resolve().parent / "registros"
ARCHIVO_REGION = CARPETA / "ultima_region.json"
TOLERANCIA = 0.011


def leer_numero(texto):
    """Devuelve el último monto que aparece en el texto, en formato de Estados Unidos (1,234.56)."""
    candidatos = re.findall(r"\d[\d,]*(?:\.\d+)?", texto.replace(" ", ""))
    for c in reversed(candidatos):
        limpio = c.replace(",", "")
        try:
            return round(float(limpio), 2)
        except ValueError:
            continue
    return None


class Registro:
    """Convierte la secuencia de saldos estables en giros con su premio."""

    def __init__(self, apuesta=None, al_cerrar=None):
        self.apuesta = apuesta
        self.al_cerrar = al_cerrar or (lambda giro: None)
        self.saldo = None
        self.abierto = None
        self.giros = 0
        self.aciertos = 0
        self.apostado = 0.0
        self.pagado = 0.0
        self.mayor = 0.0
        self.descartes = 0
        self.pendiente = None
        self.rechazado = None
        self.veces_rechazado = 0

    def lectura(self, valor):
        if valor is None:
            return
        if self.saldo is not None:
            d = valor - self.saldo
            if abs(d) < 0.005:
                self.pendiente = None
                return
            if self.apuesta is not None and abs(-d - self.apuesta) <= TOLERANCIA:
                self.pendiente = None
                self.nuevo_saldo(valor)
                return
        if valor == self.pendiente:
            self.pendiente = None
            self.nuevo_saldo(valor)
        else:
            self.pendiente = valor

    def _abrir(self, saldo_antes):
        self._cerrar()
        self.abierto = {"apuesta": self.apuesta, "ganancia": 0.0, "saldo_antes": saldo_antes}

    def _cerrar(self):
        g = self.abierto
        if g is None:
            return
        self.abierto = None
        g["ganancia"] = round(g["ganancia"], 2)
        self.giros += 1
        self.apostado += g["apuesta"]
        self.pagado += g["ganancia"]
        if g["ganancia"] > 0:
            self.aciertos += 1
            self.mayor = max(self.mayor, g["ganancia"] / g["apuesta"])
        g["numero"] = self.giros
        self.al_cerrar(g)

    def nuevo_saldo(self, saldo):
        if self.saldo is None:
            self.saldo = saldo
            return
        d = round(saldo - self.saldo, 2)
        anterior = self.saldo
        if abs(d) < 0.005:
            return
        if d < 0:
            if self.apuesta is None:
                self.apuesta = -d
            k = math.ceil(-d / self.apuesta - TOLERANCIA)
            if k > 50:
                if saldo == self.rechazado:
                    self.veces_rechazado += 1
                else:
                    self.rechazado, self.veces_rechazado = saldo, 1
                if self.veces_rechazado >= 5:
                    self._cerrar()
                    self.saldo = saldo
                else:
                    self.descartes += 1
                return
            self.saldo = saldo
            premio = round(k * self.apuesta + d, 2)
            for i in range(k):
                self._abrir(anterior - i * self.apuesta)
            if premio > TOLERANCIA:
                self.abierto["ganancia"] += premio
        else:
            self.saldo = saldo
            if self.abierto is not None:
                self.abierto["ganancia"] += d

    def terminar(self):
        self._cerrar()

    def frecuencia(self):
        if not self.giros:
            return 0.0, 0.0
        p = self.aciertos / self.giros
        return p, 1.96 * math.sqrt(p * (1 - p) / self.giros)


def leer_apuesta(texto):
    limpio = texto.strip().replace("$", "").replace(",", ".")
    if limpio == "":
        return None
    try:
        valor = float(limpio)
    except ValueError:
        raise argparse.ArgumentTypeError(f"no entiendo la apuesta {texto!r}; escríbela como 0.15")
    if valor <= 0:
        raise argparse.ArgumentTypeError("la apuesta tiene que ser mayor que cero")
    return round(valor, 2)


def leer_extra(texto):
    limpio = texto.strip().replace("$", "").replace(",", ".")
    if limpio == "":
        return 0.0
    try:
        valor = float(limpio)
    except ValueError:
        raise argparse.ArgumentTypeError(f"no entiendo el extra {texto!r}; escríbelo como 0.20")
    if valor < 0:
        raise argparse.ArgumentTypeError("el extra jackpot no puede ser negativo")
    return round(valor, 2)


CSV_CAMPOS = [
    "giro", "hora", "apuesta", "apuesta_base", "extra_jackpot",
    "ganancia", "multiplicador", "saldo_antes",
]


def _hora_corta():
    return datetime.now().strftime("%H:%M:%S")


def _opcional_int(texto):
    if texto is None:
        return None
    t = str(texto).strip()
    if t == "":
        return None
    try:
        return int(float(t.replace(",", ".")))
    except ValueError:
        return None


def _opcional_float(texto):
    if texto is None:
        return None
    t = str(texto).strip().replace("$", "").replace(",", ".")
    if t == "":
        return None
    try:
        return round(float(t), 2)
    except ValueError:
        return None


class Sesion:
    def __init__(
        self,
        apuesta_base=None,
        extra_jackpot=0.0,
        extra_cuenta_rollover=True,
        free_spins_cuentan_rollover=False,
        juego="",
        casino="",
        estado="",
        proveedor="",
        al_cerrar_giro=None,
        reloj=None,
    ):
        self.apuesta_base = apuesta_base
        self.extra_jackpot = extra_jackpot if extra_jackpot is not None else 0.0
        self.extra_cuenta_rollover = extra_cuenta_rollover
        self.free_spins_cuentan_rollover = free_spins_cuentan_rollover
        self.juego = juego or ""
        self.casino = casino or ""
        self.estado = estado or ""
        self.proveedor = proveedor or ""
        self.fecha = datetime.now().strftime("%Y-%m-%d")
        self.hora_inicio = datetime.now().isoformat(timespec="seconds")
        self.reloj = reloj or time.time
        self._al_cerrar_giro = al_cerrar_giro or (lambda giro: None)
        coste = None
        if apuesta_base is not None:
            coste = round(apuesta_base + self.extra_jackpot, 2)
        self.registro = Registro(coste, self._al_cerrar)
        self.giros = []
        self.bonos = []
        self.bono_abierto = None
        self.saldo_inicial = None
        self.saldo_final = None

    def _coste(self):
        if self.registro.apuesta is not None:
            return self.registro.apuesta
        if self.apuesta_base is not None:
            return round(self.apuesta_base + self.extra_jackpot, 2)
        return None

    def _base_efectiva(self):
        if self.apuesta_base is not None:
            return self.apuesta_base
        coste = self._coste()
        if coste is None:
            return None
        estimado = round(coste - self.extra_jackpot, 2)
        return estimado if estimado > 0 else None

    def volumen_por_giro_pagado(self):
        base = self._base_efectiva()
        if base is None:
            return 0.0
        extra = self.extra_jackpot if self.extra_cuenta_rollover else 0.0
        return round(base + extra, 2)

    def _volumen_free_spins(self):
        if not self.free_spins_cuentan_rollover:
            return 0.0
        base = self._base_efectiva()
        if base is None:
            return 0.0
        total = 0.0
        for b in self.bonos:
            if b.get("spinsInternos") is None:
                continue
            total += b["spinsInternos"] * base
        return round(total, 2)

    def _actualizar_saldos(self):
        if self.registro.saldo is None:
            return
        if self.saldo_inicial is None:
            self.saldo_inicial = self.registro.saldo
        self.saldo_final = self.registro.saldo

    def lectura(self, valor):
        self.registro.lectura(valor)
        self._actualizar_saldos()

    def nuevo_saldo(self, saldo):
        self.registro.nuevo_saldo(saldo)
        self._actualizar_saldos()

    def _inferir_base(self, coste):
        if self.apuesta_base is not None or coste is None:
            return
        estimado = round(coste - self.extra_jackpot, 2)
        if estimado > 0:
            self.apuesta_base = estimado

    def _al_cerrar(self, g):
        self._inferir_base(g["apuesta"])
        coste = g["apuesta"]
        fila = {
            "giro": g["numero"],
            "hora": _hora_corta(),
            "apuesta": coste,
            "apuesta_base": self.apuesta_base,
            "extra_jackpot": self.extra_jackpot,
            "ganancia": g["ganancia"],
            "multiplicador": round(g["ganancia"] / coste, 4) if coste else 0.0,
            "saldo_antes": g["saldo_antes"],
            "pagado": True,
            "volumen_rollover": self.volumen_por_giro_pagado(),
        }
        self.giros.append(fila)
        self._al_cerrar_giro(fila)

    def giro_actual(self):
        if self.registro.abierto is not None:
            return self.registro.giros + 1
        return self.registro.giros

    def bonus_inicio(self):
        if self.bono_abierto is not None:
            return self.bono_abierto
        bono = {
            "inicioGiro": self.giro_actual(),
            "horaInicio": _hora_corta(),
            "tInicio": self.reloj(),
            "saldoAntes": self.registro.saldo,
            "saldoDespues": None,
            "horaFin": None,
            "tFin": None,
            "duracionSegundos": None,
            "duracionCorregida": None,
            "premioAproximado": None,
            "premio": None,
            "spinsInternos": None,
        }
        self.bono_abierto = bono
        self.bonos.append(bono)
        return bono

    def bonus_fin(self):
        if self.bono_abierto is None:
            return None
        b = self.bono_abierto
        ahora = self.reloj()
        b["tFin"] = ahora
        b["horaFin"] = _hora_corta()
        b["saldoDespues"] = self.registro.saldo
        if b["tInicio"] is not None:
            b["duracionSegundos"] = round(ahora - b["tInicio"], 2)
        if b["saldoAntes"] is not None and b["saldoDespues"] is not None:
            b["premioAproximado"] = round(b["saldoDespues"] - b["saldoAntes"], 2)
        self.bono_abierto = None
        return b

    def completar_bonus(self, spins_internos=None, duracion=None, premio=None, indice=None):
        if not self.bonos:
            return None
        b = self.bonos[indice] if indice is not None else self.bonos[-1]
        n = _opcional_int(spins_internos)
        if n is not None:
            b["spinsInternos"] = n
        d = _opcional_float(duracion)
        if d is not None:
            b["duracionCorregida"] = d
        p = _opcional_float(premio)
        if p is not None:
            b["premio"] = p
        return b

    def terminar(self):
        if self.bono_abierto is not None:
            self.bonus_fin()
        self.registro.terminar()
        self._actualizar_saldos()

    def _suma_bonos(self, elegir):
        if not self.bonos:
            return 0
        valores = []
        for b in self.bonos:
            v = elegir(b)
            if v is None:
                return None
            valores.append(v)
        return round(sum(valores), 2)

    def resumen(self):
        giros_pagados = len(self.giros)
        volumen_apostado = round(sum((g["apuesta"] or 0) for g in self.giros), 2)
        volumen_rollover = round(
            sum((g.get("volumen_rollover") or 0) for g in self.giros) + self._volumen_free_spins(),
            2,
        )
        premios_giros = round(sum(g["ganancia"] for g in self.giros), 2)
        if self.saldo_inicial is not None and self.saldo_final is not None:
            premios_totales = round(self.saldo_final - self.saldo_inicial + volumen_apostado, 2)
        else:
            premios_totales = premios_giros
        mayor = max((g["ganancia"] for g in self.giros), default=None)
        aciertos = sum(1 for g in self.giros if g["ganancia"] > 0)
        hit = round(aciertos / giros_pagados, 4) if giros_pagados else None
        retorno = round(premios_totales / volumen_apostado, 4) if volumen_apostado else None
        premio_b = lambda b: b["premio"] if b.get("premio") is not None else b.get("premioAproximado")
        dur_b = lambda b: b["duracionCorregida"] if b.get("duracionCorregida") is not None else b.get("duracionSegundos")
        return {
            "juego": self.juego,
            "casino": self.casino,
            "estado": self.estado,
            "fecha": self.fecha,
            "saldoInicial": self.saldo_inicial,
            "saldoFinal": self.saldo_final,
            "apuestaBase": self.apuesta_base,
            "extraJackpot": self.extra_jackpot,
            "costePorGiro": self._coste(),
            "spinsPagados": giros_pagados,
            "volumenApostado": volumen_apostado,
            "volumenRollover": volumen_rollover,
            "extraCuentaRollover": self.extra_cuenta_rollover,
            "freeSpinsCuentanRollover": self.free_spins_cuentan_rollover,
            "premiosTotales": premios_totales,
            "bonosActivados": len(self.bonos),
            "spinsDondeSeActivoBonus": [b["inicioGiro"] for b in self.bonos],
            "spinsInternosBonus": self._suma_bonos(lambda b: b.get("spinsInternos")),
            "duracionBonus": self._suma_bonos(dur_b),
            "premiosBonus": self._suma_bonos(premio_b),
            "hitRateObservado": hit,
            "retornoObservado": retorno,
            "mayorPremio": mayor,
            "tipoDato": "observado",
        }

    def _bono_publico(self, b):
        return {
            "inicioGiro": b["inicioGiro"],
            "horaInicio": b["horaInicio"],
            "horaFin": b["horaFin"],
            "duracionSegundos": b["duracionSegundos"],
            "duracionCorregida": b["duracionCorregida"],
            "saldoAntes": b["saldoAntes"],
            "saldoDespues": b["saldoDespues"],
            "premioAproximado": b["premioAproximado"],
            "premio": b["premio"],
            "spinsInternos": b["spinsInternos"],
        }

    def a_json(self):
        r = self.resumen()
        return {
            "sesion": {
                "juego": self.juego,
                "casino": self.casino,
                "estado": self.estado,
                "proveedor": self.proveedor,
                "fecha": self.fecha,
                "horaInicio": self.hora_inicio,
                "tipoDato": "observado",
            },
            "datosGenerales": {
                "apuestaBase": self.apuesta_base,
                "extraJackpot": self.extra_jackpot,
                "costePorGiro": self._coste(),
                "extraCuentaRollover": self.extra_cuenta_rollover,
                "freeSpinsCuentanRollover": self.free_spins_cuentan_rollover,
                "saldoInicial": self.saldo_inicial,
                "saldoFinal": self.saldo_final,
            },
            "giros": list(self.giros),
            "bonos": [self._bono_publico(b) for b in self.bonos],
            "resumen": r,
        }

    def a_observacion(self):
        r = self.resumen()
        spins = r["spinsDondeSeActivoBonus"]
        return {
            "fecha": r["fecha"],
            "juego": r["juego"],
            "casino": r["casino"],
            "estado": r["estado"],
            "proveedor": self.proveedor,
            "rtpOficial": None,
            "slotId": "",
            "apuestaBase": r["apuestaBase"],
            "extraJackpot": r["extraJackpot"],
            "costePorSpin": r["costePorGiro"],
            "spinsObservados": r["spinsPagados"],
            "bonosActivados": r["bonosActivados"],
            "spinsDondeSeActivoBonus": ",".join(str(n) for n in spins) if spins else "",
            "spinsInternosBonus": r["spinsInternosBonus"],
            "duracionBonus": r["duracionBonus"],
            "premiosBonus": r["premiosBonus"],
            "premiosTotales": r["premiosTotales"],
            "saldoInicial": r["saldoInicial"],
            "saldoFinal": r["saldoFinal"],
            "volumenApostado": r["volumenApostado"],
            "notas": "",
            "fuente": "lector_demo",
            "tipoDato": "observado",
        }

    def fila_csv(self, giro):
        return [giro.get(c, "") for c in CSV_CAMPOS]

    def guardar_json(self, path):
        Path(path).write_text(
            json.dumps(self.a_json(), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
