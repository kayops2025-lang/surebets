from lector_demo import (
    CSV_CAMPOS,
    Registro,
    Sesion,
    leer_extra,
    leer_numero,
    colocar_panel,
    se_cruzan,
)
import json
import tempfile
from pathlib import Path

fallos = 0


def comprobar(nombre, obtenido, esperado):
    global fallos
    ok = obtenido == esperado
    if not ok:
        fallos += 1
    print(f"{'OK   ' if ok else 'FALLA'} {nombre}: {obtenido}" + ("" if ok else f" (esperaba {esperado})"))


for texto, esperado in [
    ("BALANCE $1,234.50", 1234.5),
    ("S987.35", 987.35),
    ("CREDIT 10,000.00", 10000.0),
    ("$0.15", 0.15),
    ("BALANCE:", None),
]:
    comprobar(f"leer {texto!r}", leer_numero(texto), esperado)

print("Pruebas del lector de giros completadas.")
raise SystemExit(1 if fallos else 0)
