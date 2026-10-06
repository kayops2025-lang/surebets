(function (raiz) {
  "use strict";

  const CLAVE = "herramientas-casino.observaciones.v1";
  const CAMPOS = [
    "id", "fecha", "juego", "casino", "estado", "proveedor",
    "rtpOficial", "slotId",
    "apuestaBase", "extraJackpot", "costePorSpin",
    "spinsObservados", "bonosActivados", "spinsDondeSeActivoBonus",
    "spinsInternosBonus", "duracionBonus",
    "premiosBonus", "premiosTotales",
    "saldoInicial", "saldoFinal", "volumenApostado",
    "notas", "fuente", "tipoDato",
  ];
  const NUMERICOS = new Set([
    "rtpOficial", "apuestaBase", "extraJackpot", "costePorSpin",
    "spinsObservados", "bonosActivados", "spinsInternosBonus", "duracionBonus",
    "premiosBonus", "premiosTotales", "saldoInicial", "saldoFinal", "volumenApostado",
  ]);

  function nuevoId() {
    return Math.random().toString(36).slice(2, 10);
  }

  function aNumero(v) {
    if (v === null || v === undefined) return null;
    const t = String(v).replace("%", "").replace(",", ".").trim();
    if (t === "") return null;
    const n = Number(t);
    return Number.isFinite(n) ? n : null;
  }

  function vacia() {
    const o = {};
    for (const c of CAMPOS) o[c] = NUMERICOS.has(c) ? null : "";
    o.tipoDato = "observado";
    return o;
  }

  function desdeSlot(slot) {
    const o = vacia();
    if (!slot) return o;
    o.juego = slot.juego || "";
    o.proveedor = slot.proveedor || "";
    o.casino = slot.casino || "";
    o.estado = slot.estado || "";
    o.rtpOficial = slot.rtp == null || slot.rtp === "" ? null : Number(slot.rtp);
    o.slotId = slot.id || "";
    o.tipoDato = "observado";
    return o;
  }

  function normalizar(datos) {
    const o = vacia();
    const src = datos || {};
    for (const c of CAMPOS) {
      if (!(c in src) || src[c] === undefined) continue;
      o[c] = NUMERICOS.has(c) ? aNumero(src[c]) : src[c] == null ? "" : String(src[c]);
    }
    if (!o.tipoDato) o.tipoDato = "observado";
    if (!o.id) o.id = nuevoId();
    if (o.costePorSpin == null && o.apuestaBase != null && o.extraJackpot != null) {
      o.costePorSpin = Math.round((o.apuestaBase + o.extraJackpot) * 100) / 100;
    }
    return o;
  }

  function escaparCsv(v) {
    const t = v === null || v === undefined ? "" : String(v);
    return /[",\n]/.test(t) ? `"${t.replace(/"/g, '""')}"` : t;
  }

  function leerCsv(texto) {
    const filas = [];
    let fila = [];
    let campo = "";
    let comillas = false;
    for (let i = 0; i < texto.length; i++) {
      const ch = texto[i];
      if (comillas) {
        if (ch === '"' && texto[i + 1] === '"') {
          campo += '"';
          i++;
        } else if (ch === '"') comillas = false;
        else campo += ch;
      } else if (ch === '"') comillas = true;
      else if (ch === ",") {
        fila.push(campo);
        campo = "";
      } else if (ch === "\n" || ch === "\r") {
        if (ch === "\r" && texto[i + 1] === "\n") i++;
        fila.push(campo);
        filas.push(fila);
        fila = [];
        campo = "";
      } else campo += ch;
    }
    if (campo !== "" || fila.length) {
      fila.push(campo);
      filas.push(fila);
    }
    return filas.filter((f) => f.some((c) => c.trim() !== ""));
  }

  function memoria() {
    const m = new Map();
    return {
      getItem(k) {
        return m.has(k) ? m.get(k) : null;
      },
      setItem(k, v) {
        m.set(k, v);
      },
    };
  }

  function crearRegistro(almacen) {
    const store = almacen || (typeof localStorage !== "undefined" ? localStorage : memoria());
    const leer = () => {
      try {
        const raw = store.getItem(CLAVE);
        const arr = raw ? JSON.parse(raw) : [];
        return Array.isArray(arr) ? arr.map(normalizar) : [];
      } catch (e) {
        return [];
      }
    };
    const escribir = (lista) => store.setItem(CLAVE, JSON.stringify(lista));

    return {
      CLAVE,
      CAMPOS,
      listar: leer,
      obtener(id) {
        return leer().find((x) => x.id === id) || null;
      },
      agregar(datos) {
        const o = normalizar(datos);
        const lista = leer();
        lista.push(o);
        escribir(lista);
        return o;
      },
      editar(id, datos) {
        const lista = leer();
        const i = lista.findIndex((x) => x.id === id);
        if (i < 0) return null;
        const o = normalizar({ ...lista[i], ...datos, id });
        lista[i] = o;
        escribir(lista);
        return o;
      },
      borrar(id) {
        escribir(leer().filter((x) => x.id !== id));
        return true;
      },
      buscar(texto) {
        const q = String(texto || "").trim().toLowerCase();
        const lista = leer();
        if (!q) return lista;
        return lista.filter((o) => [o.juego, o.casino, o.estado, o.proveedor, o.notas, o.fuente].join(" ").toLowerCase().includes(q));
      },
      aCsv() {
        const lista = leer();
        return [CAMPOS.join(",")].concat(lista.map((o) => CAMPOS.map((c) => escaparCsv(o[c])).join(","))).join("\n");
      },
      importarCsv(texto) {
        const filas = leerCsv(String(texto || "").replace(/^\ufeff/, ""));
        if (!filas.length) return { nuevos: 0, actualizados: 0 };
        const cabecera = filas[0].map((c) => c.trim());
        if (!cabecera.includes("juego") && !cabecera.includes("id")) {
          throw new Error('El CSV necesita una columna "juego" o "id".');
        }
        const lista = leer();
        const porId = new Map(lista.filter((o) => o.id).map((o) => [o.id, o]));
        let nuevos = 0;
        let actualizados = 0;
        for (const f of filas.slice(1)) {
          const datos = {};
          cabecera.forEach((c, i) => {
            if (!CAMPOS.includes(c)) return;
            datos[c] = (f[i] || "").trim();
          });
          if (!datos.juego && !datos.id) continue;
          const existente = datos.id ? porId.get(datos.id) : null;
          if (existente) {
            const o = normalizar({ ...existente, ...datos, id: existente.id });
            const idx = lista.findIndex((x) => x.id === existente.id);
            lista[idx] = o;
            porId.set(o.id, o);
            actualizados++;
          } else {
            const o = normalizar(datos);
            lista.push(o);
            porId.set(o.id, o);
            nuevos++;
          }
        }
        escribir(lista);
        return { nuevos, actualizados };
      },
    };
  }

  const api = { CLAVE, CAMPOS, NUMERICOS, vacia, desdeSlot, normalizar, crearRegistro, memoria, aNumero };

  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else raiz.Observaciones = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
