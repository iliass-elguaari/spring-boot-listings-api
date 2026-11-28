// Front end for the Listings API.
// Talks to the Spring Boot API on the same origin. If no API answers
// (for example on the GitHub Pages demo), it falls back to a demo store
// in localStorage so the page still works.

const API = "/listings";
const DEMO_KEY = "listings-demo";
let demo = false;

const $ = (id) => document.getElementById(id);
const money = (n) => "$" + Number(n).toLocaleString("en-CA");

// ---- demo store (used only when the API is unreachable) ----
function demoLoad() {
  try {
    const saved = JSON.parse(localStorage.getItem(DEMO_KEY));
    if (Array.isArray(saved)) return saved;
  } catch (_) { /* ignore */ }
  // Sample listings, dated a few days back so "Added" and "Newest first" have something to show.
  const daysAgo = (n) => new Date(Date.now() - n * 86400000).toISOString();
  return [
    { id: 1, address: "75 Laurier Ave E, Ottawa", monthlyRent: 950, bedrooms: 1, createdAt: daysAgo(6) },
    { id: 2, address: "200 Rideau St, Ottawa", monthlyRent: 1400, bedrooms: 2, createdAt: daysAgo(3) },
    { id: 3, address: "45 Mann Ave, Ottawa", monthlyRent: 780, bedrooms: 1, createdAt: daysAgo(1) },
  ];
}
function demoSave(list) {
  try { localStorage.setItem(DEMO_KEY, JSON.stringify(list)); } catch (_) { /* ignore */ }
}

// ---- API calls with demo fallback ----
// filters: { maxRent, minBedrooms } (each a number or null), q (address search text)
// and sort ("", "rent", "-rent" or "newest"). Returns one page: { items, total }.
const PAGE_SIZE = 10;
async function fetchListings(filters, page) {
  const { maxRent, minBedrooms, q, sort } = filters;
  if (!demo) {
    try {
      const params = new URLSearchParams();
      if (maxRent != null) params.set("maxRent", maxRent);
      if (minBedrooms != null) params.set("minBedrooms", minBedrooms);
      if (q) params.set("q", q);
      if (sort) params.set("sort", sort);
      params.set("page", page);
      params.set("size", PAGE_SIZE);
      const url = `${API}?${params}`;
      const res = await fetch(url, { headers: { Accept: "application/json" } });
      const type = res.headers.get("content-type") || "";
      if (res.ok && type.includes("application/json")) {
        const items = await res.json();
        const total = Number(res.headers.get("X-Total-Count") ?? items.length);
        return { items, total };
      }
      throw new Error("no api");
    } catch (_) {
      demo = true;
      $("demo-banner").hidden = false;
    }
  }
  const matches = demoLoad().filter((l) =>
    (!q || l.address.toLowerCase().includes(q.toLowerCase())) &&
    (maxRent == null || l.monthlyRent <= maxRent) &&
    (minBedrooms == null || l.bedrooms >= minBedrooms));
  // Same order as the API: by rent or newest when asked, ties and the default by id.
  const time = (l) => (l.createdAt ? Date.parse(l.createdAt) : -Infinity);
  if (sort === "newest") {
    matches.sort((a, b) => (time(b) - time(a)) || b.id - a.id);
  } else {
    const dir = sort === "-rent" ? -1 : 1;
    matches.sort((a, b) =>
      (sort ? dir * (a.monthlyRent - b.monthlyRent) : 0) || a.id - b.id);
  }
  return { items: matches.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE), total: matches.length };
}

// Error carrying the API's per-field messages, e.g. { monthlyRent: "monthlyRent cannot be negative" }
class FieldErrors extends Error {
  constructor(fields) {
    super("Please fix the highlighted fields.");
    this.fields = fields;
  }
}

// Creates a listing, or updates it when an id is given.
async function saveListing(listing, id) {
  if (demo) {
    const all = demoLoad();
    if (id == null) {
      const newId = all.reduce((m, l) => Math.max(m, l.id), 0) + 1;
      const now = new Date().toISOString();
      all.push({ id: newId, ...listing, createdAt: now, updatedAt: now });
    } else {
      const i = all.findIndex((l) => l.id === id);
      if (i === -1) throw new Error("That listing no longer exists.");
      all[i] = { ...all[i], ...listing, id, updatedAt: new Date().toISOString() };
    }
    demoSave(all);
    return;
  }
  const res = await fetch(id == null ? API : `${API}/${id}`, {
    method: id == null ? "POST" : "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(listing),
  });
  if (res.ok) return;
  if (res.status === 400) {
    const body = await res.json().catch(() => null);
    if (body && body.errors) throw new FieldErrors(body.errors);
    throw new Error("The API rejected that listing.");
  }
  if (res.status === 404) throw new Error("That listing no longer exists.");
  throw new Error(`Server error (${res.status}).`);
}

async function deleteListing(id) {
  if (demo) {
    demoSave(demoLoad().filter((l) => l.id !== id));
    return;
  }
  const res = await fetch(`${API}/${id}`, { method: "DELETE" });
  if (!res.ok && res.status !== 404) throw new Error(`Could not delete (${res.status}).`);
}

// ---- form state ----
// The page shows the cheapest listings first unless the visitor picks another order.
let filters = { maxRent: null, minBedrooms: null, q: "", sort: "rent" };
let editingId = null;

function showFieldErrors(fields) {
  for (const span of document.querySelectorAll(".field-error")) {
    const name = span.dataset.for;
    const msg = (fields && fields[name]) || "";
    span.textContent = msg;
    $("add-form")[name].setAttribute("aria-invalid", msg ? "true" : "false");
  }
}

