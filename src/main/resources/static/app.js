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
  return [
    { id: 1, address: "75 Laurier Ave E, Ottawa", monthlyRent: 950, bedrooms: 1 },
    { id: 2, address: "200 Rideau St, Ottawa", monthlyRent: 1400, bedrooms: 2 },
    { id: 3, address: "45 Mann Ave, Ottawa", monthlyRent: 780, bedrooms: 1 },
  ];
}
function demoSave(list) {
  try { localStorage.setItem(DEMO_KEY, JSON.stringify(list)); } catch (_) { /* ignore */ }
}

// ---- API calls with demo fallback ----
async function fetchListings(maxRent) {
  if (!demo) {
    try {
      const url = maxRent == null ? API : `${API}?maxRent=${encodeURIComponent(maxRent)}`;
      const res = await fetch(url, { headers: { Accept: "application/json" } });
      const type = res.headers.get("content-type") || "";
      if (res.ok && type.includes("application/json")) return await res.json();
      throw new Error("no api");
    } catch (_) {
      demo = true;
      $("demo-banner").hidden = false;
    }
  }
  const all = demoLoad();
  return maxRent == null ? all : all.filter((l) => l.monthlyRent <= maxRent);
}

async function addListing(listing) {
  if (demo) {
    const all = demoLoad();
    const id = all.reduce((m, l) => Math.max(m, l.id), 0) + 1;
    all.push({ id, ...listing });
    demoSave(all);
    return;
  }
  const res = await fetch(API, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(listing),
  });
  if (!res.ok) throw new Error(res.status === 400 ? "The API rejected that listing." : `Server error (${res.status}).`);
}

async function deleteListing(id) {
  if (demo) {
    demoSave(demoLoad().filter((l) => l.id !== id));
    return;
  }
  const res = await fetch(`${API}/${id}`, { method: "DELETE" });
  if (!res.ok && res.status !== 404) throw new Error(`Could not delete (${res.status}).`);
}

// ---- rendering ----
let currentMax = null;

async function render() {
  $("list-error").textContent = "";
  let list;
  try {
    list = await fetchListings(currentMax);
  } catch (e) {
    $("list-error").textContent = e.message;
    return;
  }
  list.sort((a, b) => a.monthlyRent - b.monthlyRent);
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
    meta.textContent = `${l.bedrooms} bedroom${l.bedrooms === 1 ? "" : "s"}`;
    info.append(addr, meta);

    const right = document.createElement("div");
    right.style.display = "flex";
    right.style.gap = "12px";
    right.style.alignItems = "center";
    const rent = document.createElement("span");
    rent.className = "rent";
    rent.textContent = `${money(l.monthlyRent)}/mo`;
    const del = document.createElement("button");
    del.className = "ghost";
    del.textContent = "Delete";
    del.setAttribute("aria-label", `Delete ${l.address}`);
    del.onclick = async () => {
      try { await deleteListing(l.id); render(); }
      catch (e) { $("list-error").textContent = e.message; }
    };
    right.append(rent, del);

    li.append(info, right);
    ul.append(li);
  }
  $("count").textContent = `(${list.length})`;
  $("empty").hidden = list.length > 0;
}

$("add-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target;
  const address = f.address.value.trim();
  const monthlyRent = Number(f.monthlyRent.value);
  const bedrooms = Number(f.bedrooms.value);
  const err = $("form-error");
  if (!address) return (err.textContent = "Enter an address.");
  if (f.monthlyRent.value === "" || !Number.isInteger(monthlyRent) || monthlyRent < 0)
    return (err.textContent = "Rent must be a whole number, 0 or more.");
  if (f.bedrooms.value === "" || !Number.isInteger(bedrooms) || bedrooms < 0)
    return (err.textContent = "Bedrooms must be a whole number, 0 or more.");
  err.textContent = "";
  try {
    await addListing({ address, monthlyRent, bedrooms });
    f.reset();
    render();
  } catch (ex) {
    err.textContent = ex.message;
  }
});

$("filter-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const v = e.target.maxRent.value;
  currentMax = v === "" ? null : Number(v);
  render();
});

$("clear-filter").addEventListener("click", () => {
  $("filter-form").reset();
  currentMax = null;
  render();
});

render();
