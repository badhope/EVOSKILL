/**
 * OnlineCode - 包管理模块
 */

class PackageManager {
    constructor() {
        this.installedPackages = new Set();
        this.loadInstalledPackages();
    }

    loadInstalledPackages() {
        const saved = localStorage.getItem('onlinecode_packages');
        if (saved) {
            this.installedPackages = new Set(JSON.parse(saved));
        }
    }

    saveInstalledPackages() {
        localStorage.setItem('onlinecode_packages', JSON.stringify([...this.installedPackages]));
    }

    async searchPackage(name) {
        // 模拟搜索PyPI
        const mockPackages = [
            { name: 'numpy', version: '1.24.0', description: '科学计算库' },
            { name: 'pandas', version: '2.0.0', description: '数据分析库' },
            { name: 'matplotlib', version: '3.7.0', description: '绘图库' },
            { name: 'requests', version: '2.28.0', description: 'HTTP请求库' },
            { name: 'pillow', version: '9.5.0', description: '图像处理库' }
        ];

        return mockPackages.filter(pkg => 
            pkg.name.toLowerCase().includes(name.toLowerCase())
        );
    }

    async installPackage(name) {
        if (this.installedPackages.has(name)) {
            return { success: false, message: `${name} 已安装` };
        }

        // 模拟安装过程
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        this.installedPackages.add(name);
        this.saveInstalledPackages();
        
        return { success: true, message: `${name} 安装成功` };
    }

    isInstalled(name) {
        return this.installedPackages.has(name);
    }

    getInstalledPackages() {
        return [...this.installedPackages];
    }
}

// 初始化包管理器
document.addEventListener('DOMContentLoaded', () => {
    window.packageManager = new PackageManager();

    // 绑定包搜索
    const searchBtn = document.getElementById('btn-search-package');
    const searchInput = document.getElementById('package-search');
    const packageList = document.getElementById('package-list');

    searchBtn.addEventListener('click', async () => {
        const query = searchInput.value.trim();
        if (!query) return;

        searchBtn.textContent = '搜索中...';
        searchBtn.disabled = true;

        const results = await window.packageManager.searchPackage(query);
        
        packageList.innerHTML = results.map(pkg => `
            <div class="package-item">
                <span class="package-name">${pkg.name}</span>
                <span class="package-version">${pkg.version}</span>
                <button class="btn-small btn-install" data-name="${pkg.name}">
                    ${window.packageManager.isInstalled(pkg.name) ? '已安装' : '安装'}
                </button>
            </div>
        `).join('');

        // 绑定安装按钮
        packageList.querySelectorAll('.btn-install').forEach(btn => {
            btn.addEventListener('click', async () => {
                const name = btn.dataset.name;
                btn.textContent = '安装中...';
                btn.disabled = true;

                const result = await window.packageManager.installPackage(name);
                
                btn.textContent = result.success ? '已安装' : '安装';
                btn.disabled = result.success;
                
                window.app.log('info', result.message);
            });
        });

        searchBtn.textContent = '搜索';
        searchBtn.disabled = false;
    });
});
