/**
 * OnlineCode - 真正的代码编译执行引擎
 */

class Compiler {
    constructor() {
        this.pyodide = null;
        this.ready = false;
        this.loading = false;
    }

    async initPython() {
        if (this.pyodide) return;
        this.loading = true;
        const loadText = document.getElementById('loading-text');
        try {
            loadText.textContent = '正在加载 Python 运行环境...';
            this.pyodide = await loadPyodide({
                indexURL: 'https://cdn.jsdelivr.net/pyodide/v0.24.1/full/'
            });
            this.ready = true;
            this.loading = false;
        } catch (e) {
            loadText.textContent = 'Python 环境加载失败，请刷新重试';
            this.loading = false;
            throw e;
        }
    }

    async run(lang, code, onOutput, onError) {
        switch (lang) {
            case 'python': return await this.runPython(code, onOutput, onError);
            case 'js': return this.runJS(code, onOutput, onError);
            default:
                onError(`${lang.toUpperCase()} 编译器正在开发中，目前支持 Python 和 JavaScript`);
                return;
        }
    }

    async runPython(code, onOutput, onError) {
        if (!this.pyodide) {
            onError('Python 环境尚未加载完成，请稍候...');
            return;
        }

        try {
            // 重定向 stdout 和 stderr
            this.pyodide.runPython(`
import sys
from io import StringIO
sys.stdout = StringIO()
sys.stderr = StringIO()
`);

            // 执行用户代码
            const result = this.pyodide.runPython(code);

            // 获取输出
            const stdout = this.pyodide.runPython('sys.stdout.getvalue()');
            const stderr = this.pyodide.runPython('sys.stderr.getvalue()');

            if (stdout) onOutput(stdout);
            if (stderr) onError(stderr);
            if (result !== undefined && result !== null && !stdout) {
                onOutput(String(result));
            }
        } catch (e) {
            // 解析错误信息，提取行号
            let errMsg = e.message;
            if (errMsg.includes('PythonError')) {
                errMsg = errMsg.split('\n').slice(1).join('\n').trim();
            }
            onError(errMsg);
        } finally {
            // 恢复 stdout/stderr
            try {
                this.pyodide.runPython(`
import sys
sys.stdout = sys.__stdout__
sys.stderr = sys.__stderr__
`);
            } catch (e) {}
        }
    }

    runJS(code, onOutput, onError) {
        try {
            // 劫持 console.log / console.error / console.warn
            const origLog = console.log;
            const origError = console.error;
            const origWarn = console.warn;

            const outputs = [];

            console.log = (...args) => {
                outputs.push({ type: 'out', text: args.map(a => {
                    if (typeof a === 'object') return JSON.stringify(a, null, 2);
                    return String(a);
                }).join(' ') });
            };
            console.error = (...args) => {
                outputs.push({ type: 'err', text: args.map(a => String(a)).join(' ') });
            };
            console.warn = (...args) => {
                outputs.push({ type: 'warn', text: args.map(a => String(a)).join(' ') });
            };

            // 执行代码
            const fn = new Function(code);
            fn();

            // 恢复
            console.log = origLog;
            console.error = origError;
            console.warn = origWarn;

            // 输出结果
            if (outputs.length === 0) {
                onOutput('(代码执行完成，无输出)');
            } else {
                outputs.forEach(o => {
                    if (o.type === 'err') onError(o.text);
                    else if (o.type === 'warn') onOutput('[警告] ' + o.text);
                    else onOutput(o.text);
                });
            }
        } catch (e) {
            console.log = console.log; // 恢复
            // 提取错误行号
            let errMsg = e.message || String(e);
            if (e instanceof SyntaxError) {
                errMsg = `语法错误: ${errMsg}`;
            }
            onError(errMsg);
        }
    }

    async installPackage(name) {
        if (!this.pyodide) {
            throw new Error('Python 环境尚未加载');
        }
        await this.pyodide.loadPackage(name);
        return true;
    }
}

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    window.compiler = new Compiler();
});
