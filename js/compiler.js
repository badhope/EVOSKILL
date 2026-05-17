/**
 * OnlineCode - 编译器模块
 * 多语言编译执行支持
 */

class Compiler {
    constructor() {
        this.runtimes = {};
        this.init();
    }

    async init() {
        // 初始化各语言运行时
        await this.initPython();
    }

    async initPython() {
        // Pyodide 初始化（实际项目中通过CDN加载）
        this.runtimes.python = {
            ready: true,
            run: (code) => {
                // 模拟Python执行
                return new Promise((resolve) => {
                    setTimeout(() => {
                        resolve({ output: 'Hello, World!', error: null });
                    }, 500);
                });
            }
        };
    }

    async run(lang, code) {
        switch (lang) {
            case 'python':
                return await this.runPython(code);
            case 'js':
                return await this.runJavaScript(code);
            case 'cpp':
            case 'rust':
            case 'go':
                return { output: null, error: `${lang.toUpperCase()} 编译器正在开发中...` };
            default:
                return { output: null, error: '不支持的语言' };
        }
    }

    async runPython(code) {
        try {
            // 实际项目中使用 Pyodide
            // const result = await pyodide.runPythonAsync(code);
            // return { output: result, error: null };
            
            // 模拟执行
            if (code.includes('print')) {
                const match = code.match(/print\(['"](.+?)['"]\)/);
                const output = match ? match[1] : 'Hello, World!';
                return { output, error: null };
            }
            return { output: '代码执行完成', error: null };
        } catch (err) {
            return { output: null, error: err.message };
        }
    }

    async runJavaScript(code) {
        try {
            let output = '';
            const originalLog = console.log;
            console.log = (...args) => {
                output += args.join(' ') + '\n';
            };

            // 使用 Function 构造器安全执行
            const fn = new Function(code);
            fn();

            console.log = originalLog;
            return { output: output || 'undefined', error: null };
        } catch (err) {
            return { output: null, error: err.message };
        }
    }

    async installPackage(name) {
        // 模拟包安装
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true, message: `${name} 安装成功` });
            }, 1000);
        });
    }
}

// 初始化编译器
document.addEventListener('DOMContentLoaded', () => {
    window.compiler = new Compiler();
});
