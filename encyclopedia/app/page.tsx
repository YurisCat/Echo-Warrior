"use client";

import {
  type CSSProperties,
  type PointerEvent as ReactPointerEvent,
  type WheelEvent as ReactWheelEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import atlasSource from "@/content/atlas.zh_cn.json";

type Status = "implemented" | "development" | "planned";
type ConnectionKind = "primary" | "related" | "skill";

type Category = {
  id: string;
  label: string;
  shortLabel: string;
  symbol: string;
  kicker: string;
  description: string;
  accent: string;
};

type ArticleSection = {
  type: "prose" | "stats" | "steps" | "cards" | "recipe" | "callout";
  title: string;
  tone?: "info" | "warning" | "planned" | "muted";
  body?: string;
  paragraphs?: string[];
  items?: Array<{
    title?: string;
    label?: string;
    value?: string;
    meta?: string;
    note?: string;
    body?: string;
    icon?: string;
  }>;
  grid?: Array<{
    name: string;
    icon: string;
    count?: number;
  } | null>;
  output?: {
    name: string;
    icon: string;
    count?: number;
  };
  shapeless?: boolean;
};

type AtlasNode = {
  id: string;
  categoryId: string;
  nodeType?: "hero" | "skill";
  parentId?: string;
  title: string;
  subtitle: string;
  status: Status;
  x: number;
  y: number;
  symbol: string;
  icon?: string;
  connections: Array<{ targetId: string; kind: ConnectionKind }>;
  article: {
    summary: string;
    tags: string[];
    sections: ArticleSection[];
    related: string[];
  };
};

type AtlasData = {
  schemaVersion: number;
  locale: string;
  meta: {
    title: string;
    displayTitle: string;
    description: string;
    lastVerified: string;
  };
  categories: Category[];
  nodes: AtlasNode[];
};

type MapView = { x: number; y: number; scale: number };

type PersistedViewState = {
  selectedNodeId?: string;
  activeCategoryId?: string;
  splitPercent?: number;
  views?: Record<string, MapView>;
  articleScroll?: Record<string, number>;
  windowScrollY?: number;
};

type HeroGrowthProfile = {
  name: string;
  baseHealth: number;
  baseAttack: number;
  attackLabel: string;
};

const atlas = atlasSource as AtlasData;
const DEFAULT_WORLD_WIDTH = 880;
const DEFAULT_WORLD_HEIGHT = 620;
const MIN_SCALE = 0.38;
const MAX_SCALE = 1.65;
const VIEW_STATE_KEY = "echo-archive:view-state:v3";
const INITIAL_VIEW: MapView = { x: -90, y: -58, scale: 0.82 };

const isInitialView = (view: MapView) =>
  view.x === INITIAL_VIEW.x && view.y === INITIAL_VIEW.y && view.scale === INITIAL_VIEW.scale;

const heroGrowthProfiles: Record<string, HeroGrowthProfile> = {
  roman_legionary: { name: "罗马军团兵", baseHealth: 30, baseAttack: 6, attackLabel: "攻击力" },
  aztec_warrior: { name: "阿兹特克勇士", baseHealth: 34, baseAttack: 8, attackLabel: "攻击力" },
  egyptian_archer: { name: "埃及弓箭手", baseHealth: 28, baseAttack: 5, attackLabel: "远程攻击" },
  guandao_warrior: { name: "中国关刀战士", baseHealth: 30, baseAttack: 7, attackLabel: "攻击力" },
  japanese_samurai: { name: "日本武士", baseHealth: 28, baseAttack: 6, attackLabel: "攻击力" },
};

const statusCopy: Record<Status, string> = {
  implemented: "已实装",
  development: "开发中",
  planned: "计划中",
};

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max);

const formatGrowthValue = (value: number) =>
  Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/0+$/, "").replace(/\.$/, "");

