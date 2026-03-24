// Service Worker — cache-first for static assets, network-first for per-class detail JSON
const CACHE_NAME = 'pz-api-v1';

const PRECACHE_URLS = [
  './',
  './index.html',
  './app.css',
  './js/state.js',
  './js/utils.js',
  './js/search-index.js',
  './js/source-viewer.js',
  './js/class-list.js',
  './js/class-detail.js',
  './js/globals.js',
  './js/app.js',
  './lua_api_index.json'
];

// Install: pre-cache static shell
self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(CACHE_NAME).then(function(cache) {
      return cache.addAll(PRECACHE_URLS);
    }).then(function() {
      return self.skipWaiting();
    })
  );
});

// Activate: purge old caches
self.addEventListener('activate', function(event) {
  event.waitUntil(
    caches.keys().then(function(names) {
      return Promise.all(
        names
          .filter(function(name) { return name !== CACHE_NAME; })
          .map(function(name) { return caches.delete(name); })
      );
    }).then(function() {
      return self.clients.claim();
    })
  );
});

// Fetch: network-first for detail JSON, cache-first for everything else
self.addEventListener('fetch', function(event) {
  var url = new URL(event.request.url);

  // Network-first for per-class detail files
  if (url.pathname.indexOf('/lua_api_detail/') !== -1) {
    event.respondWith(
      fetch(event.request).then(function(response) {
        if (response.ok) {
          var clone = response.clone();
          caches.open(CACHE_NAME).then(function(cache) {
            cache.put(event.request, clone);
          });
        }
        return response;
      }).catch(function() {
        return caches.match(event.request);
      })
    );
    return;
  }

  // Cache-first for all other requests
  event.respondWith(
    caches.match(event.request).then(function(cached) {
      if (cached) {
        return cached;
      }
      return fetch(event.request).then(function(response) {
        if (response.ok && url.origin === self.location.origin) {
          var clone = response.clone();
          caches.open(CACHE_NAME).then(function(cache) {
            cache.put(event.request, clone);
          });
        }
        return response;
      });
    })
  );
});
