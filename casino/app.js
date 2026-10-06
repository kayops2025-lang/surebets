(function () {
  "use strict";

  const M = window.Modelo;
  const $ = (id) => document.getElementById(id);
  const dinero = new Intl.NumberFormat("es-US", { style: "currency", currency: "USD" });
  const numero = (d) => new Intl.NumberFormat("es-US", { maximumFractionDigits: d, minimumFractionDigits: d });
  const pct = (x, d = 1) => numero(d).format(100 * x) + "%";

  // ---------- Pestañas ----------
  function mostrarSeccion(id) {
    document.querySelectorAll(".pestana").forEach((b) => b.classList.toggle("activa", b.dataset.seccion === id));
    document.querySelectorAll(".seccion").forEach((s) => s.classList.toggle("activa", s.id === id));
  }
  document.querySelectorAll(".pestana").forEach((b) => b.addEventListener("click", () => mostrarSeccion(b.dataset.seccion)));

  // ---------- Base de slots ----------
  const CLAVE = "herramientas-casino.slots.v1";
  const CAMPOS = [
    "juego", "proveedor", "casino", "estado", "rtp", "volatilidad", "hit", "maxWin", "revisado", "notas",
    "frecuenciaBono", "duracionBono", "spinsBono", "rtpBase", "rtpBono", "fuenteDatos", "fechaDatos", "tipoDatoBono", "version",
  ];
  const NUMERICOS = new Set(["rtp", "volatilidad", "hit", "maxWin", "frecuenciaBono", "duracionBono", "spinsBono", "rtpBase", "rtpBono"]);
  const EXTRAS_SLOT = ["frecuenciaBono", "duracionBono", "spinsBono", "rtpBase", "rtpBono", "fuenteDatos", "fechaDatos", "tipoDatoBono", "version"];

  const nuevoId = () => Math.random().toString(36).slice(2, 10);
  const conId = (s) => ({ ...s, id: s.id || nuevoId() });

  let slots = cargar();
  let orden = { campo: "juego", asc: true };
  let editando = null;
  let extrasSlot = {};

  function extrasDesdeSlot(s) {
    const extra = {};
    for (const c of EXTRAS_SLOT) extra[c] = s && s[c] != null && s[c] !== "" ? s[c] : null;
    return extra;
  }

  function cargar() {
    try {
      const guardado = JSON.parse(localStorage.getItem(CLAVE));
      if (Array.isArray(guardado)) return guardado.map(conId);
    } catch (e) {
      /* lista dañada: se vuelve a la inicial */
    }
    return window.SLOTS_INICIALES.map(conId);
  }

  function guardar() {
    localStorage.setItem(CLAVE, JSON.stringify(slots));
    pintarTabla();
    pintarSelectorSlots();
    if (typeof pintarSelectorObsSlots === "function") pintarSelectorObsSlots();
  }

  function aNumero(v) {
    if (v === null || v === undefined) return null;
    const t = String(v).replace("%", "").replace(",", ".").trim();
    if (t === "") return null;
    const n = Number(t);
    return Number.isFinite(n) ? n : null;
  }

  function celda(valor, formato) {
    const td = document.createElement("td");
    if (valor === null || valor === undefined || valor === "") {
      td.textContent = "sin dato";
      td.className = "sin-dato";
    } else {
      td.textContent = formato ? formato(valor) : valor;
    }
    return td;
  }

  function pintarTabla() {
    const filtro = $("t-buscar").value.trim().toLowerCase();
    const soloRtp = $("t-solo-rtp").checked;
    const lista = slots
      .filter((s) => !soloRtp || s.rtp !== null)
      .filter((s) => !filtro || [s.juego, s.proveedor, s.casino, s.estado].join(" ").toLowerCase().includes(filtro))
      .sort((a, b) => {
        const va = a[orden.campo];
        const vb = b[orden.campo];
        const vacioA = va === null || va === "" || va === undefined;
        const vacioB = vb === null || vb === "" || vb === undefined;
        if (vacioA !== vacioB) return vacioA ? 1 : -1;
        const c = NUMERICOS.has(orden.campo) ? va - vb : String(va).localeCompare(String(vb), "es");
        return orden.asc ? c : -c;
      });

    const cuerpo = $("t-cuerpo");
    cuerpo.replaceChildren();
    for (const s of lista) {
      const tr = document.createElement("tr");
      tr.append(
        celda(s.juego),
        celda(s.proveedor),
        celda(s.casino),
        celda(s.estado),
        celda(s.rtp, (v) => numero(2).format(v) + "%"),
        celda(s.volatilidad, (v) => `${v} · ${M.NOMBRES_VOLATILIDAD[v]}`),
        celda(s.hit, (v) => numero(1).format(v) + "%"),
        celda(s.maxWin, (v) => numero(0).format(v) + "×"),
        celda(s.revisado),
        celda(s.notas)
      );
      const acciones = document.createElement("td");
      acciones.className = "acciones";
      const boton = (texto, fn, deshabilitado) => {
        const b = document.createElement("button");
        b.textContent = texto;
        b.disabled = !!deshabilitado;
        b.addEventListener("click", fn);
        acciones.append(b, " ");
      };
      boton("Simular", () => usarSlotEnBono(s.id), s.rtp === null);
      boton("Editar", () => abrirEditor(s));
      boton("Borrar", () => {
        if (confirm(`¿Borrar ${s.juego}?`)) {
          slots = slots.filter((x) => x.id !== s.id);
          guardar();
        }
      });
      tr.append(acciones);
      cuerpo.append(tr);
    }
  }

  document.querySelectorAll("th[data-orden]").forEach((th) =>
    th.addEventListener("click", () => {
      const campo = th.dataset.orden;
      orden = { campo, asc: orden.campo === campo ? !orden.asc : !NUMERICOS.has(campo) };
      pintarTabla();
    })
  );
  $("t-buscar").addEventListener("input", pintarTabla);
  $("t-solo-rtp").addEventListener("change", pintarTabla);

  function abrirEditor(s) {
    editando = s ? s.id : null;
    $("editor-titulo").textContent = s ? "Editar juego" : "Agregar juego";
    const form = $("editor-form");
    const hoy = new Date().toISOString().slice(0, 10);
    for (const c of CAMPOS) {
      const v = s ? s[c] : c === "revisado" ? hoy : "";
      form.elements[c].value = v === null || v === undefined ? "" : v;
    }
    $("editor").showModal();
  }

  $("t-agregar").addEventListener("click", () => abrirEditor(null));
  $("editor").addEventListener("close", () => {
    if ($("editor").returnValue !== "guardar") return;
    const form = $("editor-form");
    const datos = {};
    for (const c of CAMPOS) {
      const v = form.elements[c].value.trim();
      datos[c] = NUMERICOS.has(c) ? aNumero(v) : v;
    }
    if (editando) slots = slots.map((s) => (s.id === editando ? { ...s, ...datos } : s));
    else slots.push(conId(datos));
    guardar();
  });

  function aCsv() {
    const escapar = (v) => {
      const t = v === null || v === undefined ? "" : String(v);
      return /[",\n]/.test(t) ? `"${t.replace(/"/g, '""')}"` : t;
    };
    const filas = [CAMPOS.join(",")].concat(slots.map((s) => CAMPOS.map((c) => escapar(s[c])).join(",")));
    return filas.join("\n");
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

  $("t-exportar").addEventListener("click", () => {
    const blob = new Blob(["\ufeff" + aCsv()], { type: "text/csv;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "slots.csv";
    a.click();
    URL.revokeObjectURL(a.href);
  });

  $("t-importar").addEventListener("change", async (e) => {
    const archivo = e.target.files[0];
    e.target.value = "";
    if (!archivo) return;
    const filas = leerCsv((await archivo.text()).replace(/^\ufeff/, ""));
    if (!filas.length) return;
    const cabecera = filas[0].map((c) => c.trim());
    if (!cabecera.includes("juego")) {
      alert("El CSV necesita una columna llamada \"juego\". Usa Exportar CSV para ver el formato.");
      return;
    }
    const clave = (s) => [s.juego, s.casino, s.estado].map((v) => String(v || "").toLowerCase().trim()).join("|");
    const porClave = new Map(slots.map((s) => [clave(s), s]));
    let nuevos = 0;
    let actualizados = 0;
    for (const f of filas.slice(1)) {
      const datos = {};
      cabecera.forEach((c, i) => {
        if (!CAMPOS.includes(c)) return;
        const v = (f[i] || "").trim();
        datos[c] = NUMERICOS.has(c) ? aNumero(v) : v;
      });
      if (!datos.juego) continue;
      const existente = porClave.get(clave(datos));
      if (existente) {
        Object.assign(existente, datos);
        actualizados++;
      } else {
        const s = conId({ proveedor: "", casino: "", estado: "", rtp: null, volatilidad: null, hit: null, maxWin: null, revisado: "", notas: "", frecuenciaBono: null, duracionBono: null, spinsBono: null, rtpBase: null, rtpBono: null, fuenteDatos: "", fechaDatos: "", tipoDatoBono: "", version: "", ...datos });
        slots.push(s);
        porClave.set(clave(s), s);
        nuevos++;
      }
    }
    guardar();
    alert(`Importados: ${nuevos} nuevos, ${actualizados} actualizados.`);
  });

  $("t-restaurar").addEventListener("click", () => {
    if (!confirm("Esto reemplaza tu base por la lista inicial. ¿Seguir?")) return;
    slots = window.SLOTS_INICIALES.map(conId);
    guardar();
  });

  // ---------- Bono y simulador ----------
  const ENTRADAS = [
    "b-bono", "b-deposito", "b-requisito", "b-base", "b-contribucion", "b-retirable", "b-tope",
    "b-rtp", "b-volatilidad", "b-hit", "b-maxwin", "b-apuesta", "b-extra", "b-retorno-extra", "b-extra-cuenta", "b-segundos",
  ];

  function duracion(segundos) {
    const min = Math.round(segundos / 60);
    if (min < 60) return `${min} min`;
    return `${Math.floor(min / 60)} h ${min % 60} min`;
  }

  const porHora = (ganancia, segundos) => (segundos > 0 ? dinero.format((ganancia * 3600) / segundos) + " por hora" : "—");

  function leerParametros() {
    const n = (id) => {
      const v = aNumero($(id).value);
      return v === null ? 0 : v;
    };
    return {
      bono: n("b-bono"),
      deposito: n("b-deposito"),
      requisito: n("b-requisito"),
      baseRequisito: $("b-base").value,
      contribucion: n("b-contribucion"),
      retirable: $("b-retirable").checked,
      topeRetiro: n("b-tope"),
      rtp: n("b-rtp"),
      volatilidad: Number($("b-volatilidad").value),
      hit: n("b-hit"),
      maxWin: n("b-maxwin"),
      apuesta: n("b-apuesta"),
      extra: n("b-extra"),
      retornoExtra: n("b-retorno-extra"),
      extraCuenta: $("b-extra-cuenta").checked,
      segundos: n("b-segundos"),
      frecuenciaBono: extrasSlot.frecuenciaBono,
      duracionBono: extrasSlot.duracionBono,
      spinsBono: extrasSlot.spinsBono,
      rtpBase: extrasSlot.rtpBase,
      rtpBono: extrasSlot.rtpBono,
      fuenteDatos: extrasSlot.fuenteDatos || "",
      fechaDatos: extrasSlot.fechaDatos || "",
      tipoDatoBono: extrasSlot.tipoDatoBono || "",
      version: extrasSlot.version || "",
    };
  }

  function errorParametros(p) {
    if (p.apuesta <= 0) return "La apuesta por giro tiene que ser mayor que cero.";
    if (p.rtp <= 0) return "Falta el retorno (RTP) del juego.";
    if (p.contribucion <= 0) return "Con contribución 0% el requisito nunca se cumple.";
    if (p.hit <= 0 || p.hit >= 100) return "La frecuencia de acierto tiene que estar entre 0% y 100%.";
    if (p.bono + p.deposito < p.apuesta + p.extra) return "El saldo inicial no alcanza para un giro.";
    return null;
  }

  function pintarPares(dl, pares) {
    dl.replaceChildren();
    for (const [titulo, valor, clase] of pares) {
      const dt = document.createElement("dt");
      dt.textContent = titulo;
      const dd = document.createElement("dd");
      dd.textContent = valor;
      if (clase) dd.className = clase;
      dl.append(dt, dd);
    }
  }

  const claseSigno = (x, extra = "") => `${extra} ${x >= 0 ? "positivo" : "negativo"}`.trim();

  function actualizarAnalitico() {
    const p = leerParametros();
    const dl = $("r-analitico");
    const error = errorParametros(p);
    if (error) {
      pintarPares(dl, [["Revisa los datos", error, "negativo"]]);
      return;
    }
    const a = M.evAnalitico(p);
    const pares = [
      ["Requisito", dinero.format(a.requisito)],
      ["Giros necesarios", numero(0).format(Math.ceil(a.giros))],
      ["Total apostado", dinero.format(a.apostado)],
      ["Costo del juego", dinero.format(-a.costoJuego), "negativo"],
    ];
    if (p.extra > 0) pares.push(["Costo de los jackpots", dinero.format(-a.costoExtra), "negativo"]);
    pares.push(["Valor esperado del bono", dinero.format(a.ev), claseSigno(a.ev, "grande")]);
    if (p.bono > 0) pares.push(["Te quedas con", pct(a.ev / p.bono) + " del bono"]);
    const segundos = a.tiempoTotal;
    pares.push(["Tiempo estimado", duracion(segundos)], ["Ganancia por hora", porHora(a.ev, segundos), claseSigno(a.ev)]);
    if (a.pBonoEstimada > 0) {
      const etiqueta = a.tipoDatoBono === "real" ? "real/oficial" : a.tipoDatoBono === "observado" ? "observado" : "estimado del modelo";
      pares.push(["Bonos (" + etiqueta + ")", numero(1).format(a.giros * a.pBonoEstimada)]);
    }
    pintarPares(dl, pares);
  }

  ENTRADAS.forEach((id) => {
    $(id).addEventListener("input", actualizarAnalitico);
    $(id).addEventListener("change", actualizarAnalitico);
  });
  ["b-rtp", "b-volatilidad", "b-hit", "b-maxwin"].forEach((id) =>
    $(id).addEventListener("input", () => {
      $("b-slot").value = "";
      extrasSlot = extrasDesdeSlot(null);
      $("b-aviso-slot").textContent = "";
    })
  );

  function pintarSelectorSlots() {
    const sel = $("b-slot");
    const actual = sel.value;
    sel.replaceChildren(new Option("Manual", ""));
    slots
      .filter((s) => s.rtp !== null)
      .sort((a, b) => a.juego.localeCompare(b.juego, "es"))
      .forEach((s) => {
        const lugar = [s.casino, s.estado].filter(Boolean).join(" ");
        sel.append(new Option(`${s.juego} — ${numero(2).format(s.rtp)}%${lugar ? " (" + lugar + ")" : ""}`, s.id));
      });
    if ([...sel.options].some((o) => o.value === actual)) sel.value = actual;
  }

  function aplicarSlot(id) {
    const s = slots.find((x) => x.id === id);
    if (!s) return;
    $("b-rtp").value = s.rtp;
    $("b-volatilidad").value = s.volatilidad || M.VOLATILIDAD_POR_DEFECTO;
    $("b-hit").value = s.hit || M.HIT_POR_DEFECTO;
    $("b-maxwin").value = s.maxWin || 0;
    extrasSlot = extrasDesdeSlot(s);
    const faltan = [];
    if (!s.volatilidad) faltan.push("volatilidad (se usa 3 · Media)");
    if (!s.hit) faltan.push(`frecuencia de acierto (se usa ${M.HIT_POR_DEFECTO}% = giros con premio, no bonos)`);
    $("b-aviso-slot").textContent = faltan.length ? `Sin dato de ${faltan.join(" ni de ")}. Complétalo en la base para afinar la simulación.` : "";
    actualizarAnalitico();
  }

  $("b-slot").addEventListener("change", (e) => {
    if (e.target.value) aplicarSlot(e.target.value);
    else {
      extrasSlot = extrasDesdeSlot(null);
      $("b-aviso-slot").textContent = "";
    }
  });

  function usarSlotEnBono(id) {
    $("b-slot").value = id;
    aplicarSlot(id);
    mostrarSeccion("bono");
  }

  const LIMITE_GIROS = 2e8;
  let simulando = false;

  $("s-correr").addEventListener("click", () => {
    if (simulando) return;
    const p = leerParametros();
    const error = errorParametros(p);
    if (error) {
      pintarPares($("r-simulacion"), [["Revisa los datos", error, "negativo"]]);
      return;
    }
    const giros = Math.max(1, M.evAnalitico(p).giros);
    let n = Number($("s-n").value);
    const recorte = n * giros > LIMITE_GIROS;
    if (recorte) n = Math.max(100, Math.floor(LIMITE_GIROS / giros));

    const sim = M.nuevaSimulacion(p);
    const lote = Math.max(1, Math.floor(300000 / giros));
    const barra = $("s-progreso");
    barra.hidden = false;
    barra.value = 0;
    simulando = true;
    $("s-correr").disabled = true;

    const paso = () => {
      sim.correr(Math.min(lote, n - sim.hechas()));
      barra.value = sim.hechas() / n;
      if (sim.hechas() < n) {
        setTimeout(paso, 0);
        return;
      }
      simulando = false;
      $("s-correr").disabled = false;
      barra.hidden = true;
      mostrarSimulacion(sim.resumen(), sim.slot, p, recorte);
    };
    setTimeout(paso, 0);
  });

  function mostrarSimulacion(r, slot, p, recorte) {
    const margen = 1.96 * r.errorMedia;
    const segundos = r.tiempoPromedio;
    const pares = [
      ["Valor esperado", `${dinero.format(r.media)} ± ${dinero.format(margen)}`, claseSigno(r.media, "grande")],
      ["Saldo final promedio", dinero.format(r.saldoFinalPromedio), claseSigno(r.saldoFinalPromedio - p.deposito)],
      ["Terminas con ganancia", pct(r.positivas)],
      ["Completas el requisito", pct(r.completado)],
      ["Te quedas sin saldo antes de cumplir", pct(r.quiebra)],
      ["Resultado típico (mediana)", dinero.format(r.p50), claseSigno(r.p50)],
      ["Percentil 25 / 75", `${dinero.format(r.p25)} / ${dinero.format(r.p75)}`],
      ["Peor / mejor", `${dinero.format(r.min)} / ${dinero.format(r.max)}`],
      ["El 10% peor termina en", `${dinero.format(r.p10)} o menos`],
      ["El 10% mejor termina en", `${dinero.format(r.p90)} o más`],
      ["Giros normales promedio", numero(1).format(r.girosPromedio)],
      ["Bonos promedio por sesión", numero(2).format(r.bonusesPromedio) + (r.tipoDatoBono === "estimado" ? " (estimado)" : r.tipoDatoBono === "observado" ? " (observado)" : " (dato real)")],
      ["Giros internos de bono (no pagados)", numero(1).format(r.spinsBonusPromedio)],
      ["Apostado promedio", dinero.format(r.apostadoPromedio)],
      ["Apuesta base / extra jackpot", `${dinero.format(r.apostadoBasePromedio)} / ${dinero.format(r.apostadoExtraPromedio)}`],
      ["Volumen que cuenta al requisito", dinero.format(r.volumenPromedio)],
      ["Coste esperado del rollover", dinero.format(r.costeRollover), claseSigno(-r.costeRollover)],
      ["Tiempo de giros / de bonos", `${duracion(r.tiempoSpinsPromedio)} / ${duracion(r.tiempoBonusesPromedio)}`],
      ["Tiempo promedio", duracion(segundos)],
      ["Ganancia por hora", porHora(r.media, segundos), claseSigno(r.media)],
      ["Simulaciones", numero(0).format(r.n) + (recorte ? " (recortadas: demasiados giros por bono)" : "")],
      ["Desviación por giro del modelo", numero(1).format(slot.sigma) + "× la apuesta"],
    ];
    pintarPares($("r-simulacion"), pares);
    dibujarHistograma(r);
  }

  function dibujarHistograma(r) {
    const lienzo = $("s-histograma");
    const ctx = lienzo.getContext("2d");
    const ancho = lienzo.width;
    const alto = lienzo.height;
    ctx.clearRect(0, 0, ancho, alto);

    const g = r.ganancias;
    const n = g.length;
    let min = g[Math.floor(0.005 * (n - 1))];
    let max = g[Math.floor(0.995 * (n - 1))];
    if (max - min < 1e-6) {
      min -= 1;
      max += 1;
    }
    const cajas = 40;
    const conteo = new Array(cajas).fill(0);
    for (const x of g) {
      if (x < min || x > max) continue;
      conteo[Math.min(cajas - 1, Math.floor(((x - min) / (max - min)) * cajas))]++;
    }
    const tope = Math.max(...conteo);
    const izq = 8;
    const abajo = 22;
    const util = ancho - 2 * izq;
    const anchoCaja = util / cajas;
    const xDe = (v) => izq + ((v - min) / (max - min)) * util;

    for (let i = 0; i < cajas; i++) {
      const h = ((alto - abajo - 8) * conteo[i]) / tope;
      const centro = min + ((i + 0.5) / cajas) * (max - min);
      ctx.fillStyle = centro >= 0 ? "#3fb27f" : "#e05d5d";
      ctx.fillRect(izq + i * anchoCaja + 1, alto - abajo - h, anchoCaja - 2, h);
    }

    ctx.font = "12px system-ui, sans-serif";
    ctx.fillStyle = "#9aa4b2";
    ctx.textBaseline = "top";
    ctx.textAlign = "left";
    ctx.fillText(dinero.format(min), izq, alto - abajo + 6);
    ctx.textAlign = "right";
    ctx.fillText(dinero.format(max), ancho - izq, alto - abajo + 6);

    const linea = (v, color, texto) => {
      if (v < min || v > max) return;
      const x = xDe(v);
      ctx.strokeStyle = color;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.moveTo(x, 4);
      ctx.lineTo(x, alto - abajo);
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.fillStyle = color;
      ctx.textAlign = "center";
      ctx.fillText(texto, x, alto - abajo + 6);
    };
    linea(0, "#e6e9ef", "$0");
    linea(r.media, "#d4ac0d", "promedio");
  }

  // ---------- Blackjack ----------
  const CARTAS = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "10"];
  const ETIQUETA_CARTA = (c) => (c === "10" ? "10/J/Q/K" : c);
  let crupier = null;
  let mano = [];

  function botonesCartas(contenedor, alElegir) {
    for (const c of CARTAS) {
      const b = document.createElement("button");
      b.textContent = ETIQUETA_CARTA(c);
      b.dataset.carta = c;
      b.addEventListener("click", () => alElegir(c));
      contenedor.append(b);
    }
  }

  botonesCartas($("bj-crupier"), (c) => {
    crupier = c;
    pintarBlackjack();
  });
  botonesCartas($("bj-jugador"), (c) => {
    mano.push(c);
    pintarBlackjack();
  });
  $("bj-borrar-ultima").addEventListener("click", () => {
    mano.pop();
    pintarBlackjack();
  });
  $("bj-limpiar").addEventListener("click", () => {
    mano = [];
    crupier = null;
    pintarBlackjack();
  });

  const TEXTO_CODIGO = { H: "H", S: "S", D: "D", X: "Ds", P: "P" };

  function tablaBj(titulo, filas, actual) {
    const t = document.createElement("table");
    t.className = "tabla-bj";
    const cab = document.createElement("tr");
    const th0 = document.createElement("th");
    th0.textContent = titulo;
    th0.style.width = "56px";
    cab.append(th0);
    for (const c of M.CRUPIER) {
      const th = document.createElement("th");
      th.textContent = c;
      cab.append(th);
    }
    t.append(cab);
    for (const [etiqueta, clave, codigos] of filas) {
      const tr = document.createElement("tr");
      const th = document.createElement("th");
      th.textContent = etiqueta;
      tr.append(th);
      [...codigos].forEach((cod, i) => {
        const td = document.createElement("td");
        td.textContent = TEXTO_CODIGO[cod];
        td.className = "c-" + cod;
        if (actual && actual.clave === clave && actual.col === i) td.classList.add("actual");
        tr.append(td);
      });
      t.append(tr);
    }
    return t;
  }

  function pintarTablaEstrategia(r) {
    const col = crupier ? M.CRUPIER.indexOf(crupier) : -1;
    const marca = (tipo) => (r && r.tipo === tipo && col >= 0 ? { clave: tipo === "dura" ? Math.min(17, Math.max(8, r.clave)) : r.clave, col } : null);

    const duras = Object.keys(M.DURAS).map(Number).map((t) => [t === 8 ? "8 o menos" : t === 17 ? "17 o más" : String(t), t, M.DURAS[t]]);
    const blandas = [13, 14, 15, 16, 17, 18, 19, 20].map((t) => [`A,${t - 11}`, t, M.BLANDAS[t]]);
    const pares = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "A"].map((c) => [`${c},${c}`, c, c === "5" ? M.DURAS[10] : M.PARES[c]]);

    $("bj-tabla").replaceChildren(
      tablaBj("Dura", duras, marca("dura")),
      tablaBj("Blanda", blandas, marca("blanda")),
      tablaBj("Par", pares, marca("par"))
    );
  }

  function pintarBlackjack() {
    document.querySelectorAll("#bj-crupier button").forEach((b) => b.classList.toggle("elegida", b.dataset.carta === crupier));
    const caja = $("bj-decision");
    caja.className = "decision";
    if (!mano.length) {
      $("bj-mano").textContent = "Sin cartas";
    } else {
      let total = mano[0] === "A" ? "11" : mano[0];
      if (mano.length > 1) {
        const r = M.estrategiaBasica(mano, crupier || "2");
        total = `${r.blanda ? "blanda " : ""}${r.total}`;
      }
      $("bj-mano").textContent = `${mano.map(ETIQUETA_CARTA).join(" + ")} = ${total}`;
    }

    if (!crupier || mano.length < 2) {
      caja.textContent = !crupier ? "Elige la carta del crupier" : "Agrega tus dos cartas";
      pintarTablaEstrategia(null);
      return;
    }
    const r = M.estrategiaBasica(mano, crupier);
    caja.textContent = r.accion;
    if (r.codigo) {
      const codigoEfectivo = { PEDIR: "H", PLANTARSE: "S", DOBLAR: r.codigo === "X" ? "X" : "D", SEPARAR: "P" }[r.accion];
      caja.classList.add("c-" + codigoEfectivo);
    }
    pintarTablaEstrategia(r);
  }

  // ---------- Observaciones (no escribe en la ficha del slot ni en el simulador) ----------
  const Obs = window.Observaciones.crearRegistro();
  let editandoObs = null;

  function pintarSelectorObsSlots() {
    const sel = $("o-slot");
    if (!sel) return;
    const actual = sel.value;
    sel.replaceChildren(new Option("Manual / sin slot", ""));
    slots
      .slice()
      .sort((a, b) => a.juego.localeCompare(b.juego, "es"))
      .forEach((s) => {
        const lugar = [s.casino, s.estado].filter(Boolean).join(" ");
        const rtp = s.rtp == null ? "sin RTP" : numero(2).format(s.rtp) + "%";
        sel.append(new Option(`${s.juego} — ${rtp}${lugar ? " (" + lugar + ")" : ""}`, s.id));
      });
    if ([...sel.options].some((o) => o.value === actual)) sel.value = actual;
  }

  function rellenarFormObs(o) {
    const form = $("editor-obs-form");
    for (const c of window.Observaciones.CAMPOS) {
      if (!form.elements[c]) continue;
      const v = o ? o[c] : c === "fecha" ? new Date().toISOString().slice(0, 10) : c === "tipoDato" ? "observado" : "";
      form.elements[c].value = v === null || v === undefined ? "" : v;
    }
  }

  function pintarObs() {
    const lista = Obs.buscar($("o-buscar").value);
    const cuerpo = $("o-cuerpo");
    cuerpo.replaceChildren();
    for (const o of lista) {
      const tr = document.createElement("tr");
      tr.append(
        celda(o.fecha),
        celda(o.juego),
        celda(o.casino),
        celda(o.estado),
        celda(o.apuestaBase, (v) => dinero.format(v)),
        celda(o.spinsObservados, (v) => numero(0).format(v)),
        celda(o.bonosActivados, (v) => numero(0).format(v)),
        celda(o.tipoDato),
        celda(o.rtpOficial, (v) => numero(2).format(v) + "%")
      );
      const acciones = document.createElement("td");
      acciones.className = "acciones";
      const ed = document.createElement("button");
      ed.textContent = "Editar";
      ed.addEventListener("click", () => abrirEditorObs(o));
      const bor = document.createElement("button");
      bor.textContent = "Borrar";
      bor.addEventListener("click", () => {
        if (confirm("¿Borrar esta observación? No cambia la ficha del slot.")) {
          Obs.borrar(o.id);
          pintarObs();
        }
      });
      acciones.append(ed, " ", bor);
      tr.append(acciones);
      cuerpo.append(tr);
    }
  }

  function abrirEditorObs(o) {
    editandoObs = o ? o.id : null;
    $("editor-obs-titulo").textContent = o ? "Editar observación" : "Agregar observación";
    rellenarFormObs(o || window.Observaciones.vacia());
    if (!o) {
      $("editor-obs-form").elements.fecha.value = new Date().toISOString().slice(0, 10);
      $("editor-obs-form").elements.tipoDato.value = "observado";
    }
    $("o-slot").value = o && o.slotId ? o.slotId : "";
    $("editor-obs").showModal();
  }

  $("o-slot").addEventListener("change", () => {
    const s = slots.find((x) => x.id === $("o-slot").value);
    if (!s) return;
    const copia = window.Observaciones.desdeSlot(s);
    const form = $("editor-obs-form");
    form.elements.juego.value = copia.juego;
    form.elements.proveedor.value = copia.proveedor;
    form.elements.casino.value = copia.casino;
    form.elements.estado.value = copia.estado;
    form.elements.rtpOficial.value = copia.rtpOficial == null ? "" : copia.rtpOficial;
    form.elements.slotId.value = copia.slotId;
    form.elements.tipoDato.value = "observado";
  });

  $("o-agregar").addEventListener("click", () => abrirEditorObs(null));
  $("o-buscar").addEventListener("input", pintarObs);
  $("editor-obs").addEventListener("close", () => {
    if ($("editor-obs").returnValue !== "guardar") return;
    const form = $("editor-obs-form");
    const datos = {};
    for (const c of window.Observaciones.CAMPOS) {
      if (!form.elements[c]) continue;
      const v = form.elements[c].value.trim();
      datos[c] = window.Observaciones.NUMERICOS.has(c) ? window.Observaciones.aNumero(v) : v;
    }
    if (!datos.tipoDato) datos.tipoDato = "observado";
    if (editandoObs) Obs.editar(editandoObs, datos);
    else Obs.agregar(datos);
    pintarObs();
  });

  $("o-exportar").addEventListener("click", () => {
    const blob = new Blob(["\ufeff" + Obs.aCsv()], { type: "text/csv;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "observaciones.csv";
    a.click();
    URL.revokeObjectURL(a.href);
  });

  $("o-importar").addEventListener("change", async (e) => {
    const archivo = e.target.files[0];
    e.target.value = "";
    if (!archivo) return;
    try {
      const r = Obs.importarCsv(await archivo.text());
      pintarObs();
      alert(`Observaciones: ${r.nuevos} nuevas, ${r.actualizados} actualizadas. El RTP de los slots no cambia.`);
    } catch (err) {
      alert(err.message);
    }
  });

  // ---------- Inicio ----------
  pintarTabla();
  pintarSelectorSlots();
  const wolf = slots.find((s) => s.juego.startsWith("Wolf It Up"));
  if (wolf && wolf.rtp !== null) {
    $("b-slot").value = wolf.id;
    aplicarSlot(wolf.id);
  } else {
    actualizarAnalitico();
  }
  pintarBlackjack();
  pintarSelectorObsSlots();
  pintarObs();
})();
