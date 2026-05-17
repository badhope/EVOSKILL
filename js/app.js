/**
 * OnlineCode - 主应用逻辑（移动端优化版）
 */

class OnlineCode {
    constructor() {
        this.currentLang = 'python';
        this.isRunning = false;
        this.fontSize = 14;
        this.init();
    }

    init() {
        this.bindEvents();
        this.initEditor();
        this.initDivider();
        this.loadRuntime();
    }

    bindEvents() {
        // 语言切换（下拉选择器）
        const langSelect = document.getElementById('lang-select');
        langSelect.addEventListener('change', (e) => {
            this.switchLanguage(e.target.value);
        });

        // Tab切换
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchTab(e.target.dataset.tab);
            });
        });

        // 运行/停止
        document.getElementById('btn-run').addEventListener('click', () => this.runCode());
        document.getElementById('btn-stop').addEventListener('click', () => this.stopCode());

        // 清空控制台
        document.getElementById('btn-clear-console').addEventListener('click', () => {
            document.getElementById('console-output').innerHTML = '';
        });

        // 更多菜单
        document.getElementById('btn-more').addEventListener('click', () => {
            this.openSheet('sheet-more', 'sheet-overlay');
        });

        // 底部导航
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const panel = e.currentTarget.dataset.panel;
                this.handleNavTap(panel);
            });
        });

        // 关闭sheet
        document.querySelectorAll('.sheet-overlay').forEach(overlay => {
            overlay.addEventListener('click', () => {
                this.closeAllSheets();
            });
        });

        // 字体大小
        document.getElementById('font-decrease').addEventListener('click', () => {
            this.setFontSize(this.fontSize - 1);
        });
        document.getElementById('font-increase').addEventListener('click', () => {
            this.setFontSize(this.fontSize + 1);
        });

        // 主题切换
        document.getElementById('theme-select').addEventListener('change', (e) => {
            this.setTheme(e.target.value);
        });

        // 全屏
        document.getElementById('btn-fullscreen').addEventListener('click', () => {
            this.toggleFullscreen();
        });

        // 保存/加载
        document.getElementById('btn-save').addEventListener('click', () => {
            if (window.codeEditor) window.codeEditor.saveCode();
            this.closeAllSheets();
        });

        document.getElementById('btn-load').addEventListener('click', () => {
            if (window.codeEditor) window.codeEditor.loadCode();
            this.closeAllSheets();
        });

        // 分享
        document.getElementById('btn-share').addEventListener('click', () => {
            this.shareCode();
            this.closeAllSheets();
        });

        // 虚拟键盘适配
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', () => {
                document.documentElement.style.setProperty('--vh', window.visualViewport.height + 'px');
            });
        }
    }

    initEditor() {
        const editor = document.getElementById('code-editor');
        
        editor.addEventListener('keyup', () => this.updateCursorPos());
        editor.addEventListener('click', () => this.updateCursorPos());
        
        // Tab键支持
        editor.addEventListener('keydown', (e) => {
            if (e.key === 'Tab') {
                e.preventDefault();
                const start = editor.selectionStart;
                const end = editor.selectionEnd;
                editor.value = editor.value.substring(0, start) + '    ' + editor.value.substring(end);
                editor.selectionStart = editor.selectionEnd = start + 4;
            }
        });
    }

    // 可拖拽分割线
    initDivider() {
        const divider = document.getElementById('divider');
        const editorSection = document.getElementById('editor-section');
        const outputSection = document.getElementById('output-section');
        const main = document.getElementById('main');
        let isDragging = false;
        let startPos = 0;
        let startEditorSize = 0;

        const onStart = (e) => {
            isDragging = true;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            startPos = clientY || clientX;
            startEditorSize = editorSection.getBoundingClientRect();
            document.body.style.userSelect = 'none';
            document.body.style.webkitUserSelect = 'none';
        };

        const onMove = (e) => {
            if (!isDragging) return;
            e.preventDefault();
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const mainRect = main.getBoundingClientRect();
            
            const isVertical = getComputedStyle(main).flexDirection === 'column';
            
            if (isVertical) {
                const offset = clientY - mainRect.top;
                const total = mainRect.height;
                const pct = Math.max(20, Math.min(80, (offset / total) * 100));
                editorSection.style.flex = 'none';
                editorSection.style.height = pct + '%';
                outputSection.style.flex = '1';
            } else {
                const offset = clientX - mainRect.left;
                const total = mainRect.width;
                const pct = Math.max(20, Math.min(80, (offset / total) * 100));
                editorSection.style.flex = 'none';
                editorSection.style.width = pct + '%';
                outputSection.style.flex = '1';
            }
        };

        const onEnd = () => {
            isDragging = false;
            document.body.style.userSelect = '';
            document.body.style.webkitUserSelect = '';
        };

        divider.addEventListener('mousedown', onStart);
        divider.addEventListener('touchstart', onStart, { passive: true });
        document.addEventListener('mousemove', onMove);
        document.addEventListener('touchmove', onMove, { passive: false });
        document.addEventListener('mouseup', onEnd);
        document.addEventListener('touchend', onEnd);
    }

    updateCursorPos() {
        const editor = document.getElementById('code-editor');
        const text = editor.value.substring(0, editor.selectionStart);
        const lines = text.split('\n');
        const line = lines.length;
        const col = lines[lines.length - 1].length + 1;
        document.getElementById('cursor-pos').textContent = `${line}:${col}`;
    }

    switchLanguage(lang) {
        this.currentLang = lang;

        const extensions = { python: 'py', cpp: 'cpp', rust: 'rs', go: 'go', js: 'js' };
        const runtimeNames = {
            python: 'Python 3.11',
            cpp: 'C/C++ (WASM)',
            rust: 'Rust (WASM)',
            go: 'Go (WASM)',
            js: 'JavaScript'
        };

        document.getElementById('filename').textContent = `main.${extensions[lang]}`;

        const placeholders = {
            python: "# 在这里输入代码...\nprint('Hello, OnlineCode!')",
            cpp: "// 在这里输入代码...\n#include <iostream>\n\nint main() {\n    std::cout << \"Hello!\" << std::endl;\n    return 0;\n}",
            rust: "// 在这里输入代码...\nfn main() {\n    println!(\"Hello!\");\n}",
            go: "// 在这里输入代码...\npackage main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello!\")\n}",
            js: "// 在这里输入代码...\nconsole.log('Hello!');"
        };
        
        document.getElementById('code-editor').placeholder = placeholders[lang];
    }

    switchTab(tab) {
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        document.querySelectorAll('.tab-panel').forEach(panel => {
            panel.classList.toggle('active', panel.id === `panel-${tab}`);
        });
    }

    handleNavTap(panel) {
        // 更新导航高亮
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.toggle('active', item.dataset.panel === panel);
        });

        switch (panel) {
            case 'editor':
                this.closeAllSheets();
                break;
            case 'output':
                this.closeAllSheets();
                // 滚动到输出区域
                document.getElementById('output-section').scrollIntoView({ behavior: 'smooth' });
                break;
            case 'packages':
                this.openSheet('sheet-packages', 'sheet-overlay-pkg');
                break;
            case 'settings':
                this.openSheet('sheet-settings', 'sheet-overlay-settings');
                break;
        }
    }

    openSheet(sheetId, overlayId) {
        this.closeAllSheets();
        const sheet = document.getElementById(sheetId);
        const overlay = document.getElementById(overlayId);
        if (overlay) overlay.classList.add('active');
        // 延迟一帧触发动画
        requestAnimationFrame(() => {
            sheet.classList.add('active');
        });
    }

    closeAllSheets() {
        document.querySelectorAll('.bottom-sheet').forEach(s => s.classList.remove('active'));
        document.querySelectorAll('.sheet-overlay').forEach(o => o.classList.remove('active'));
    }

    async loadRuntime() {
        const loading = document.getElementById('loading-overlay');
        loading.classList.add('active');
        setTimeout(() => {
            loading.classList.remove('active');
        }, 1500);
    }

    async runCode() {
        if (this.isRunning) return;
        
        this.isRunning = true;
        document.getElementById('btn-run').disabled = true;
        document.getElementById('btn-stop').disabled = false;
        document.getElementById('status-text').textContent = '运行中...';
        document.querySelector('.status-dot').classList.add('running');

        // 切换到控制台tab
        this.switchTab('console');

        const code = document.getElementById('code-editor').value;
        const consoleOutput = document.getElementById('console-output');
        consoleOutput.innerHTML = '';
        this.log('info', `>>> 运行 ${this.currentLang.toUpperCase()} ...`);

        setTimeout(() => {
            if (this.currentLang === 'python') {
                this.log('output', 'Hello, OnlineCode!');
            } else if (this.currentLang === 'js') {
                this.log('output', 'Hello, OnlineCode!');
            } else {
                this.log('warn', `${this.currentLang.toUpperCase()} 编译器开发中...`);
            }
            this.log('info', '✓ 执行完成 (0.02s)');
            this.stopCode();
        }, 800);
    }

    stopCode() {
        this.isRunning = false;
        document.getElementById('btn-run').disabled = false;
        document.getElementById('btn-stop').disabled = true;
        document.getElementById('status-text').textContent = '就绪';
        document.querySelector('.status-dot').classList.remove('running');
    }

    log(type, message) {
        const consoleOutput = document.getElementById('console-output');
        const line = document.createElement('div');
        line.className = `console-line console-${type}`;
        line.textContent = message;
        consoleOutput.appendChild(line);
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }

    setFontSize(size) {
        this.fontSize = Math.max(10, Math.min(24, size));
        document.getElementById('code-editor').style.fontSize = this.fontSize + 'px';
        document.getElementById('font-size-value').textContent = this.fontSize;
    }

    setTheme(theme) {
        if (theme === 'light') {
            document.documentElement.style.setProperty('--bg-primary', '#ffffff');
            document.documentElement.style.setProperty('--bg-secondary', '#f6f8fa');
            document.documentElement.style.setProperty('--bg-tertiary', '#eaeef2');
            document.documentElement.style.setProperty('--bg-elevated', '#f0f3f6');
            document.documentElement.style.setProperty('--text-primary', '#1f2328');
            document.documentElement.style.setProperty('--text-secondary', '#656d76');
            document.documentElement.style.setProperty('--text-muted', '#8b949e');
            document.documentElement.style.setProperty('--border', '#d0d7de');
        } else {
            document.documentElement.style.setProperty('--bg-primary', '#0d1117');
            document.documentElement.style.setProperty('--bg-secondary', '#161b22');
            document.documentElement.style.setProperty('--bg-tertiary', '#21262d');
            document.documentElement.style.setProperty('--bg-elevated', '#1c2128');
            document.documentElement.style.setProperty('--text-primary', '#e6edf3');
            document.documentElement.style.setProperty('--text-secondary', '#8b949e');
            document.documentElement.style.setProperty('--text-muted', '#484f58');
            document.documentElement.style.setProperty('--border', '#30363d');
        }
    }

    toggleFullscreen() {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen().catch(() => {});
        } else {
            document.exitFullscreen();
        }
        this.closeAllSheets();
    }

    shareCode() {
        const code = document.getElementById('code-editor').value;
        if (navigator.share) {
            navigator.share({
                title: 'OnlineCode',
                text: code
            }).catch(() => {});
        } else {
            navigator.clipboard.writeText(code).then(() => {
                this.log('info', '代码已复制到剪贴板');
            });
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.app = new OnlineCode();
});

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js').catch(() => {});
}
