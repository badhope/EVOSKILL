/**
 * OnlineCode - 主应用逻辑
 */

class OnlineCode {
    constructor() {
        this.currentLang = 'python';
        this.isRunning = false;
        this.init();
    }

    init() {
        this.bindEvents();
        this.initEditor();
        this.loadRuntime();
    }

    bindEvents() {
        // 语言切换
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchLanguage(e.target.dataset.lang);
            });
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

        // 包管理
        document.getElementById('btn-packages').addEventListener('click', () => {
            document.getElementById('modal-packages').classList.add('active');
        });

        // 设置
        document.getElementById('btn-settings').addEventListener('click', () => {
            document.getElementById('modal-settings').classList.add('active');
        });

        // 关闭弹窗
        document.querySelectorAll('.modal-close').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.target.closest('.modal').classList.remove('active');
            });
        });

        // 点击遮罩关闭
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) modal.classList.remove('active');
            });
        });

        // 字体大小
        const fontSizeInput = document.getElementById('font-size');
        fontSizeInput.addEventListener('input', (e) => {
            const size = e.target.value;
            document.getElementById('font-size-value').textContent = size + 'px';
            document.getElementById('code-editor').style.fontSize = size + 'px';
        });

        // 主题切换
        document.getElementById('theme-select').addEventListener('change', (e) => {
            this.setTheme(e.target.value);
        });
    }

    initEditor() {
        const editor = document.getElementById('code-editor');
        
        // 光标位置更新
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

    updateCursorPos() {
        const editor = document.getElementById('code-editor');
        const text = editor.value.substring(0, editor.selectionStart);
        const lines = text.split('\n');
        const line = lines.length;
        const col = lines[lines.length - 1].length + 1;
        document.getElementById('cursor-pos').textContent = `Ln ${line}, Col ${col}`;
    }

    switchLanguage(lang) {
        this.currentLang = lang;
        
        // 更新按钮状态
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.lang === lang);
        });

        // 更新文件名和状态
        const extensions = {
            python: 'py',
            cpp: 'cpp',
            rust: 'rs',
            go: 'go',
            js: 'js'
        };
        
        const runtimeNames = {
            python: 'Python 3.11 (Pyodide)',
            cpp: 'C/C++ (WebAssembly)',
            rust: 'Rust (WebAssembly)',
            go: 'Go (WebAssembly)',
            js: 'JavaScript (V8)'
        };

        document.querySelector('.filename').textContent = `main.${extensions[lang]}`;
        document.getElementById('lang-status').textContent = runtimeNames[lang];

        // 更新编辑器placeholder
        const placeholders = {
            python: "# 在这里输入Python代码...\nprint('Hello, World!')",
            cpp: "// 在这里输入C++代码...\n#include <iostream>\n\nint main() {\n    std::cout << \"Hello, World!\" << std::endl;\n    return 0;\n}",
            rust: "// 在这里输入Rust代码...\nfn main() {\n    println!(\"Hello, World!\");\n}",
            go: "// 在这里输入Go代码...\npackage main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello, World!\")\n}",
            js: "// 在这里输入JavaScript代码...\nconsole.log('Hello, World!');"
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

    async loadRuntime() {
        const loading = document.getElementById('loading-overlay');
        loading.classList.add('active');

        // 模拟加载Pyodide
        setTimeout(() => {
            document.getElementById('runtime-status').textContent = 'Pyodide 已就绪';
            loading.classList.remove('active');
        }, 2000);
    }

    async runCode() {
        if (this.isRunning) return;
        
        this.isRunning = true;
        document.getElementById('btn-run').disabled = true;
        document.getElementById('btn-stop').disabled = false;
        document.getElementById('status').textContent = '运行中...';

        const code = document.getElementById('code-editor').value;
        const consoleOutput = document.getElementById('console-output');
        
        // 清空控制台
        consoleOutput.innerHTML = '';
        this.log('info', `正在运行 ${this.currentLang.toUpperCase()} 代码...`);

        // 模拟执行
        setTimeout(() => {
            if (this.currentLang === 'python') {
                this.log('output', 'Hello, World!');
            } else if (this.currentLang === 'js') {
                this.log('output', 'Hello, World!');
            } else {
                this.log('info', `${this.currentLang.toUpperCase()} 编译器正在开发中...`);
            }
            
            this.stopCode();
        }, 1000);
    }

    stopCode() {
        this.isRunning = false;
        document.getElementById('btn-run').disabled = false;
        document.getElementById('btn-stop').disabled = true;
        document.getElementById('status').textContent = '就绪';
    }

    log(type, message) {
        const consoleOutput = document.getElementById('console-output');
        const line = document.createElement('div');
        line.className = `console-line console-${type}`;
        line.textContent = message;
        consoleOutput.appendChild(line);
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }

    setTheme(theme) {
        document.body.setAttribute('data-theme', theme);
    }
}

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    window.app = new OnlineCode();
});

// 注册Service Worker (PWA)
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js')
        .then(reg => console.log('Service Worker registered'))
        .catch(err => console.log('Service Worker registration failed'));
}
