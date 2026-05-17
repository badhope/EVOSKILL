/**
 * OnlineCode - 编辑器模块
 * 代码编辑、语法高亮、自动补全等功能
 */

class CodeEditor {
    constructor() {
        this.editor = document.getElementById('code-editor');
        this.init();
    }

    init() {
        this.setupKeyBindings();
        this.setupAutoSave();
    }

    setupKeyBindings() {
        this.editor.addEventListener('keydown', (e) => {
            // Ctrl/Cmd + S 保存
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                this.saveCode();
            }

            // Ctrl/Cmd + Enter 运行
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                window.app.runCode();
            }

            // 自动缩进
            if (e.key === 'Enter') {
                this.handleAutoIndent(e);
            }
        });
    }

    handleAutoIndent(e) {
        const editor = this.editor;
        const cursorPos = editor.selectionStart;
        const textBefore = editor.value.substring(0, cursorPos);
        const currentLine = textBefore.split('\n').pop();
        
        // 计算当前缩进
        const indentMatch = currentLine.match(/^(\s*)/);
        const currentIndent = indentMatch ? indentMatch[1] : '';
        
        // 检查是否需要增加缩进（行尾有 : { [ ( ）
        const shouldIncreaseIndent = /[:{\[(]$/.test(currentLine.trim());
        const extraIndent = shouldIncreaseIndent ? '    ' : '';
        
        e.preventDefault();
        
        const newText = '\n' + currentIndent + extraIndent;
        editor.value = textBefore + newText + editor.value.substring(editor.selectionEnd);
        editor.selectionStart = editor.selectionEnd = cursorPos + newText.length;
    }

    setupAutoSave() {
        let timeout;
        this.editor.addEventListener('input', () => {
            if (document.getElementById('auto-save').checked) {
                clearTimeout(timeout);
                timeout = setTimeout(() => this.saveCode(), 2000);
            }
        });
    }

    saveCode() {
        const code = this.editor.value;
        const lang = window.app.currentLang;
        localStorage.setItem(`onlinecode_${lang}`, code);
        window.app.log('info', '代码已保存到本地');
    }

    loadCode() {
        const lang = window.app.currentLang;
        const saved = localStorage.getItem(`onlinecode_${lang}`);
        if (saved) {
            this.editor.value = saved;
            window.app.log('info', '已加载上次保存的代码');
        }
    }

    clearCode() {
        this.editor.value = '';
    }

    insertText(text) {
        const editor = this.editor;
        const start = editor.selectionStart;
        const end = editor.selectionEnd;
        editor.value = editor.value.substring(0, start) + text + editor.value.substring(end);
        editor.selectionStart = editor.selectionEnd = start + text.length;
        editor.focus();
    }
}

// 初始化编辑器
document.addEventListener('DOMContentLoaded', () => {
    window.codeEditor = new CodeEditor();
});
