(() => {
  "use strict";
  const cfg = window.ECHO_WORKBOARD;
  if (!cfg) {
    document.body.textContent = "清单数据未加载，请确认 data.js 与 index.html 在同一目录。";
    return;
  }
  const $ = (q) => document.querySelector(q);
  const clone = (v) => JSON.parse(JSON.stringify(v));
  const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const clean = (v, max = 20000) => typeof v === "string" ? v.slice(0, max) : "";
  const own = (o, k) => Object.prototype.hasOwnProperty.call(o, k);
  const choices = (map, selected) => Object.entries(map).map(([key, val]) => `<option value="${esc(key)}"${key === selected ? " selected" : ""}>${esc(val)}</option>`).join("");
  const defaultFilters = () => ({ status: "all", scope: "all", priority: "all", kind: "all", query: "" });
  let saveBlocked = false, storageConflict = false, recoveryRaw = "", toastTimer, editId = null, pendingConfirm = null, dragId = null;
  let filters = defaultFilters();
  const opened = new Set(["UI-01"]);
  const seedMap = new Map(cfg.tasks.map(t => [t.id, t]));
  const createTask = (t) => ({
    ...clone(t), status: "todo", scope: "candidate", note: "", custom: false,
    steps: t.steps.map((text, i) => ({ id: `${t.id}-S${i + 1}`, text, done: false }))
  });
  const defaultState = () => ({ version: 1, category: "ui", tasks: cfg.tasks.map(createTask), decisions: {}, designNote: "", deskOpen: true, modified: null });

  function validateEnvelope(data) {
    if (!data || data.type !== cfg.backupType || data.version !== 1 || !data.state || data.state.version !== 1)
      throw new Error("这不是英灵回响更新工作间的 v1 备份。");
    if (!Array.isArray(data.state.tasks) || data.state.tasks.length > 1000)
      throw new Error("备份中的任务列表无效或超过 1000 项。");
    const ids = new Set();
    for (const t of data.state.tasks) {
      if (!t || typeof t.id !== "string" || !/^[A-Z0-9-]{1,90}$/.test(t.id) || ids.has(t.id))
        throw new Error("备份包含无效或重复的任务编号。");
      ids.add(t.id);
      if (!seedMap.has(t.id) && !(t.custom === true && t.id.startsWith("CUSTOM-")))
        throw new Error("备份含有无法识别的任务，请使用本工作间导出的文件。");
      if (typeof t.title !== "string" || !t.title.trim() || !own(cfg.categories, t.category)
          || !own(cfg.statuses, t.status) || !own(cfg.scopes, t.scope) || !own(cfg.priorities, t.priority)
          || !Array.isArray(t.steps) || t.steps.length > 100
          || t.steps.some(s => !s || typeof s.text !== "string" || typeof s.done !== "boolean")
          || new Set(t.steps.map(s => s.id)).size !== t.steps.length
          || t.steps.some(s => typeof s.id !== "string" || !/^[A-Za-z0-9-]{1,120}$/.test(s.id)))
        throw new Error(`任务 ${t.id} 的字段不完整或格式不正确。`);
    }
    if (data.state.decisions !== undefined && (!data.state.decisions || typeof data.state.decisions !== "object" || Array.isArray(data.state.decisions)))
      throw new Error("设计便笺格式不正确。");
    for (const d of cfg.decisions) {
      const value = data.state.decisions?.[d.id];
      if (value !== undefined && value !== "" && !d.options.includes(value)) throw new Error(`设计选项「${d.label}」无法识别。`);
    }
  }

  function normalizeState(saved) {
    const base = defaultState(), result = [], used = new Set();
    for (const t of saved.tasks) {
      const seed = seedMap.get(t.id);
      const original = seed ? createTask(seed) : {
        id: t.id, refs: [], questions: [], dependencies: [], kind: "derived",
        deliverable: "自定义工作：以任务说明和子步骤为验收依据。", custom: true
      };
      result.push({
        ...original, title: clean(t.title, 120), summary: clean(t.summary, 4000),
        category: t.category, priority: t.priority, status: t.status, scope: t.scope,
        note: clean(t.note), steps: t.steps.map(s => ({ id: s.id, text: clean(s.text, 1000), done: s.done }))
      });
      used.add(t.id);
    }
    for (const t of base.tasks) if (!used.has(t.id)) result.push(t);
    base.tasks = result;
    base.category = saved.category === "all" || own(cfg.categories, saved.category) ? saved.category : "ui";
    for (const d of cfg.decisions) base.decisions[d.id] = clean(saved.decisions?.[d.id], 200);
    base.designNote = clean(saved.designNote);
    base.deskOpen = saved.deskOpen !== false;
    base.modified = clean(saved.modified, 100) || null;
    return base;
  }

  function envelope(value = state) {
    return { type: cfg.backupType, version: 1, exportedAt: new Date().toISOString(), state: value };
  }
  function setSaveStatus(text, error = false) {
    $("#saveState").textContent = text;
    $("#saveState").classList.toggle("error", error);
  }
  function load() {
    try {
      recoveryRaw = localStorage.getItem(cfg.storageKey) || "";
      if (!recoveryRaw) { setSaveStatus("改动将自动保存在此浏览器"); return defaultState(); }
      const data = JSON.parse(recoveryRaw);
      validateEnvelope(data);
      setSaveStatus("已恢复本地进度");
      return normalizeState(data.state);
    } catch (e) {
      if (recoveryRaw) saveBlocked = true;
      setSaveStatus(recoveryRaw ? "旧数据读取失败 · 请先导出保留" : "本地保存不可用 · 请导出备份", true);
      return defaultState();
    }
  }
  let state = load();
  function save() {
    state.modified = new Date().toISOString();
    if (saveBlocked) {
      setSaveStatus(storageConflict ? "另一页面已修改 · 请刷新或导出当前工作" : "旧数据未覆盖 · 请导出原数据后导入备份", true);
      return false;
    }
    try {
      localStorage.setItem(cfg.storageKey, JSON.stringify(envelope()));
      setSaveStatus("已自动保存 · " + new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }));
      return true;
    } catch (e) {
      setSaveStatus("未能保存到浏览器 · 请导出备份", true);
      return false;
    }
  }
  function toast(text) {
    clearTimeout(toastTimer);
    $("#toast").textContent = text; $("#toast").hidden = false;
    toastTimer = setTimeout(() => { $("#toast").hidden = true; }, 4000);
  }
  function confirmAction(title, text, action) {
    $("#confirmTitle").textContent = title; $("#confirmText").textContent = text;
    pendingConfirm = action; $("#confirmDialog").showModal();
  }
  function dateStamp() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }
  function download(name, text, type) {
    const url = URL.createObjectURL(new Blob([text], { type }));
    const a = document.createElement("a"); a.href = url; a.download = name;
    document.body.append(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 3000);
  }
  function exportBackup() {
    if (storageConflict) {
      download(`echo-workboard-this-tab-${dateStamp()}.json`, JSON.stringify(envelope(), null, 2), "application/json;charset=utf-8");
      toast("已导出当前页面的工作；另一页面的保存没有被覆盖。");
    } else if (saveBlocked) {
      download(`echo-workboard-recovery-${dateStamp()}.txt`, recoveryRaw, "text/plain;charset=utf-8");
      toast("已导出无法读取的原数据，未覆盖它。");
    } else {
      download(`echo-workboard-${dateStamp()}.json`, JSON.stringify(envelope(), null, 2), "application/json;charset=utf-8");
      toast("已导出任务、备注与设计便笺。");
    }
  }
  function matchingTasks() {
    const q = filters.query.trim().toLocaleLowerCase();
    return state.tasks.filter(t =>
      (state.category === "all" || t.category === state.category)
      && (filters.status === "all" || t.status === filters.status)
      && (filters.scope === "all" || t.scope === filters.scope)
      && (filters.priority === "all" || t.priority === filters.priority)
      && (filters.kind === "all" || t.kind === filters.kind)
      && (!q || [t.id, t.title, t.summary, t.note, ...t.steps.map(s => s.text),
        ...t.refs.flatMap(ref => { const s = cfg.sources[ref]; return [ref, s.title, s.where, s.request, s.response]; })]
        .join("\n").toLocaleLowerCase().includes(q)));
  }
  function renderStats() {
    const selected = state.tasks.filter(t => t.scope === "selected");
    const done = selected.filter(t => t.status === "done").length;
    const percent = selected.length ? Math.round(done / selected.length * 100) : 0;
    $("#statSelected").textContent = selected.length;
    $("#statDoing").textContent = selected.filter(t => t.status === "doing").length;
    $("#statDone").textContent = done;
    $("#progressText").textContent = `${percent}%`;
    $("#progressBar").style.width = `${percent}%`;
    $("#progressDetail").textContent = selected.length ? `${done} / ${selected.length} 项完成` : "先选定想做的内容";
  }
  function renderTabs() {
    const categories = { ...cfg.categories, all: { label: "全部工作" } };
    $("#categoryTabs").innerHTML = Object.entries(categories).map(([key, c]) =>
      `<button type="button" data-category="${key}" class="${state.category === key ? "active" : ""}" aria-pressed="${state.category === key}">${esc(c.label)}<b>${key === "all" ? state.tasks.length : state.tasks.filter(t => t.category === key).length}</b></button>`).join("");
    const cat = cfg.categories[state.category];
    $("#listTitle").textContent = cat?.label || "全部工作";
    $("#categoryIntro").textContent = cat?.intro || "跨分类查看，每项工作都保留对应愿望与作者回复。";
  }
  function renderDecisions() {
    $("#decisions").innerHTML = cfg.decisions.map((d, i) =>
      `<div class="decision"><label for="decision-${d.id}"><b>0${i + 1}</b>${esc(d.label)}</label><select id="decision-${d.id}" data-decision="${d.id}"><option value="">待决定</option>${d.options.map(o => `<option${state.decisions[d.id] === o ? " selected" : ""}>${esc(o)}</option>`).join("")}</select><p>${esc(d.hint)}</p></div>`).join("");
    $("#designNote").value = state.designNote;
    renderDesk();
  }
  function renderDesk() {
    $("#deskBody").hidden = !state.deskOpen;
    $("#deskToggle").textContent = state.deskOpen ? "收起设计便笺 −" : "展开设计便笺 ＋";
    $("#deskToggle").setAttribute("aria-expanded", String(state.deskOpen));
  }
  function sourceHtml(t) {
    if (!t.refs.length) return `<div class="sources">这是你添加的自定义任务。</div>`;
    return `<details class="sources"><summary>留言依据与当前状态 · ${t.refs.join(" / ")}</summary>${t.refs.map(ref => {
      const s = cfg.sources[ref];
      return `<div class="source-entry"><strong>${ref} · ${esc(s.title)}</strong><p>${esc(s.where)}</p><p>玩家诉求：${esc(s.request)}</p><p>作者回复：${esc(s.response)}</p><p>当前状态：${esc(s.current)}</p><a href="${s.url}" target="_blank" rel="noopener noreferrer">打开来源评论区 ↗</a></div>`;
    }).join("")}</details>`;
  }
  function cardHtml(t, index, total) {
    const isOpen = opened.has(t.id);
    const done = t.steps.filter(s => s.done).length;
    return `<article class="task-card" id="task-${t.id}" data-id="${t.id}" data-status="${t.status}">
      <div class="task-head">
        <button type="button" class="drag-handle" draggable="true" data-action="drag" aria-label="拖动排序 ${esc(t.title)}" title="拖动排序；也可用卡片下方的上下箭头">⠿</button>
        <div><div class="card-meta"><span class="task-id">${t.id}</span><span class="pill ${t.kind}">${esc(cfg.kinds[t.kind])}</span><span class="pill">${esc(cfg.categories[t.category].label)}</span>${t.priority === "high" ? '<span class="priority-high">↑ 优先处理</span>' : ""}</div>
          <button type="button" class="task-title" data-action="expand" aria-expanded="${isOpen}" aria-controls="detail-${t.id}">${esc(t.title)}</button>
          <p class="task-summary">${esc(t.summary)}</p></div>
        <select class="task-status" data-field="status" aria-label="${t.id} 状态">${choices(cfg.statuses, t.status)}</select>
      </div>
      <div class="card-footer">
        <select class="scope-select" data-field="scope" data-scope="${t.scope}" aria-label="${t.id} 本次范围">${choices(cfg.scopes, t.scope)}</select>
        <span class="step-count">${done}/${t.steps.length} 步</span><span class="task-links">${esc(t.refs.join(" · "))}</span>
        <button type="button" data-action="up" aria-label="上移 ${t.id}"${index === 0 ? " disabled" : ""}>↑</button>
        <button type="button" data-action="down" aria-label="下移 ${t.id}"${index === total - 1 ? " disabled" : ""}>↓</button>
        <button type="button" data-action="edit">编辑</button>${t.custom ? '<button type="button" data-action="delete">删除</button>' : ""}
        <button type="button" class="expand-button" data-action="expand" aria-expanded="${isOpen}" aria-controls="detail-${t.id}">${isOpen ? "收起 −" : "展开 ＋"}</button>
      </div>
      <div class="task-detail" id="detail-${t.id}"${isOpen ? "" : " hidden"}>
        <div class="detail-columns">
          <div><h3 class="detail-heading">可以逐项推进</h3><div class="checklist">${t.steps.map(s =>
            `<label class="${s.done ? "checked" : ""}"><input type="checkbox" data-step="${s.id}"${s.done ? " checked" : ""}><span>${esc(s.text)}</span></label>`).join("") || '<p class="muted">尚无子步骤，可以通过编辑添加。</p>'}</div></div>
          <div><h3 class="detail-heading">设计时留意</h3><ul class="question-list">${t.questions.map(q => `<li>${esc(q)}</li>`).join("") || "<li>按你写下的任务说明与交付要求推进。</li>"}</ul><div class="deliverable">交付物：${esc(t.deliverable)}</div></div>
        </div>
        ${t.dependencies.length ? `<div class="dependencies">关联工作：${t.dependencies.map(id => `<button type="button" data-jump="${id}">${id}</button>`).join("")}</div>` : ""}
        <label class="task-note">我的方案、备注与草稿路径<textarea data-field="note" rows="3" maxlength="20000" placeholder="决定了什么？还有什么没想好？把草稿路径也记下来。">${esc(t.note)}</textarea></label>
        ${sourceHtml(t)}
      </div>
    </article>`;
  }
  function renderList(focus = null) {
    const tasks = matchingTasks();
    $("#taskList").innerHTML = tasks.map((t, i) => cardHtml(t, i, tasks.length)).join("");
    $("#visibleCount").textContent = `显示 ${tasks.length} / ${state.tasks.length} 项`;
    $("#emptyState").hidden = tasks.length > 0;
    if (focus) document.querySelector(focus)?.focus({ preventScroll: true });
  }
  function renderAll() { renderTabs(); renderStats(); renderList(); }
  function resetFilters() {
    filters = defaultFilters();
    for (const key of ["status", "scope", "priority", "kind"]) $(`#${key}Filter`).value = "all";
    $("#search").value = "";
  }
  function jumpTo(id) {
    const task = state.tasks.find(t => t.id === id);
    if (!task) return;
    state.category = task.category; resetFilters(); opened.add(id); save(); renderAll();
    document.getElementById(`task-${id}`).scrollIntoView({ behavior: "smooth", block: "start" });
    document.querySelector(`#task-${id} .task-title`).focus({ preventScroll: true });
  }
  function reorder(id, targetId) {
    if (!id || id === targetId) return;
    const from = state.tasks.findIndex(t => t.id === id), to = state.tasks.findIndex(t => t.id === targetId);
    if (from < 0 || to < 0) return;
    const [item] = state.tasks.splice(from, 1);
    state.tasks.splice(to, 0, item);
    save(); renderList(`#task-${id} .drag-handle`);
  }
  function setTaskField(id, field, value) {
    const t = state.tasks.find(x => x.id === id);
    if (!t) return;
    t[field] = value; save(); renderStats();
    if (field !== "note") renderList(`#task-${id} [data-field="${field}"]`);
  }
  function openTaskEditor(task = null) {
    editId = task?.id || null;
    $("#editTitle").textContent = task ? `编辑 ${task.id}` : "新增任务";
    $("#editCategory").innerHTML = choices(Object.fromEntries(Object.entries(cfg.categories).map(([k, v]) => [k, v.label])), task?.category || (state.category === "all" ? "ui" : state.category));
    const form = $("#taskForm");
    form.elements.title.value = task?.title || "";
    form.elements.priority.value = task?.priority || "normal";
    form.elements.summary.value = task?.summary || "";
    form.elements.steps.value = task?.steps.map(s => s.text).join("\n") || "";
    $("#taskDialog").showModal();
    form.elements.title.focus();
  }
  function makeId() {
    return `CUSTOM-${Date.now().toString(36).toUpperCase()}-${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
  }
  function exportBrief() {
    const md = (s) => clean(s).replace(/[\\`*_{}\[\]<>|#]/g, "\\$&").replace(/\r/g, "");
    const lines = ["# 英灵回响 · 更新设计说明", "", `导出日期：${dateStamp()}。反馈核对基线：2026-11-11，当前版本基线：0.2.11。`, "", "## 设计便笺", ""];
    for (const d of cfg.decisions) lines.push(`- ${d.label}：${md(state.decisions[d.id] || "待决定")}`);
    lines.push("", "### 布局想法与备注", "", state.designNote ? md(state.designNote) : "尚未记录。", "", "## 本次选定工作", "");
    const selected = state.tasks.filter(t => t.scope === "selected");
    if (!selected.length) lines.push("尚未选入任务。可在卡片中将“待决定”改为“本次要做”。", "");
    for (const [category, meta] of Object.entries(cfg.categories)) {
      const tasks = selected.filter(t => t.category === category);
      if (!tasks.length) continue;
      lines.push(`### ${meta.label}`, "");
      for (const t of tasks) {
        lines.push(`#### ${t.id} ${md(t.title)}`, "", `状态：${cfg.statuses[t.status]}；优先级：${cfg.priorities[t.priority]}；来源：${cfg.kinds[t.kind]}。`, "", md(t.summary), "");
        for (const step of t.steps) lines.push(`- [${step.done ? "x" : " "}] ${md(step.text)}`);
        lines.push("", `交付物：${md(t.deliverable)}`, "", `备注：${md(t.note || "尚未记录。")}`, "");
        for (const ref of t.refs) {
          const s = cfg.sources[ref];
          lines.push(`- ${ref}：${md(s.title)}；${md(s.where)}`, `  - 诉求：${md(s.request)}`, `  - 回复：${md(s.response)}`, `  - 当前：${md(s.current)}`, `  - 来源：${s.url}`);
        }
        lines.push("");
      }
    }
    lines.push("## 其他候选", "", "以下没有自动进入本次范围：", "");
    for (const t of state.tasks.filter(t => t.scope !== "selected")) lines.push(`- ${t.id} ${md(t.title)} — ${cfg.scopes[t.scope]}，${cfg.statuses[t.status]}`);
    download(`echo-warrior-design-${dateStamp()}.md`, lines.join("\n"), "text/markdown;charset=utf-8");
    toast(`已导出设计便笺与 ${selected.length} 项本次工作。`);
  }

  $("#categoryTabs").addEventListener("click", e => {
    const btn = e.target.closest("[data-category]");
    if (!btn) return;
    state.category = btn.dataset.category; save(); renderAll();
  });
  $("#decisions").addEventListener("change", e => {
    const id = e.target.dataset.decision;
    if (!id) return;
    state.decisions[id] = e.target.value; save();
  });
  $("#designNote").addEventListener("input", e => { state.designNote = e.target.value; save(); });
  $("#deskToggle").addEventListener("click", () => { state.deskOpen = !state.deskOpen; save(); renderDesk(); });
  for (const key of ["status", "scope", "priority", "kind"]) {
    $(`#${key}Filter`).addEventListener("change", e => { filters[key] = e.target.value; renderList(); });
  }
  $("#search").addEventListener("input", e => { filters.query = e.target.value; renderList(); });
  for (const id of ["clearFilters", "emptyClear"]) $(`#${id}`).addEventListener("click", () => { resetFilters(); renderList(); });
  $("#gearFocus").addEventListener("click", () => jumpTo("UI-01"));
  $("#taskList").addEventListener("click", e => {
    const jump = e.target.closest("[data-jump]");
    if (jump) { jumpTo(jump.dataset.jump); return; }
    const btn = e.target.closest("[data-action]"), card = e.target.closest(".task-card");
    if (!btn || !card) return;
    const id = card.dataset.id, t = state.tasks.find(x => x.id === id);
    if (btn.dataset.action === "expand") {
      opened.has(id) ? opened.delete(id) : opened.add(id);
      renderList(`#task-${id} .task-title`);
    } else if (btn.dataset.action === "edit") openTaskEditor(t);
    else if (btn.dataset.action === "delete" && t.custom) confirmAction("删除这个自定义任务？", `将移除「${t.title}」和它的备注。需要保留时请先导出备份。`, () => {
      state.tasks = state.tasks.filter(x => x.id !== id); save(); renderAll();
    });
    else if (btn.dataset.action === "up" || btn.dataset.action === "down") {
      const visible = matchingTasks(), pos = visible.findIndex(x => x.id === id);
      const target = visible[pos + (btn.dataset.action === "up" ? -1 : 1)];
      if (target) reorder(id, target.id);
    }
  });
  $("#taskList").addEventListener("change", e => {
    const card = e.target.closest(".task-card");
    if (!card) return;
    const t = state.tasks.find(x => x.id === card.dataset.id);
    if (e.target.dataset.step) {
      const step = t.steps.find(s => s.id === e.target.dataset.step);
      step.done = e.target.checked;
      e.target.closest("label").classList.toggle("checked", step.done);
      card.querySelector(".step-count").textContent = `${t.steps.filter(s => s.done).length}/${t.steps.length} 步`;
      save();
    } else if (["status", "scope"].includes(e.target.dataset.field)) setTaskField(t.id, e.target.dataset.field, e.target.value);
  });
  $("#taskList").addEventListener("input", e => {
    if (e.target.dataset.field === "note") setTaskField(e.target.closest(".task-card").dataset.id, "note", e.target.value);
  });
  $("#taskList").addEventListener("dragstart", e => {
    if (!e.target.closest(".drag-handle")) { e.preventDefault(); return; }
    const card = e.target.closest(".task-card"); dragId = card.dataset.id;
    e.dataTransfer.setData("text/plain", dragId); e.dataTransfer.effectAllowed = "move"; card.classList.add("dragging");
  });
  $("#taskList").addEventListener("dragover", e => {
    const card = e.target.closest(".task-card");
    if (!card || !dragId || card.dataset.id === dragId) return;
    e.preventDefault(); e.dataTransfer.dropEffect = "move";
    document.querySelectorAll(".drop-target").forEach(el => el.classList.remove("drop-target"));
    card.classList.add("drop-target");
  });
  $("#taskList").addEventListener("drop", e => {
    const card = e.target.closest(".task-card");
    if (!card || !dragId) return;
    e.preventDefault(); const from = dragId; dragId = null; reorder(from, card.dataset.id);
  });
  $("#taskList").addEventListener("dragend", () => {
    dragId = null; document.querySelectorAll(".drop-target,.dragging").forEach(el => el.classList.remove("drop-target", "dragging"));
  });
  $("#addButton").addEventListener("click", () => openTaskEditor());
  $("#taskForm").addEventListener("submit", e => {
    e.preventDefault();
    const form = e.currentTarget;
    const title = form.elements.title.value.trim(), summary = form.elements.summary.value.trim();
    if (!title || !summary) { toast("请填写标题和工作说明。"); return; }
    const texts = form.elements.steps.value.split("\n").map(s => s.trim()).filter(Boolean);
    if (texts.length > 100) { toast("每个任务最多 100 个子步骤。"); return; }
    let t = editId ? state.tasks.find(x => x.id === editId) : null;
    if (!t) {
      if (state.tasks.length >= 1000) { toast("任务数量已达到 1000 项。"); return; }
      t = { id: makeId(), status: "todo", scope: "candidate", note: "", custom: true, refs: [], kind: "derived", questions: [], deliverable: "自定义工作：以任务说明和子步骤为验收依据。", dependencies: [], steps: [] };
      state.tasks.push(t);
    }
    const previous = t.steps.slice(), usedSteps = new Set();
    t.steps = texts.map((text, i) => {
      const old = previous.find(s => s.text === text && !usedSteps.has(s.id));
      if (old) { usedSteps.add(old.id); return old; }
      return { id: `${t.id}-S${Date.now().toString(36)}-${i}`, text, done: false };
    });
    t.title = title; t.summary = summary;
    t.category = form.elements.category.value; t.priority = form.elements.priority.value;
    state.category = t.category; resetFilters(); opened.add(t.id);
    save(); $("#taskDialog").close(); renderAll(); toast("任务已更新。");
  });
  document.querySelectorAll("[data-close]").forEach(btn => btn.addEventListener("click", () => document.getElementById(btn.dataset.close).close()));
  $("#confirmNo").addEventListener("click", () => { pendingConfirm = null; $("#confirmDialog").close(); });
  $("#confirmYes").addEventListener("click", () => {
    const action = pendingConfirm; pendingConfirm = null; $("#confirmDialog").close(); action?.();
  });
  $("#confirmDialog").addEventListener("cancel", () => { pendingConfirm = null; });
  $("#exportButton").addEventListener("click", exportBackup);
  $("#briefButton").addEventListener("click", exportBrief);
  $("#importButton").addEventListener("click", () => $("#importFile").click());
  $("#importFile").addEventListener("change", async e => {
    const file = e.target.files[0]; e.target.value = "";
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { toast("备份超过 5 MB，未导入。"); return; }
    try {
      const data = JSON.parse(await file.text()); validateEnvelope(data);
      const incoming = normalizeState(data.state);
      confirmAction("用这份备份恢复工作间？", `将载入 ${incoming.tasks.length} 项任务，并替换当前状态、排序、备注与设计便笺。\n建议先取消并导出当前进度，留一份副本。`, () => {
        state = incoming; saveBlocked = false; storageConflict = false; resetFilters(); opened.clear();
        const persisted = save(); renderDecisions(); renderAll();
        toast(persisted ? "备份已恢复并保存在此浏览器。" : "备份已载入，本地保存不可用，请保留原备份。");
      });
    } catch (err) { toast(`未导入：${err.message}`); }
  });
  $("#resetButton").addEventListener("click", () => confirmAction("恢复初始清单？", "会清除当前工作间的修改、自定义任务、勾选、排序和备注。\n需要保留时，请先取消并导出备份。", () => {
    state = defaultState(); saveBlocked = false; storageConflict = false; resetFilters(); opened.clear(); opened.add("UI-01");
    const persisted = save(); renderDecisions(); renderAll();
    toast(persisted ? "已恢复初始清单。" : "页面已恢复初始清单；浏览器保存失败。");
  }));
  $("#archiveCount").textContent = cfg.archive.length;
  $("#archiveList").innerHTML = cfg.archive.map(([version, title, text]) => `<div class="archive-item"><strong><span class="pill">${esc(version)}</span>${esc(title)}</strong><p>${esc(text)}</p></div>`).join("");
  $("#archiveButton").addEventListener("click", () => $("#archiveDialog").showModal());
  document.addEventListener("keydown", e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k" && !document.querySelector("dialog[open]")) {
      e.preventDefault(); $("#search").focus(); $("#search").select();
    }
  });
  window.addEventListener("storage", e => {
    if (e.key === cfg.storageKey && e.newValue && !saveBlocked) {
      try {
        const data = JSON.parse(e.newValue); validateEnvelope(data);
        // Preserve unsaved typing/editor content in this tab until the user chooses.
        saveBlocked = true; storageConflict = true; recoveryRaw = e.newValue;
        setSaveStatus("另一页面已修改 · 请刷新读取，或导出当前工作", true);
      } catch { setSaveStatus("其他页面写入了无法识别的数据 · 请保留备份", true); saveBlocked = true; recoveryRaw = e.newValue; }
    }
  });
  renderDecisions(); renderAll();
})();