function HeroLevelLab({ profile }: { profile: HeroGrowthProfile }) {
  const [level, setLevel] = useState(1);
  const multiplier = 1 + (level - 1) / 29;
  const health = profile.baseHealth * multiplier;
  const attack = profile.baseAttack * multiplier;
  const totalExperience = (level - 1) * (level + 15);
  const nextExperience = level === 30 ? 0 : 15 + 2 * level;

  return (
    <section className="interactive-lab" aria-labelledby="level-lab-title">
      <div className="lab-heading">
        <div>
          <p className="section-kicker">INTERACTIVE RECORD</p>
          <h2 id="level-lab-title">{profile.name}成长推演</h2>
        </div>
        <strong>Lv. {level}</strong>
      </div>
      <label className="range-row">
        <span>拖动查看等级变化</span>
        <input
          aria-label="军团兵等级"
          type="range"
          min="1"
          max="30"
          value={level}
          onChange={(event) => setLevel(Number(event.target.value))}
        />
      </label>
      <div className="lab-metrics">
        <div><span>最大生命</span><strong>{formatGrowthValue(health)}</strong></div>
        <div><span>{profile.attackLabel}</span><strong>{formatGrowthValue(attack)}</strong></div>
        <div><span>累计经验</span><strong>{totalExperience}</strong></div>
        <div><span>下一级</span><strong>{level === 30 ? "满级" : nextExperience}</strong></div>
      </div>
    </section>
  );
}

function FuelLab() {
  const [fuel, setFuel] = useState(500);
  const summons = Math.floor(fuel / 100);
  const recoverableHealth = Math.floor((fuel % 100) / 2);

  return (
    <section className="interactive-lab fuel-lab" aria-labelledby="fuel-lab-title">
      <div className="lab-heading">
        <div>
          <p className="section-kicker">FUEL SIMULATION</p>
          <h2 id="fuel-lab-title">燃料余量推演</h2>
        </div>
        <strong>{fuel} / 1000</strong>
      </div>
      <label className="range-row">
        <span>调整当前内部燃料</span>
        <input
          aria-label="召唤器内部燃料"
          type="range"
          min="0"
          max="1000"
          step="10"
          value={fuel}
          onChange={(event) => setFuel(Number(event.target.value))}
        />
      </label>
      <div className="fuel-track" aria-hidden="true">
        <span style={{ width: `${fuel / 10}%` }} />
      </div>
      <div className="lab-metrics compact">
        <div><span>完整召唤</span><strong>{summons} 次</strong></div>
        <div><span>召唤后余量</span><strong>{fuel % 100}</strong></div>
        <div><span>余量可恢复</span><strong>{recoverableHealth} 生命</strong></div>
      </div>
    </section>
  );
}

