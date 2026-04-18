"""
🔍 Discovery Module - 技能发现模块
自主探索新任务的解决方案
"""

import json
import random
from typing import Dict, List, Any
from pathlib import Path


class DiscoveryModule:
    """技能发现模块"""

    STRATEGY_TYPES = [
        "direct_approach",
        "modular_decomposition",
        "analogical_reasoning",
        "first_principles",
        "iterative_refinement",
        "constraint_oriented"
    ]

    def __init__(self, llm: Any = None):
        self.llm = llm

    async def discover_solutions(self, task: str, num_attempts: int = 3) -> List[Dict]:
        """发现多个解决方案"""
        solutions = []

        for i in range(num_attempts):
            strategy = self.STRATEGY_TYPES[i % len(self.STRATEGY_TYPES)]
            print(f"   🧪 方案 {i+1}/{num_attempts}: {strategy}")

            solution = await self._generate_solution(task, strategy, i)
            solutions.append(solution)

        return solutions

    async def _generate_solution(self, task: str, strategy: str, attempt: int) -> Dict:
        """生成单个解决方案"""
        strategy_description = self._describe_strategy(strategy)

        solution = {
            "strategy_name": strategy,
            "strategy_description": strategy_description,
            "attempt_number": attempt,
            "task": task
        }

        if self.llm:
            solution = await self._generate_with_llm(task, strategy, solution)
        else:
            solution = self._generate_fallback(task, strategy, solution)

        return solution

    def _describe_strategy(self, strategy: str) -> str:
        """描述策略类型"""
        descriptions = {
            "direct_approach": "直接法 - 针对问题核心，一步到位解决",
            "modular_decomposition": "模块化分解 - 将复杂问题拆分为可管理的子问题",
            "analogical_reasoning": "类比推理 - 借鉴类似问题的解决方案",
            "first_principles": "第一性原理 - 从基础规则重新构建解决方案",
            "iterative_refinement": "迭代求精 - 先生成最简版本，再逐步完善",
            "constraint_oriented": "约束导向 - 先识别限制条件，再设计可行方案"
        }
        return descriptions.get(strategy, "通用策略")

    async def _generate_with_llm(self, task: str, strategy: str, solution: Dict) -> Dict:
        """使用LLM生成解决方案"""
        try:
            prompt = f"""
作为AI进化系统，针对以下任务和策略生成完整的解决方案:

任务: {task}
策略: {strategy} - {self._describe_strategy(strategy)}

请输出JSON格式的解决方案:
{{
    "implementation_plan": "详细的执行步骤描述",
    "core_algorithm": "核心算法/逻辑的伪代码",
    "required_tools": ["需要的工具/能力列表"],
    "code_snippet": "关键代码片段(Python)",
    "test_cases": ["测试用例"],
    "edge_cases": ["需要处理的边界情况"],
    "expected_success_probability": 0.0-1.0,
    "estimated_complexity": "low/medium/high"
}}
"""

            if hasattr(self.llm, "complete"):
                response = await self.llm.complete(prompt)
                try:
                    parsed = self._extract_json(response)
                    solution.update(parsed)
                    solution["generated_by_llm"] = True
                except Exception as e:
                    print(f"      ⚠️  LLM解析失败: {e}")
                    solution = self._generate_fallback(task, strategy, solution)
        except Exception as e:
            print(f"      ⚠️  LLM调用失败: {e}")
            solution = self._generate_fallback(task, strategy, solution)

        return solution

    def _generate_fallback(self, task: str, strategy: str, solution: Dict) -> Dict:
        """生成备选解决方案（无LLM时）"""
        solution.update({
            "implementation_plan": f"使用{strategy}策略处理任务: {task}",
            "core_algorithm": f"采用{strategy}设计模式",
            "required_tools": ["file_operations", "shell_execution", "code_generation"],
            "code_snippet": self._generate_template_code(task),
            "test_cases": [f"测试基本功能 - {task[:30]}"],
            "edge_cases": ["错误处理", "空输入", "异常情况"],
            "expected_success_probability": 0.6 + random.random() * 0.2,
            "estimated_complexity": "medium",
            "generated_by_llm": False
        })
        return solution

    def _generate_template_code(self, task: str) -> str:
        """生成模板代码"""
        task_lower = task.lower()

        if "文件" in task_lower or "file" in task_lower:
            return '''
import os
from pathlib import Path

def process_files(target_path):
    path = Path(target_path)
    for item in path.rglob("*"):
        if item.is_file():
            # 处理文件
            pass
'''
        elif "代码" in task_lower or "code" in task_lower:
            return '''
import ast
import subprocess

def analyze_code(file_path):
    with open(file_path) as f:
        tree = ast.parse(f.read())
    # 静态分析
    pass
'''
        else:
            return '''
async def execute_task(params):
    # 通用任务处理
    result = await process_with_worker(params)
    return result
'''

    def _extract_json(self, text: str) -> Dict:
        """从文本中提取JSON"""
        import re

        match = re.search(r'\{[\s\S]*\}', text)
        if match:
            return json.loads(match.group(0))
        else:
            start = text.find('{')
            end = text.rfind('}') + 1
            if start >= 0 and end > start:
                return json.loads(text[start:end])
        raise ValueError("无法提取JSON")