function startEdit(listing) {
  editingId = listing.id;
  const f = $("add-form");
  f.address.value = listing.address;
  f.monthlyRent.value = listing.monthlyRent;
  f.bedrooms.value = listing.bedrooms;
  $("form-title").textContent = "Edit listing";
  $("submit-btn").textContent = "Save changes";
  $("cancel-edit").hidden = false;
  $("form-error").textContent = "";
  showFieldErrors(null);
  f.address.focus();
  drawList(shown); // just highlight the row, keeping any extra pages loaded
}

function stopEdit() {
  editingId = null;
  $("add-form").reset();
  $("form-title").textContent = "Post a listing";
  $("submit-btn").textContent = "Add listing";
  $("cancel-edit").hidden = true;
  $("form-error").textContent = "";
  showFieldErrors(null);
  render();
}

// ---- rendering ----

// "Nov 27", or "Nov 27, 2025" when it isn't this year.
function addedOn(iso) {
  const d = new Date(iso);
  const opts = { month: "short", day: "numeric" };
  if (d.getFullYear() !== new Date().getFullYear()) opts.year = "numeric";
  return d.toLocaleDateString("en-CA", opts);
}

// Listings loaded so far and how many match in total, for "Load more".
let shown = [];
let total = 0;
let page = 0;

// Starts over from the first page, e.g. after a filter change or a save.
async function render() {
  page = 0;
  await loadPage(false);
}

async function loadPage(append) {
  $("list-error").textContent = "";
  let result;
  try {
    result = await fetchListings(filters, page);
  } catch (e) {
    $("list-error").textContent = e.message;
    return;
  }
  shown = append ? shown.concat(result.items) : result.items;
  total = result.total;
  drawList(shown);
}

function drawList(list) {
  const ul = $("listings");
  ul.replaceChildren();
  for (const l of list) {
    const li = document.createElement("li");
    const info = document.createElement("div");
    const addr = document.createElement("div");
    addr.className = "addr";
    addr.textContent = l.address;
    const meta = document.createElement("div");
    meta.className = "meta";
    const beds = `${l.bedrooms} bedroom${l.bedrooms === 1 ? "" : "s"}`;
    meta.textContent = l.createdAt ? `${beds} · Added ${addedOn(l.createdAt)}` : beds;
    info.append(addr, meta);

    const right = document.createElement("div");
    right.className = "item-actions";
    const rent = document.createElement("span");
    rent.className = "rent";
    rent.textContent = `${money(l.monthlyRent)}/mo`;
    const del = document.createElement("button");
    del.className = "ghost";
    del.textContent = "Delete";
    del.setAttribute("aria-label", `Delete ${l.address}`);
    del.onclick = async () => {
      try {
        await deleteListing(l.id);
        if (l.id === editingId) stopEdit(); else render();
      }
      catch (e) { $("list-error").textContent = e.message; }
    };
    const edit = document.createElement("button");
    edit.className = "ghost";
    edit.textContent = "Edit";
    edit.setAttribute("aria-label", `Edit ${l.address}`);
    edit.onclick = () => startEdit(l);
    right.append(rent, edit, del);
    if (l.id === editingId) li.classList.add("editing");

    li.append(info, right);
    ul.append(li);
  }
  $("count").textContent = `(${total})`;
  $("empty").hidden = list.length > 0;
  const more = $("load-more");
  more.hidden = list.length >= total;
  more.textContent = `Load more (${total - list.length} left)`;
}

$("add-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target;
  const address = f.address.value.trim();
  const monthlyRent = Number(f.monthlyRent.value);
  const bedrooms = Number(f.bedrooms.value);

  // Same rules as the API, checked here first so the user gets instant feedback.
  const fields = {};
  if (!address) fields.address = "address is required";
  else if (address.length > 200) fields.address = "address must be at most 200 characters";
  if (f.monthlyRent.value === "" || !Number.isInteger(monthlyRent) || monthlyRent < 0)
    fields.monthlyRent = "Rent must be a whole number, 0 or more";
  if (f.bedrooms.value === "" || !Number.isInteger(bedrooms) || bedrooms < 0)
    fields.bedrooms = "Bedrooms must be a whole number, 0 or more";
  showFieldErrors(fields);
  const err = $("form-error");
  if (Object.keys(fields).length) {
    err.textContent = "Please fix the highlighted fields.";
    return;
  }
  err.textContent = "";
  try {
    await saveListing({ address, monthlyRent, bedrooms }, editingId);
    if (editingId != null) stopEdit();
    else { f.reset(); render(); }
  } catch (ex) {
    if (ex instanceof FieldErrors) showFieldErrors(ex.fields);
    err.textContent = ex.message;
  }
});

$("cancel-edit").addEventListener("click", stopEdit);

$("filter-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const read = (input) => (input.value === "" ? null : Number(input.value));
  const maxRent = read(e.target.maxRent);
  const minBedrooms = read(e.target.minBedrooms);
  if ((maxRent != null && maxRent < 0) || (minBedrooms != null && minBedrooms < 0)) {
    $("list-error").textContent = "Filters cannot be negative.";
    return;
  }
  filters = { ...filters, maxRent, minBedrooms, sort: e.target.sort.value };
  render();
});

$("clear-filter").addEventListener("click", () => {
  $("filter-form").reset();
  $("search").value = "";
  filters = { maxRent: null, minBedrooms: null, q: "", sort: "rent" };
  render();
});

// Changing the order applies right away, keeping the current filters.
$("sort").addEventListener("change", (e) => {
  filters = { ...filters, sort: e.target.value };
  render();
});

// Search as you type, waiting for a short pause so each keystroke isn't a request.
let searchTimer;
$("search").addEventListener("input", (e) => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    filters = { ...filters, q: e.target.value.trim() };
    render();
  }, 250);
});

$("load-more").addEventListener("click", () => {
  page += 1;
  loadPage(true);
});

render();
