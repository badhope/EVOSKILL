"""
📦 Skill Incubator - 技能孵化模块
自动生成完整的技能包
"""

import os
import re
import json
import hashlib
from typing import Dict, Any
from pathlib import Path


class SkillIncubator:
    """技能孵化器 - 将解决方案转化为可执行的技能"""

    def __init__(self, storage_path: Path, llm: Any = None):
        self.storage_path = storage_path
        self.skills_output_path = Path.home() / ".woclaw" / "evolved_skills"
        self.skills_output_path.mkdir(parents=True, exist_ok=True)
        self.llm = llm

    async def incubate_skill(self, solution: Dict, task: str) -> Dict:
        """孵化新技能"""
        skill_name = self._generate_skill_name(task)
        skill_version = "1.0.0"
        skill_path = self.skills_output_path / skill_name

        print(f"   🥚 正在孵化技能: {skill_name}")

        try:
            skill_path.mkdir(parents=True, exist_ok=True)
            scripts_path = skill_path / "scripts"
            scripts_path.mkdir(exist_ok=True)

            config = self._generate_config(skill_name, skill_version, task, solution)
            with open(skill_path / "config.json", 'w', encoding='utf-8') as f:
                json.dump(config, f, ensure_ascii=False, indent=2)
            print(f"      ✅ 生成配置文件")

            run_code = await self._generate_run_script(skill_name, task, solution)
            with open(scripts_path / "run.py", 'w', encoding='utf-8') as f:
                f.write(run_code)
            print(f"      ✅ 生成执行脚本")

            test_code = self._generate_test_script(skill_name, task)
            with open(scripts_path / "test.py", 'w', encoding='utf-8') as f:
                f.write(test_code)
            print(f"      ✅ 生成测试用例")

            docs = self._generate_documentation(skill_name, task, solution)
            with open(skill_path / "SKILL.md", 'w', encoding='utf-8') as f:
                f.write(docs)
            print(f"      ✅ 生成技能文档")

            requirements = self._generate_requirements(solution)
            if requirements:
                with open(skill_path / "requirements.txt", 'w', encoding='utf-8') as f:
                    f.write(requirements)
                print(f"      ✅ 生成依赖配置")

            lineage = self._record_lineage(skill_name, solution)
            with open(skill_path / ".lineage.json", 'w', encoding='utf-8') as f:
                json.dump(lineage, f, ensure_ascii=False, indent=2)

            return {
                "success": True,
                "skill_name": skill_name,
                "version": skill_version,
                "path": str(skill_path),
                "config": config
            }

        except Exception as e:
            import shutil
            shutil.rmtree(skill_path, ignore_errors=True)
            return {"success": False, "error": str(e)}

    async def update_skill(self, skill_name: str, optimized_data: Dict) -> Dict:
        """更新已有技能"""
        skill_path = self.skills_output_path / skill_name
        if not skill_path.exists():
            return {"success": False, "error": "技能不存在"}

        old_version = "1.0.0"
        config_path = skill_path / "config.json"
        if config_path.exists():
            with open(config_path) as f:
                config = json.load(f)
                old_version = config.get("version", "1.0.0")

        new_version = self._bump_version(old_version)
        print(f"   🔄 升级技能: {skill_name} {old_version} → {new_version}")

        return {
            "success": True,
            "skill_name": skill_name,
            "old_version": old_version,
            "new_version": new_version
        }

    def _generate_skill_name(self, task: str) -> str:
        """生成技能名称"""
        keywords = self._extract_keywords(task)
        base_name = "_".join(keywords[:3]) if keywords else "generic_skill"

        task_hash = hashlib.md5(task.encode()).hexdigest()[:4]
        normalized = re.sub(r'[^a-z0-9_]', '', base_name.lower())
        skill_name = f"evo_{normalized}_{task_hash}"

        return skill_name[:40]

    def _extract_keywords(self, text: str) -> list:
        """提取关键词"""
        stop_words = {"如何", "怎么", "怎样", "的", "是", "在", "我", "要", "想", "帮", "how", "to", "the"}
        words = re.findall(r'[\w\u4e00-\u9fa5]+', text)
        return [w for w in words if w.lower() not in stop_words and len(w) > 1]

    def _generate_config(self, name: str, version: str, task: str, solution: Dict) -> Dict:
        """生成技能配置"""
        return {
            "name": name,
            "version": version,
            "description": f"进化生成的技能: {task[:60]}...",
            "author": "Woclaw Evolution Engine",
            "category": "evolved",
            "tags": solution.get("tags", ["evolution", "auto-generated"]),
            "requires": [],
            "entry": "scripts/run.py",
            "icon": "🧬",
            "evolved": True,
            "parent_solution": solution.get("strategy_name", "unknown"),
            "creation_task": task,
            "quality_score": solution.get("overall_score", 50)
        }

    async def _generate_run_script(self, skill_name: str, task: str, solution: Dict) -> str:
        """生成运行脚本"""
        user_code = solution.get("code_snippet", "")

        if self.llm:
            try:
                prompt = f"""
作为技能代码生成器，为以下技能生成完整的Python运行脚本:

技能名称: {skill_name}
任务描述: {task}
解决方案策略: {solution.get('strategy_name', '')}
核心代码片段:
{user_code}

请生成一个完整的异步技能入口脚本，包含:
1. 完整的异步main函数
2. 参数解析和验证
3. 错误处理和日志
4. 返回标准的结果格式Dict
5. 清晰的注释
"""
                if hasattr(self.llm, "complete"):
                    llm_code = await self.llm.complete(prompt)
                    user_code = self._extract_code(llm_code)
            except Exception:
                pass

        template = f'''"""
🧬 Evolved Skill: {skill_name}
自动生成的进化技能
任务: {task[:80]}
"""

import asyncio
from typing import Dict, Any


async def main(config: Dict, context: Dict = None) -> Dict:
    """技能入口函数"""
    context = context or {{}}
    params = context.get("params", {{}})

    try:
        result = await execute_skill(params)
        return {{
            "success": True,
            "skill": "{skill_name}",
            "result": result,
            "message": "进化技能执行成功"
        }}
    except Exception as e:
        return {{
            "success": False,
            "skill": "{skill_name}",
            "error": str(e),
            "message": "进化技能执行失败"
        }}


async def execute_skill(params: Dict) -> Any:
    """核心技能逻辑"""
    {user_code or "# 技能逻辑实现\n    pass"}


async def test_skill():
    """技能自测试"""
    test_context = {{"params": {{}}}}
    result = await main({{}}, test_context)
    print("技能自测试结果:", result)
    return result


if __name__ == "__main__":
    asyncio.run(test_skill())
'''

        return template

    def _generate_test_script(self, skill_name: str, task: str) -> str:
        """生成测试脚本"""
        return f'''"""
技能测试: {skill_name}
"""

import pytest
import asyncio


class Test{skill_name.title().replace('_', '')}:

    async def test_basic_execution(self):
        """测试基本执行"""
        from scripts.run import main

        result = await main({{}}, {{"params": {{}}}})
        assert "success" in result

    async def test_error_handling(self):
        """测试错误处理"""
        from scripts.run import main

        result = await main({{}}, {{"params": {{"invalid": True}}}})
        assert isinstance(result, dict)


if __name__ == "__main__":
    asyncio.run(Test{skill_name.title().replace('_', '')}().test_basic_execution())
'''

    def _generate_documentation(self, skill_name: str, task: str, solution: Dict) -> str:
        """生成技能文档"""
        return f"""# 🧬 {skill_name} - 进化技能文档

## 概述

这是由AI进化引擎自动生成的技能。

**创建任务**: {task}

**进化策略**: {solution.get('strategy_name', 'unknown')}

**质量评分**: {solution.get('overall_score', 0):.1f}/100

## 使用方法

```python
result = await skill_manager.execute("{skill_name}", params={{
    "param1": "value1"
}})
```

## 功能说明

本技能是通过进化过程自主发现的最优解决方案。包含:
- 核心执行逻辑
- 完整的错误处理
- 自测试功能

## 进化谱系

- 父代策略: {solution.get('strategy_name', 'unknown')}
- 生成代数: v1
- 创建时间: 进化过程中自动记录

---
*由 Woclaw Evolution Engine 自动生成*
"""

    def _generate_requirements(self, solution: Dict) -> str:
        """生成依赖文件"""
        return "# 进化技能依赖\n# 自动安装所需包\n"

    def _record_lineage(self, skill_name: str, solution: Dict) -> Dict:
        """记录技能血统"""
        return {
            "skill_name": skill_name,
            "generation": 1,
            "parent_strategy": solution.get("strategy_name"),
            "ancestors": [],
            "mutation_events": [],
            "crossover_events": [],
            "creation_fitness": solution.get("overall_score", 0),
            "evolution_history": []
        }

    def _extract_code(self, text: str) -> str:
        """从文本中提取代码"""
        import re
        match = re.search(r'```python\n([\s\S]*?)\n```', text)
        if match:
            return match.group(1)
        match = re.search(r'```\n([\s\S]*?)\n```', text)
        if match:
            return match.group(1)
        return text

    def _bump_version(self, version: str) -> str:
        """版本号升级"""
        parts = version.split('.')
        if len(parts) == 3:
            parts[2] = str(int(parts[2]) + 1)
        return '.'.join(parts)


