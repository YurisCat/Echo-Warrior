"use strict";

(() => {
  const data = window.MCMOD_PREVIEW_DATA;
  if (!data || !Array.isArray(data.items)) {
    document.getElementById("catalog").textContent = "稿件未能加载，请保留 content.js 与入口页面在同一目录。";
    return;
  }
  const $ = (id) => document.getElementById(id);
  const byKey = new Map(data.items.map((item) => [item.key, item]));
  const byUrl = new Map(data.items.map((item) => [item.url, item]));
  let markupMode = false;
  let searchTimer;
  let activeItem = null;

  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
  }

  function route(values = {}) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(values)) if (value) params.set(key, value);
    return "#" + params.toString();
  }

  function link(text, href, className) {
    const node = element("a", text, className);
    node.href = href;
    return node;
  }

  function state() {
    const params = new URLSearchParams(location.hash.slice(1));
    return { category: params.get("category") || "", item: params.get("item") || "", q: params.get("q") || "", section: params.get("section") || "" };
  }

  function breadcrumbs(item, category) {
    const node = $("breadcrumb");
    node.replaceChildren(link("英灵回声", "#"));
    const add = (child) => node.append(element("span", "›", "divider"), child);
    add(link("物品 / 方块", "#"));
    if (category) add(link(category, route({ category })));
    if (item) add(element("span", item.name));
  }

  function categories(selected) {
    const nav = $("categories");
    nav.replaceChildren();
    for (const category of ["", ...data.categories]) {
      const count = category ? data.items.filter((item) => item.category === category).length : data.items.length;
      const a = link("", route({ category }), "category-link" + (selected === category ? " active" : ""));
      a.append(element("span", category || "全部物品"), element("span", count, "count"));
      if (selected === category) a.setAttribute("aria-current", "page");
      nav.append(a);
    }
  }

  function itemCard(item) {
    const a = link("", route({ category: item.category, item: item.key }), "item-card");
    const img = element("img");
    img.src = "icons/" + item.key + ".png";
    img.width = img.height = 40;
    img.alt = "";
    const name = element("span", item.name);
    if (item.key === "tutorial_manual") name.append(element("small", "教程手册", "manual-subtitle"));
    a.append(img, name);
    return a;
  }

  function catalog(s) {
    $("catalog-title").textContent = s.category || "全部物品";
    $("search").value = s.q;
    $("clear-search").hidden = !s.q;
    const query = s.q.trim().toLocaleLowerCase();
    const items = data.items.filter((item) => (!s.category || item.category === s.category) &&
      [item.name, item.englishName, item.registerName, item.key === "tutorial_manual" ? "教程手册" : ""].some((text) => text.toLocaleLowerCase().includes(query)));
    $("result-count").textContent = (query ? "找到 " : "共 ") + items.length + " 件资料";
    const target = $("catalog");
    target.replaceChildren();
    if (!items.length) {
      const empty = element("div", undefined, "empty-state");
      empty.append(element("p", "没有找到匹配的物品，试试其他名称。"));
      empty.append(link("清空搜索", route({ category: s.category })));
      target.append(empty);
      return;
    }
    for (const category of data.categories) {
      const group = items.filter((item) => item.category === category);
      if (!group.length) continue;
      const section = element("section", undefined, "catalog-group");
      const heading = element("h3", undefined, "group-heading");
      heading.append(link(category, route({ category })), element("small", group.length + " 件"));
      const grid = element("div", undefined, "item-grid");
      group.forEach((item) => grid.append(itemCard(item)));
      section.append(heading, grid);
      target.append(section);
    }
  }

  // Only format the saved article fragment; never load scripts, styles or embeds
  // from an imported document. Links to these 43 items stay inside the preview.
  function articleFragment(html, item) {
    const parsed = new DOMParser().parseFromString(html, "text/html");
    const allowed = new Set(["P", "BR", "UL", "OL", "LI", "TABLE", "THEAD", "TBODY", "TR", "TH", "TD", "A", "STRONG", "EM", "B", "I", "CODE", "SPAN", "H2", "H3"]);
    parsed.querySelectorAll("script,style,iframe,object,embed,form,svg,math").forEach((node) => node.remove());
    for (const node of [...parsed.body.querySelectorAll("*")]) {
      if (!allowed.has(node.tagName)) { node.replaceWith(...node.childNodes); continue; }
      const href = node.getAttribute("href");
      for (const attr of [...node.attributes]) node.removeAttribute(attr.name);
      if (node.tagName === "A" && href) {
        const local = byUrl.get(href);
        if (local) node.href = route({ category: local.category, item: local.key });
        else if (/^https:\/\//i.test(href)) { node.href = href; node.target = "_blank"; node.rel = "noopener noreferrer"; }
      }
    }
    const headings = [];
    for (const p of [...parsed.body.querySelectorAll("p")]) {
      const match = p.textContent.trim().match(/^\[h([12])=([^\]]+)\]$/);
      if (!match) continue;
      const id = "section-" + (headings.length + 1);
      const heading = element(markupMode ? "p" : match[1] === "1" ? "h2" : "h3", markupMode ? match[0] : match[2], markupMode ? "heading-code" : "");
      heading.id = id;
      p.replaceWith(heading);
      headings.push({ id, title: match[2], level: match[1] });
    }
    const toc = $("toc");
    toc.replaceChildren();
    for (const heading of headings) {
      const a = link(heading.title, route({ category: item.category, item: item.key, section: heading.id }), heading.level === "2" ? "toc-sub" : "");
      toc.append(a);
    }
    $("toc-panel").hidden = !headings.length;
    const fragment = document.createDocumentFragment();
    fragment.append(...parsed.body.childNodes);
    return fragment;
  }

  function article(item) {
    $("article-name").textContent = item.name;
    $("article-category").textContent = item.category;
    $("article-english").textContent = item.englishName;
    $("article-icon").src = "icons/" + item.key + ".png";
    $("site-link").href = item.url;
    const meta = $("article-meta");
    meta.replaceChildren();
    for (const [label, value] of [["注册名", item.registerName], ["类型", item.type === "Block" ? "方块" : "物品"], ["最大堆叠", item.maxStack]]) {
      const row = element("div");
      const dd = element("dd");
      dd.append(element(label === "注册名" ? "code" : "span", value));
      row.append(element("dt", label), dd);
      meta.append(row);
    }
    $("article-body").replaceChildren(articleFragment(item.html, item));
    const siblings = data.items.filter((entry) => entry.category === item.category);
    const index = siblings.indexOf(item);
    const pager = $("article-pager");
    pager.replaceChildren();
    for (const [label, sibling] of [["上一篇", siblings[index - 1]], ["下一篇", siblings[index + 1]]]) {
      if (!sibling) { pager.append(element("span")); continue; }
      const a = link("", route({ category: sibling.category, item: sibling.key }));
      a.append(element("small", label), element("span", sibling.name));
      pager.append(a);
    }
  }

  function render({ resetScroll = false } = {}) {
    const s = state();
    clearTimeout(searchTimer);
    const item = byKey.get(s.item);
    const category = item ? item.category : data.categories.includes(s.category) ? s.category : "";
    const unknown = Boolean(s.item && !item);
    categories(category);
    breadcrumbs(item, category);
    $("catalog-view").hidden = Boolean(item) || unknown;
    $("article-view").hidden = !item;
    $("not-found").hidden = !unknown;
    $("toc-panel").hidden = true;
    if (item) article(item);
    else if (!unknown) catalog({ ...s, category });
    document.title = (item ? item.name : category || "全部物品") + " · 英灵回声稿件预览";
    if (s.section && item) $(s.section)?.scrollIntoView({ block: "start" });
    else if (resetScroll) window.scrollTo(0, 0);
    if (resetScroll && s.item !== activeItem) $("main").focus({ preventScroll: true });
    activeItem = s.item;
  }

  function updateSearch() {
    const s = state();
    const q = $("search").value;
    // Replace search state without adding a browser-history step per keystroke.
    // Changing only the fragment also works when opened directly from disk.
    history.replaceState(null, "", route({ category: s.category, q }));
    render();
  }
  $("search").addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(updateSearch, 100);
  });
  $("search").addEventListener("keydown", (event) => { if (event.key === "Enter") { clearTimeout(searchTimer); updateSearch(); } });
  $("clear-search").addEventListener("click", () => { $("search").value = ""; updateSearch(); $("search").focus(); });
  for (const [id, enabled] of [["preview-mode", false], ["markup-mode", true]]) {
    $(id).addEventListener("click", () => {
      markupMode = enabled;
      for (const other of ["preview-mode", "markup-mode"]) {
        $(other).classList.toggle("selected", other === id);
        $(other).setAttribute("aria-pressed", String(other === id));
      }
      const item = byKey.get(state().item);
      if (item) article(item);
    });
  }
  $("total-count").textContent = data.items.length;
  $("snapshot-date").textContent = data.snapshotDate;
  window.addEventListener("hashchange", () => render({ resetScroll: true }));
  render();
})();
