import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", { headers: { accept: "text/html" } }),
    { ASSETS: { fetch: async () => new Response("Not found", { status: 404 }) } },
    { waitUntil() {}, passThroughOnException() {} },
  );
}

test("server-renders the Echo Archive shell", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>回声档案馆｜Echo Warrior<\/title>/i);
  assert.match(html, /回声档案馆/);
  assert.match(html, /知识地图/);
  assert.doesNotMatch(html, /codex-preview|Your site is taking shape|react-loading-skeleton/i);
});

test("keeps the knowledge graph in a neutral content file", async () => {
  const content = JSON.parse(
    await readFile(new URL("../content/atlas.zh_cn.json", import.meta.url), "utf8"),
  );

  assert.equal(content.schemaVersion, 2);
  assert.equal(content.locale, "zh_cn");
  assert.ok(content.categories.length >= 6);
  assert.ok(content.nodes.length >= 15);
  assert.ok(content.nodes.every((node) => Number.isFinite(node.x) && Number.isFinite(node.y)));
  assert.ok(content.nodes.some((node) => node.id === "roman_legionary"));
  assert.equal(content.nodes.filter((node) => node.categoryId === "inheritance").length, 6);
  assert.equal(content.nodes.filter((node) => node.categoryId === "accessories").length, 26);
  assert.ok(content.nodes.some((node) =>
    node.id === "tomato_fish_accessory" &&
    node.article.sections.some((section) => section.type === "recipe" && section.grid.length === 9),
  ));
});
