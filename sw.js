/**
 * OnlineCode - Service Worker
 * 实现离线缓存和PWA功能
 */

const CACHE_NAME = 'onlinecode-v1';
const STATIC_ASSETS = [
  '/OnlineCode/',
  '/OnlineCode/index.html',
  '/OnlineCode/styles/main.css',
  '/OnlineCode/js/app.js',
  '/OnlineCode/js/editor.js',
  '/OnlineCode/js/compiler.js',
  '/OnlineCode/js/packages.js',
  '/OnlineCode/manifest.json'
];

// 安装时缓存静态资源
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Service Worker: 缓存静态资源');
        return cache.addAll(STATIC_ASSETS);
      })
      .catch((err) => {
        console.log('Service Worker: 缓存失败', err);
      })
  );
  self.skipWaiting();
});

// 激活时清理旧缓存
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => name !== CACHE_NAME)
          .map((name) => caches.delete(name))
      );
    })
  );
  self.clients.claim();
});

// 拦截网络请求
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        // 缓存命中则返回缓存
        if (response) {
          return response;
        }

        // 否则发起网络请求
        return fetch(event.request)
          .then((networkResponse) => {
            // 只缓存GET请求
            if (event.request.method !== 'GET') {
              return networkResponse;
            }

            // 克隆响应（因为response只能读取一次）
            const responseToCache = networkResponse.clone();

            caches.open(CACHE_NAME)
              .then((cache) => {
                cache.put(event.request, responseToCache);
              });

            return networkResponse;
          })
          .catch(() => {
            // 网络失败时返回离线页面
            if (event.request.mode === 'navigate') {
              return caches.match('/OnlineCode/');
            }
          });
      })
  );
});
