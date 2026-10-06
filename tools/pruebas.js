const M = require("../casino/modelo.js");

let fallos = 0;
function comprobar(nombre, condicion, detalle) {
  console.log(`${condicion ? "OK   " : "FALLA"} ${nombre}${detalle ? " — " + detalle : ""}`);
  if (!condicion) fallos++;
}

for (const vol of [1, 2, 3, 4, 5]) {
  const slot = M.construirSlot({ rtp: 94.5, hit: 30, volatilidad: vol });
  const n = 100_000;
  let suma = 0;
  let aciertos = 0;
  for (let i = 0; i < n; i++) {
    const x = M.girar(slot, Math.random);
    suma += x;
    if (x > 0) aciertos++;
  }
  comprobar(
    `volatilidad ${vol}: retorno y acierto`,
    Math.abs(suma / n - 0.945) < 0.05 && Math.abs(aciertos / n - 0.3) < 0.02,
    `retorno ${(100 * suma / n).toFixed(2)}%, acierto ${(100 * aciertos / n).toFixed(1)}%`
  );
}

const casos = [
  [["10", "6"], "10", "PEDIR"],
  [["10", "6"], "6", "PLANTARSE"],
  [["6", "5"], "A", "PEDIR"],
  [["6", "5"], "10", "DOBLAR"],
  [["A", "7"], "2", "PLANTARSE"],
  [["A", "7"], "4", "DOBLAR"],
  [["8", "8"], "A", "SEPARAR"],
  [["A", "K"], "6", "BLACKJACK"],
];
for (const [mano, crupier, esperado] of casos) {
  const r = M.estrategiaBasica(mano, crupier);
  comprobar(`Blackjack ${mano.join("+")} contra ${crupier}`, r.accion === esperado, r.accion);
}

console.log("Suite de pruebas de casino completada.");
process.exitCode = fallos ? 1 : 0;
