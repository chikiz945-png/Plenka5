// Сервис-воркер: всегда пробует взять свежие файлы из сети, без сети отдаёт сохранённые копии.
const C='plenka-v1',FILES=['./','index.html','data.js','manifest.webmanifest','icon-192.png','icon-512.png'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(C).then(c=>c.addAll(FILES)).then(()=>self.skipWaiting()))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!=C).map(x=>caches.delete(x)))).then(()=>self.clients.claim()))});
self.addEventListener('fetch',e=>{
 const r=e.request;
 if(r.method!='GET'||new URL(r.url).origin!=location.origin)return;
 e.respondWith(fetch(r,{cache:'no-cache'}).then(res=>{if(res.ok){const c2=res.clone();caches.open(C).then(c=>c.put(r,c2))}return res}).catch(()=>caches.match(r,{ignoreSearch:true}).then(m=>m||caches.match('index.html'))))});
