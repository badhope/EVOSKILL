/**
 * OnlineCode - 极简版主逻辑
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
        this.loadRuntime();
    }

    bindEvents() {
        // 语言切换
        document.getElementById('lang-picker').addEventListener('change', e => this.switchLang(e.target.value));

        // 输出面板
        document.getElementById('tb-output').addEventListener('click', () => this.toggleOutput());
        document.getElementById('output-close').addEventListener('click', () => this.closeOutput());
        document.querySelectorAll('.otab').forEach(b => b.addEventListener('click', e => {
            document.querySelectorAll('.otab').forEach(t => t.classList.remove('active'));
            e.target.classList.add('active');
            document.querySelectorAll('.opanel').forEach(p => p.classList.remove('active'));
            document.getElementById('panel-' + e.target.dataset.tab).classList.add('active');
        }));

        // 运行/停止
        document.getElementById('fab-run').addEventListener('click', () => this.runCode());
        document.getElementById('fab-stop').addEventListener('click', () => this.stopCode());

        // 菜单
        document.getElementById('tb-menu').addEventListener('click', () => this.openMenu());
        document.getElementById('menu-overlay').addEventListener('click', () => this.closeAll());

        // 菜单项
        document.getElementById('mi-save').addEventListener('click', () => { this.saveCode(); this.closeAll(); });
        document.getElementById('mi-load').addEventListener('click', () => { this.loadCode(); this.closeAll(); });
        document.getElementById('mi-share').addEventListener('click', () => { this.shareCode(); this.closeAll(); });
        document.getElementById('mi-clear').addEventListener('click', () => { document.getElementById('code-editor').value = ''; this.closeAll(); });
        document.getElementById('mi-fullscreen').addEventListener('click', () => { this.toggleFullscreen(); this.closeAll(); });

        // 子面板
        document.getElementById('mi-packages').addEventListener('click', () => this.openSub('sub-packages'));
        document.getElementById('mi-settings').addEventListener('click', () => this.openSub('sub-settings'));
        document.getElementById('pkg-back').addEventListener('click', () => this.closeSub('sub-packages'));
        document.getElementById('settings-back').addEventListener('click', () => this.closeSub('sub-settings'));

        // 设置
        document.getElementById('font-dec').addEventListener('click', () => this.setFont(this.fontSize - 1));
        document.getElementById('font-inc').addEventListener('click', () => this.setFont(this.fontSize + 1));
        document.getElementById('theme-select').addEventListener('change', e => this.setTheme(e.target.value));

        // Tab键
        const editor = document.getElementById('code-editor');
        editor.addEventListener('keydown', e => {
            if (e.key === 'Tab') {
                e.preventDefault();
                const s = editor.selectionStart, end = editor.selectionEnd;
                editor.value = editor.value.substring(0, s) + '    ' + editor.value.substring(end);
                editor.selectionStart = editor.selectionEnd = s + 4;
            }
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                this.runCode();
            }
        });

        // 自动保存
        let timer;
        editor.addEventListener('input', () => {
            if (document.getElementById('auto-save').checked) {
                clearTimeout(timer);
                timer = setTimeout(() => this.saveCode(true), 2000);
            }
        });
    }

    switchLang(lang) {
        this.currentLang = lang;
        const ph = {
            python: "# 在这里输入代码...\nprint('Hello, OnlineCode!')",
            cpp: "// 在这里输入代码...\n#include <iostream>\n\nint main() {\n    std::cout << \"Hello!\" << std::endl;\n    return 0;\n}",
            rust: "// 在这里输入代码...\nfn main() {\n    println!(\"Hello!\");\n}",
            go: "// 在这里输入代码...\npackage main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello!\")\n}",
            js: "// 在这里输入代码...\nconsole.log('Hello!');"
        };
        document.getElementById('code-editor').placeholder = ph[lang];
    }

    toggleOutput() {
        document.getElementById('output-panel').classList.toggle('open');
    }

    closeOutput() {
        document.getElementById('output-panel').classList.remove('open');
    }

    openMenu() {
        document.getElementById('menu-overlay').classList.add('show');
        document.getElementById('menu-panel').classList.add('open');
    }

    openSub(id) {
        document.getElementById(id).classList.add('open');
    }

    closeSub(id) {
        document.getElementById(id).classList.remove('open');
    }

    closeAll() {
        document.getElementById('menu-overlay').classList.remove('show');
        document.getElementById('menu-panel').classList.remove('open');
        document.querySelectorAll('.sub-panel').forEach(p => p.classList.remove('open'));
    }

    showToast(text, running = false) {
        const toast = document.getElementById('run-toast');
        const dot = toast.querySelector('.toast-dot');
        document.getElementById('toast-text').textContent = text;
        dot.classList.toggle('run', running);
        toast.classList.add('show');
        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => toast.classList.remove('show'), running ? 99999 : 3000);
    }

    async runCode() {
        if (this.isRunning) return;
        const code = document.getElementById('code-editor').value.trim();
        if (!code) {
            this.showToast('请先输入代码');
            return;
        }

        this.isRunning = true;
        document.getElementById('fab-run').style.display = 'none';
        document.getElementById('fab-stop').style.display = 'flex';
        this.showToast('运行中...', true);

        // 打开输出面板
        document.getElementById('output-panel').classList.add('open');
        const con = document.getElementById('console-output');
        con.innerHTML = '';
        this.log('info', `>>> ${this.currentLang.toUpperCase()} 运行中...`);

        const startTime = performance.now();

        try {
            await window.compiler.run(
                this.currentLang,
                code,
                // onOutput
                (text) => {
                    text.split('\n').forEach(line => {
                        if (line) this.log('out', line);
                    });
                },
                // onError
                (text) => {
                    text.split('\n').forEach(line => {
                        if (line) this.log('err', line);
                    });
                }
            );
        } catch (e) {
            this.log('err', e.message || String(e));
        }

        const elapsed = ((performance.now() - startTime) / 1000).toFixed(2);
        this.log('info', `✓ 完成 (${elapsed}s)`);
        this.stopCode();
    }

    stopCode() {
        this.isRunning = false;
        document.getElementById('fab-run').style.display = 'flex';
        document.getElementById('fab-stop').style.display = 'none';
        this.showToast('就绪');
    }

    log(type, msg) {
        const con = document.getElementById('console-output');
        const cls = { info: 'c-info', out: 'c-out', err: 'c-err', warn: 'c-warn' }[type] || 'c-info';
        const div = document.createElement('div');
        div.className = 'console-line ' + cls;
        div.textContent = msg;
        con.appendChild(div);
        con.scrollTop = con.scrollHeight;
    }

    saveCode(silent = false) {
        const code = document.getElementById('code-editor').value;
        localStorage.setItem('oc_' + this.currentLang, code);
        if (!silent) this.showToast('已保存');
    }

    loadCode() {
        const saved = localStorage.getItem('oc_' + this.currentLang);
        if (saved) {
            document.getElementById('code-editor').value = saved;
            this.showToast('已加载');
        }
    }

    shareCode() {
        const code = document.getElementById('code-editor').value;
        if (navigator.share) {
            navigator.share({ title: 'OnlineCode', text: code }).catch(() => {});
        } else {
            navigator.clipboard.writeText(code).then(() => this.showToast('已复制'));
        }
    }

    setFont(size) {
        this.fontSize = Math.max(10, Math.min(24, size));
        document.getElementById('code-editor').style.fontSize = this.fontSize + 'px';
        document.getElementById('font-val').textContent = this.fontSize;
    }

    setTheme(t) {
        const d = t === 'light';
        const r = document.documentElement.style;
        r.setProperty('--bg', d ? '#fff' : '#0d1117');
        r.setProperty('--bg2', d ? '#f6f8fa' : '#161b22');
        r.setProperty('--bg3', d ? '#eaeef2' : '#21262d');
        r.setProperty('--bg4', d ? '#f0f3f6' : '#1c2128');
        r.setProperty('--t1', d ? '#1f2328' : '#e6edf3');
        r.setProperty('--t2', d ? '#656d76' : '#8b949e');
        r.setProperty('--t3', d ? '#8b949e' : '#484f58');
        r.setProperty('--border', d ? '#d0d7de' : '#30363d');
    }

    toggleFullscreen() {
        if (!document.fullscreenElement) document.documentElement.requestFullscreen().catch(() => {});
        else document.exitFullscreen();
    }

    async loadRuntime() {
        const ld = document.getElementById('loading');
        const loadText = document.getElementById('loading-text');
        ld.classList.add('show');

        try {
            await window.compiler.initPython();
            loadText.textContent = '加载完成!';
            setTimeout(() => ld.classList.remove('show'), 500);
        } catch (e) {
            loadText.textContent = 'Python 加载失败，JS仍可用';
            setTimeout(() => ld.classList.remove('show'), 2000);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => { window.app = new OnlineCode(); });
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
