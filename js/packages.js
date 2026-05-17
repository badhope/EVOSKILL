/**
 * OnlineCode - 包管理（真实安装）
 */

class PackageManager {
    constructor() {
        this.installed = new Set(JSON.parse(localStorage.getItem('oc_pkgs') || '[]'));
    }

    save() {
        localStorage.setItem('oc_pkgs', JSON.stringify([...this.installed]));
    }

    isInstalled(name) {
        return this.installed.has(name);
    }

    async install(name) {
        if (!window.compiler || !window.compiler.pyodide) {
            throw new Error('Python 环境未加载');
        }
        await window.compiler.pyodide.loadPackage(name);
        this.installed.add(name);
        this.save();
    }

    async search(query) {
        // Pyodide 支持的包列表（常用）
        const all = [
            { name: 'numpy', desc: '科学计算' },
            { name: 'pandas', desc: '数据分析' },
            { name: 'matplotlib', desc: '绘图库' },
            { name: 'requests', desc: 'HTTP请求' },
            { name: 'pillow', desc: '图像处理' },
            { name: 'scipy', desc: '科学计算' },
            { name: 'sympy', desc: '符号数学' },
            { name: 'beautifulsoup4', desc: 'HTML解析' },
            { name: 'regex', desc: '正则表达式' },
            { name: 'pyyaml', desc: 'YAML解析' },
            { name: 'lxml', desc: 'XML处理' },
            { name: 'networkx', desc: '图论算法' },
            { name: 'scikit-learn', desc: '机器学习' },
        ];
        if (!query) return all;
        const q = query.toLowerCase();
        return all.filter(p => p.name.includes(q) || p.desc.includes(q));
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.pkgMgr = new PackageManager();

    const searchBtn = document.getElementById('btn-search-package');
    const searchInput = document.getElementById('package-search');
    const pkgList = document.getElementById('package-list');

    // 初始化列表
    renderList(window.pkgMgr.search(''));

    searchBtn.addEventListener('click', async () => {
        const q = searchInput.value.trim();
        searchBtn.textContent = '搜索中...';
        searchBtn.disabled = true;
        const results = await window.pkgMgr.search(q);
        renderList(results);
        searchBtn.textContent = '搜索';
        searchBtn.disabled = false;
    });

    function renderList(packages) {
        pkgList.innerHTML = packages.map(p => {
            const done = window.pkgMgr.isInstalled(p.name);
            return `<div class="pkg-item">
                <div><b>${p.name}</b><small>${p.desc}</small></div>
                <button class="btn-install${done ? ' done' : ''}" data-name="${p.name}" ${done ? 'disabled' : ''}>${done ? '已安装' : '安装'}</button>
            </div>`;
        }).join('');

        pkgList.querySelectorAll('.btn-install:not(.done)').forEach(btn => {
            btn.addEventListener('click', async () => {
                const name = btn.dataset.name;
                btn.textContent = '安装中...';
                btn.disabled = true;
                try {
                    await window.pkgMgr.install(name);
                    btn.textContent = '已安装';
                    btn.classList.add('done');
                } catch (e) {
                    btn.textContent = '失败';
                    btn.disabled = false;
                }
            });
        });
    }
});