function Section({ section }: { section: ArticleSection }) {
  if (section.type === "prose") {
    return (
      <section className="article-section">
        <p className="section-kicker">ARCHIVE NOTE</p>
        <h2>{section.title}</h2>
        <div className="prose-stack">
          {section.paragraphs?.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
        </div>
      </section>
    );
  }

  if (section.type === "stats") {
    return (
      <section className="article-section">
        <p className="section-kicker">FIELD DATA</p>
        <h2>{section.title}</h2>
        <div className="stat-grid">
          {section.items?.map((item) => (
            <div className="stat-card" key={`${item.label}-${item.value}`}>
              {item.icon && <img src={item.icon} alt="" />}
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              {item.note && <small>{item.note}</small>}
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (section.type === "steps") {
    return (
      <section className="article-section">
        <p className="section-kicker">SEQUENCE</p>
        <h2>{section.title}</h2>
        <ol className="step-list">
          {section.items?.map((item, index) => (
            <li key={item.title}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <div><h3>{item.title}</h3><p>{item.body}</p></div>
            </li>
          ))}
        </ol>
      </section>
    );
  }

  if (section.type === "cards") {
    return (
      <section className="article-section">
        <p className="section-kicker">LINKED RECORDS</p>
        <h2>{section.title}</h2>
        <div className="record-grid">
          {section.items?.map((item) => (
            <article className="record-card" key={item.title}>
              {item.icon && <img src={item.icon} alt="" />}
              <div>
                <p>{item.meta}</p>
                <h3>{item.title}</h3>
                <span>{item.body}</span>
              </div>
            </article>
          ))}
        </div>
      </section>
    );
  }

  if (section.type === "recipe") {
    return (
      <section className="article-section">
        <p className="section-kicker">CRAFTING RECORD</p>
        <h2>{section.title}</h2>
        <div className="recipe-panel">
          <div className={`craft-grid ${section.shapeless ? "shapeless" : ""}`} aria-label={section.shapeless ? "无序合成材料" : "有序合成表"}>
            {Array.from({ length: 9 }, (_, index) => {
              const slot = section.grid?.[index] ?? null;
              return (
                <div className="craft-slot" key={index} title={slot?.name}>
                  {slot && <>
                    <img src={slot.icon} alt={slot.name} />
                    {(slot.count ?? 1) > 1 && <span>{slot.count}</span>}
                  </>}
                </div>
              );
            })}
          </div>
          <span className="recipe-arrow" aria-hidden="true">→</span>
          <div className="recipe-result">
            <div className="craft-slot output" title={section.output?.name}>
              {section.output && <>
                <img src={section.output.icon} alt={section.output.name} />
                {(section.output.count ?? 1) > 1 && <span>{section.output.count}</span>}
              </>}
            </div>
            <strong>{section.output?.name}</strong>
            <small>{section.shapeless ? "无序合成" : "有序合成"}</small>
          </div>
        </div>
      </section>
    );
  }

  return (
    <aside className={`article-callout ${section.tone ?? "info"}`}>
      <p className="section-kicker">ARCHIVIST NOTICE</p>
      <h2>{section.title}</h2>
      <p>{section.body}</p>
    </aside>
  );
}

export default function Home() {
  const nodeById = useMemo(
    () => new Map(atlas.nodes.map((node) => [node.id, node])),
    [],
  );
  const [activeCategoryId, setActiveCategoryId] = useState(atlas.categories[0].id);
  const [selectedNodeId, setSelectedNodeId] = useState("archive_overview");
  const [search, setSearch] = useState("");
  const [splitPercent, setSplitPercent] = useState(43);
  const [viewStateReady, setViewStateReady] = useState(false);
  const [views, setViews] = useState<Record<string, MapView>>(() =>
    Object.fromEntries(atlas.categories.map((category) => [category.id, { ...INITIAL_VIEW }])),
  );

  const shellRef = useRef<HTMLElement>(null);
  const mapRef = useRef<HTMLDivElement>(null);
  const articleRef = useRef<HTMLElement>(null);
  const dragRef = useRef<{ pointerId: number; startX: number; startY: number; view: MapView } | null>(null);
  const articleScrollRef = useRef<Record<string, number>>({});
  const windowScrollRef = useRef(0);
  const persistTimerRef = useRef<number | null>(null);
  const initializedCategories = useRef(new Set<string>());

  const activeCategory = atlas.categories.find((category) => category.id === activeCategoryId)!;
  const selectedNode = nodeById.get(selectedNodeId) ?? atlas.nodes[0];
  const categoryNodes = atlas.nodes.filter((node) => node.categoryId === activeCategoryId);
  const currentView = views[activeCategoryId];
  const worldSize = useMemo(() => ({
    width: Math.max(DEFAULT_WORLD_WIDTH, ...categoryNodes.map((node) => node.x + 150)),
    height: Math.max(DEFAULT_WORLD_HEIGHT, ...categoryNodes.map((node) => node.y + 150)),
  }), [categoryNodes]);

  const searchResults = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("zh-CN");
    if (!query) return [];
    return atlas.nodes
      .filter((node) =>
        [node.title, node.subtitle, ...node.article.tags]
          .join(" ")
          .toLocaleLowerCase("zh-CN")
          .includes(query),
      )
      .slice(0, 8);
  }, [search]);

  const visibleEdges = useMemo(() => {
    const edges: Array<{ from: AtlasNode; to: AtlasNode; kind: ConnectionKind; key: string }> = [];
    const seen = new Set<string>();
    for (const node of categoryNodes) {
      for (const connection of node.connections) {
        const target = nodeById.get(connection.targetId);
        if (!target || target.categoryId !== activeCategoryId) continue;
        const key = [node.id, target.id].sort().join("--");
        if (seen.has(key)) continue;
        seen.add(key);
        edges.push({ from: node, to: target, kind: connection.kind, key });
      }
    }
    return edges;
  }, [activeCategoryId, categoryNodes, nodeById]);

  const updateView = useCallback((categoryId: string, view: MapView) => {
    setViews((current) => ({ ...current, [categoryId]: view }));
  }, []);

  const persistViewState = useCallback(() => {
    if (!viewStateReady) return;
    const initializedViews = Object.fromEntries(
      [...initializedCategories.current]
        .map((categoryId) => [categoryId, views[categoryId]] as const)
        .filter((entry): entry is readonly [string, MapView] => Boolean(entry[1]) && !isInitialView(entry[1])),
    );
    const state: PersistedViewState = {
      selectedNodeId,
      activeCategoryId,
      splitPercent,
      views: initializedViews,
      articleScroll: articleScrollRef.current,
      windowScrollY: windowScrollRef.current,
    };
    try {
      window.localStorage.setItem(VIEW_STATE_KEY, JSON.stringify(state));
    } catch {
      // 浏览器禁用本地存储时，百科仍可正常使用，只是不保留位置。
    }
  }, [activeCategoryId, selectedNodeId, splitPercent, viewStateReady, views]);

  const schedulePersist = useCallback(() => {
    if (!viewStateReady) return;
    if (persistTimerRef.current) window.clearTimeout(persistTimerRef.current);
    persistTimerRef.current = window.setTimeout(persistViewState, 120);
  }, [persistViewState, viewStateReady]);

  const fitCategory = useCallback((categoryId = activeCategoryId) => {
    const viewport = mapRef.current;
    const nodes = atlas.nodes.filter((node) => node.categoryId === categoryId);
    if (!viewport || nodes.length === 0) return;
    const minX = Math.min(...nodes.map((node) => node.x)) - 95;
    const maxX = Math.max(...nodes.map((node) => node.x)) + 95;
    const minY = Math.min(...nodes.map((node) => node.y)) - 100;
    const maxY = Math.max(...nodes.map((node) => node.y)) + 100;
    const scale = clamp(
      Math.min(viewport.clientWidth / (maxX - minX), viewport.clientHeight / (maxY - minY)) * 0.88,
      MIN_SCALE,
      1.08,
    );
    updateView(categoryId, {
      scale,
      x: viewport.clientWidth / 2 - ((minX + maxX) / 2) * scale,
      y: viewport.clientHeight / 2 - ((minY + maxY) / 2) * scale,
    });
  }, [activeCategoryId, updateView]);

  const centerNode = (node: AtlasNode) => {
    const viewport = mapRef.current;
    if (!viewport) return;
    const view = views[node.categoryId];
    updateView(node.categoryId, {
      ...view,
      x: viewport.clientWidth / 2 - node.x * view.scale,
      y: viewport.clientHeight / 2 - node.y * view.scale,
    });
  };

  const chooseNode = (nodeId: string, locate = false) => {
    const nextNode = nodeById.get(nodeId);
    if (!nextNode) return;
    if (window.matchMedia("(max-width: 900px)").matches) {
      articleScrollRef.current[selectedNodeId] = window.scrollY;
    } else if (articleRef.current) {
      articleScrollRef.current[selectedNodeId] = articleRef.current.scrollTop;
    }
    setSelectedNodeId(nodeId);
    setActiveCategoryId(nextNode.categoryId);
    setSearch("");
    if (locate) window.setTimeout(() => centerNode(nextNode), 30);
  };

  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      try {
        const raw = window.localStorage.getItem(VIEW_STATE_KEY);
        if (raw) {
          const restored = JSON.parse(raw) as PersistedViewState;
          const restoredNode = restored.selectedNodeId ? nodeById.get(restored.selectedNodeId) : undefined;
          if (restoredNode) {
            setSelectedNodeId(restoredNode.id);
          }
          if (restored.activeCategoryId && atlas.categories.some((category) => category.id === restored.activeCategoryId)) {
            setActiveCategoryId(restored.activeCategoryId);
          } else if (restoredNode) {
            setActiveCategoryId(restoredNode.categoryId);
          }
          if (typeof restored.splitPercent === "number") {
            setSplitPercent(clamp(restored.splitPercent, 29, 48));
          }
          if (restored.views) {
            setViews((current) => {
              const next = { ...current };
              for (const category of atlas.categories) {
                const view = restored.views?.[category.id];
                if (!view || !Number.isFinite(view.x) || !Number.isFinite(view.y) || !Number.isFinite(view.scale)) continue;
                const safeView = { ...view, scale: clamp(view.scale, MIN_SCALE, MAX_SCALE) };
                if (isInitialView(safeView)) continue;
                next[category.id] = safeView;
                initializedCategories.current.add(category.id);
              }
              return next;
            });
          }
          if (restored.articleScroll) articleScrollRef.current = restored.articleScroll;
          if (typeof restored.windowScrollY === "number" && Number.isFinite(restored.windowScrollY)) {
            windowScrollRef.current = Math.max(0, restored.windowScrollY);
          }
        }
      } catch {
        // 损坏或旧版状态会被安全忽略，并在下一次交互时覆盖。
      }
      setViewStateReady(true);
    });
    return () => cancelAnimationFrame(frame);
  }, [nodeById]);

  useEffect(() => {
    if (!viewStateReady) return;
    const frame = requestAnimationFrame(() => {
      const restoredScroll = articleScrollRef.current[selectedNodeId] ?? 0;
      if (articleRef.current) {
        articleRef.current.scrollTop = restoredScroll;
      }
      if (window.matchMedia("(max-width: 900px)").matches) {
        window.scrollTo({ top: restoredScroll || windowScrollRef.current });
      }
    });
    return () => cancelAnimationFrame(frame);
  }, [selectedNodeId, viewStateReady]);

  useEffect(() => {
    if (!viewStateReady) return;
    if (initializedCategories.current.has(activeCategoryId)) return;
    const frame = requestAnimationFrame(() => {
      fitCategory(activeCategoryId);
      initializedCategories.current.add(activeCategoryId);
    });
    return () => cancelAnimationFrame(frame);
  }, [activeCategoryId, fitCategory, viewStateReady]);

  useEffect(() => {
    schedulePersist();
  }, [schedulePersist]);

  useEffect(() => {
    const onWindowScroll = () => {
      windowScrollRef.current = window.scrollY;
      if (window.matchMedia("(max-width: 900px)").matches) {
        articleScrollRef.current[selectedNodeId] = window.scrollY;
      }
      schedulePersist();
    };
    const onPageHide = () => persistViewState();
    window.addEventListener("scroll", onWindowScroll, { passive: true });
    window.addEventListener("pagehide", onPageHide);
    return () => {
      window.removeEventListener("scroll", onWindowScroll);
      window.removeEventListener("pagehide", onPageHide);
      if (persistTimerRef.current) window.clearTimeout(persistTimerRef.current);
    };
  }, [persistViewState, schedulePersist, selectedNodeId]);

  const connectionPath = (from: AtlasNode, to: AtlasNode) => {
    const dx = to.x - from.x;
    const dy = to.y - from.y;
    if (Math.abs(dx) >= Math.abs(dy)) {
      const direction = Math.sign(dx) || 1;
      const bend = Math.max(42, Math.abs(dx) * 0.38);
      return `M ${from.x} ${from.y} C ${from.x + direction * bend} ${from.y}, ${to.x - direction * bend} ${to.y}, ${to.x} ${to.y}`;
    }
    const direction = Math.sign(dy) || 1;
    const bend = Math.max(42, Math.abs(dy) * 0.38);
    return `M ${from.x} ${from.y} C ${from.x} ${from.y + direction * bend}, ${to.x} ${to.y - direction * bend}, ${to.x} ${to.y}`;
  };

  const onMapPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if ((event.target as HTMLElement).closest("button, input")) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      view: currentView,
    };
    event.currentTarget.classList.add("is-dragging");
  };

  const onMapPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) return;
    updateView(activeCategoryId, {
      ...drag.view,
      x: drag.view.x + event.clientX - drag.startX,
      y: drag.view.y + event.clientY - drag.startY,
    });
  };

  const stopMapDrag = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (dragRef.current?.pointerId === event.pointerId) {
      dragRef.current = null;
      event.currentTarget.classList.remove("is-dragging");
      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId);
      }
    }
  };

  const onMapWheel = (event: ReactWheelEvent<HTMLDivElement>) => {
    event.preventDefault();
    const bounds = event.currentTarget.getBoundingClientRect();
    const pointerX = event.clientX - bounds.left;
    const pointerY = event.clientY - bounds.top;
    const nextScale = clamp(currentView.scale * Math.exp(-event.deltaY * 0.0012), MIN_SCALE, MAX_SCALE);
    const worldX = (pointerX - currentView.x) / currentView.scale;
    const worldY = (pointerY - currentView.y) / currentView.scale;
    updateView(activeCategoryId, {
      scale: nextScale,
      x: pointerX - worldX * nextScale,
      y: pointerY - worldY * nextScale,
    });
  };

  const onDividerPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.currentTarget.setPointerCapture(event.pointerId);
    event.currentTarget.dataset.resizing = "true";
  };

  const onDividerPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.currentTarget.dataset.resizing !== "true" || !shellRef.current) return;
    const bounds = shellRef.current.getBoundingClientRect();
    setSplitPercent(clamp(((event.clientX - bounds.left) / bounds.width) * 100, 29, 48));
  };

  const onDividerPointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.currentTarget.dataset.resizing = "false";
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  };

  const shellStyle = { "--map-size": `${splitPercent}%` } as CSSProperties;
  const categoryStyle = { "--category-accent": activeCategory.accent } as CSSProperties;

  return (
    <main className="archive-shell" ref={shellRef} style={shellStyle}>
      <section className="atlas-pane" style={categoryStyle} aria-label="知识地图">
        <aside className="category-rail" aria-label="百科分类">
          <div className="brand-mark" aria-label="Echo Archive">E<span>W</span></div>
          <nav>
            {atlas.categories.map((category) => (
              <button
                key={category.id}
                type="button"
                className={category.id === activeCategoryId ? "active" : ""}
                onClick={() => setActiveCategoryId(category.id)}
                aria-pressed={category.id === activeCategoryId}
                title={category.label}
                style={{ "--tab-accent": category.accent } as CSSProperties}
              >
                <span>{category.symbol}</span>
                <small>{category.shortLabel}</small>
              </button>
            ))}
          </nav>
          <div className="rail-index">{String(atlas.categories.findIndex((category) => category.id === activeCategoryId) + 1).padStart(2, "0")}</div>
        </aside>

        <div className="atlas-stage">
          <header className="atlas-toolbar">
            <div className="category-heading">
              <p>{activeCategory.kicker}</p>
              <h1>{activeCategory.label}</h1>
              <span>{activeCategory.description}</span>
            </div>
            <div className="search-box">
              <span aria-hidden="true">⌕</span>
              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="搜索节点…"
                aria-label="搜索百科节点"
              />
              {search && (
                <div className="search-results">
                  {searchResults.length > 0 ? searchResults.map((node) => (
                    <button type="button" key={node.id} onClick={() => chooseNode(node.id, true)}>
                      <span>{node.title}</span><small>{atlas.categories.find((category) => category.id === node.categoryId)?.shortLabel}</small>
                    </button>
                  )) : <p>没有找到匹配节点</p>}
                </div>
              )}
            </div>
          </header>

          <div
            className="map-viewport"
            ref={mapRef}
            onPointerDown={onMapPointerDown}
            onPointerMove={onMapPointerMove}
            onPointerUp={stopMapDrag}
            onPointerCancel={stopMapDrag}
            onWheel={onMapWheel}
          >
            <div className="map-watermark" aria-hidden="true">
              <span>{activeCategory.symbol}</span>
              <strong>{activeCategory.shortLabel}</strong>
            </div>
            <div
              className="map-world"
              style={{
                width: worldSize.width,
                height: worldSize.height,
                transform: `translate3d(${currentView.x}px, ${currentView.y}px, 0) scale(${currentView.scale})`,
              }}
            >
              <svg className="connection-layer" viewBox={`0 0 ${worldSize.width} ${worldSize.height}`} aria-hidden="true">
                {visibleEdges.map(({ from, to, kind, key }) => {
                  const active = from.id === selectedNodeId || to.id === selectedNodeId;
                  return <path key={key} d={connectionPath(from, to)} className={`${kind} ${active ? "active" : ""}`} />;
                })}
              </svg>

              {categoryNodes.map((node) => (
                <button
                  type="button"
                  key={node.id}
                  className={`atlas-node ${node.nodeType ? `kind-${node.nodeType}` : ""} ${node.status} ${node.id === selectedNodeId ? "selected" : ""}`}
                  style={{ left: node.x, top: node.y }}
                  onClick={() => chooseNode(node.id)}
                  onDoubleClick={() => centerNode(node)}
                  aria-label={`${node.title}，${statusCopy[node.status]}`}
                >
                  <span className="node-orbit" />
                  <span className="node-core">
                    {node.icon ? <img src={node.icon} alt="" /> : <b>{node.symbol}</b>}
                  </span>
                  <strong>{node.title}</strong>
                  <small>{statusCopy[node.status]}</small>
                </button>
              ))}
            </div>

            <div className="map-controls" aria-label="地图控制">
              <button type="button" onClick={() => updateView(activeCategoryId, { ...currentView, scale: clamp(currentView.scale + 0.12, MIN_SCALE, MAX_SCALE) })} aria-label="放大">＋</button>
              <button type="button" onClick={() => updateView(activeCategoryId, { ...currentView, scale: clamp(currentView.scale - 0.12, MIN_SCALE, MAX_SCALE) })} aria-label="缩小">－</button>
              <button type="button" onClick={() => fitCategory()} aria-label="显示当前分类全部节点">⊙</button>
              {selectedNode.categoryId === activeCategoryId && <button type="button" onClick={() => centerNode(selectedNode)} aria-label="定位当前条目">⌖</button>}
            </div>
            <p className="map-hint">拖动画布 · 滚轮缩放 · 双击节点定位</p>
          </div>
        </div>
      </section>

      <div
        className="pane-divider"
        role="separator"
        aria-orientation="vertical"
        aria-label="调整知识地图宽度"
        tabIndex={0}
        onPointerDown={onDividerPointerDown}
        onPointerMove={onDividerPointerMove}
        onPointerUp={onDividerPointerUp}
        onPointerCancel={onDividerPointerUp}
      ><span /></div>

      <article
        className="article-pane"
        ref={articleRef}
        onScroll={(event) => {
          articleScrollRef.current[selectedNodeId] = event.currentTarget.scrollTop;
          schedulePersist();
        }}
      >
        <header className="article-hero">
          <div className="article-breadcrumb">
            <span>{atlas.categories.find((category) => category.id === selectedNode.categoryId)?.label}</span>
            <i>／</i>
            <span>{selectedNode.subtitle}</span>
          </div>
          <div className="article-title-row">
            <div className="article-emblem">
              {selectedNode.icon ? <img src={selectedNode.icon} alt="" /> : <strong>{selectedNode.symbol}</strong>}
            </div>
            <div>
              <div className={`status-badge ${selectedNode.status}`}><span />{statusCopy[selectedNode.status]}</div>
              <h1>{selectedNode.title}</h1>
              <p>{selectedNode.article.summary}</p>
            </div>
          </div>
          <div className="tag-row">
            {selectedNode.article.tags.map((tag) => <span key={tag}>{tag}</span>)}
            <button type="button" onClick={() => {
              setActiveCategoryId(selectedNode.categoryId);
              window.setTimeout(() => centerNode(selectedNode), 30);
            }}>在地图中定位</button>
          </div>
        </header>

        <div className="article-content">
          {heroGrowthProfiles[selectedNode.id] && <HeroLevelLab profile={heroGrowthProfiles[selectedNode.id]} />}
          {(selectedNode.id === "fuel" || selectedNode.id === "summoner") && <FuelLab />}
          {selectedNode.article.sections.map((section, index) => (
            <Section key={`${section.title}-${index}`} section={section} />
          ))}

          <section className="related-section">
            <p className="section-kicker">FOLLOW THE THREAD</p>
            <h2>继续探索</h2>
            <div>
              {selectedNode.article.related.map((relatedId) => {
                const relatedNode = nodeById.get(relatedId);
                if (!relatedNode) return null;
                return (
                  <button type="button" key={relatedId} onClick={() => chooseNode(relatedId, true)}>
                    <span>{relatedNode.title}</span>
                    <small>{atlas.categories.find((category) => category.id === relatedNode.categoryId)?.shortLabel} · {statusCopy[relatedNode.status]}</small>
                    <b>↗</b>
                  </button>
                );
              })}
            </div>
          </section>

          <footer className="article-footer">
            <span>资料格式 v{atlas.schemaVersion}</span>
            <span>最后核对：{atlas.meta.lastVerified}</span>
          </footer>
        </div>
      </article>
    </main>
  );
}
