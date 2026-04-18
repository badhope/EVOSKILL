"""
🎯 Mission Controller - 任务控制器
负责任务分析、难度评级、现有技能匹配
"""

import re
from typing import Dict, List, Any
from pathlib import Path


class MissionController:
    """任务控制器"""

    TASK_CATEGORIES = {
        "file_management": {
            "keywords": ["文件", "整理", "排序", "重命名", "删除", "备份", "同步", "压缩", "解压", "folder", "file", "organize"],
            "base_difficulty": 3
        },
        "code_development": {
            "keywords": ["代码", "编程", "开发", "调试", "重构", "测试", "函数", "类", "code", "debug", "refactor", "function"],
            "base_difficulty": 7
        },
        "data_processing": {
            "keywords": ["数据", "分析", "统计", "图表", "Excel", "CSV", "JSON", "转换", "提取", "data", "analyze", "process"],
            "base_difficulty": 5
        },
        "gui_automation": {
            "keywords": ["点击", "输入", "窗口", "GUI", "界面", "自动", "点击", "键盘", "鼠标", "automation", "click", "window"],
            "base_difficulty": 6
        },
        "web_operation": {
            "keywords": ["网页", "浏览器", "爬虫", "搜索", "下载", "web", "browser", "scrape", "download"],
            "base_difficulty": 5
        },
        "system_maintenance": {
            "keywords": ["系统", "清理", "优化", "进程", "服务", "安装", "卸载", "system", "clean", "optimize", "process"],
            "base_difficulty": 4
        },
        "content_creation": {
            "keywords": ["写作", "报告", "文档", "总结", "翻译", "创作", "write", "summarize", "translate", "document"],
            "base_difficulty": 4
        },
        "learning_research": {
            "keywords": ["学习", "研究", "教程", "资料", "查询", "research", "learn", "tutorial", "search"],
            "base_difficulty": 5
        }
    }

    def __init__(self, llm: Any = None):
        self.llm = llm
        self.skills_registry = self._load_skills_registry()

    def _load_skills_registry(self) -> Dict:
        """加载技能注册表"""
        skills_path = Path.home() / ".woclaw" / "skills"
        registry = {}

        if skills_path.exists():
            for skill_dir in skills_path.iterdir():
                config_file = skill_dir / "config.json"
                if config_file.exists():
                    try:
                        import json
                        with open(config_file, 'r', encoding='utf-8') as f:
                            config = json.load(f)
                        registry[config.get("name", skill_dir.name)] = config
                    except Exception:
                        pass

        return registry

    async def analyze_task(self, task: str) -> Dict:
        """分析任务"""
        print(f"   🔍 正在分析任务: {task[:50]}...")

        category = self._classify_task_category(task)
        difficulty = self._rate_difficulty(task, category)
        matching_skills, confidence = self._match_existing_skills(task)
        requires_evolution = self._determine_evolution_need(difficulty, confidence)

        result = {
            "task": task,
            "task_type": category,
            "difficulty": difficulty,
            "matching_skills": matching_skills,
            "confidence": confidence,
            "requires_evolution": requires_evolution,
            "can_handle_with_existing": confidence >= 0.7,
            "estimated_steps": max(1, difficulty // 2)
        }

        if self.llm:
            result = await self._enrich_with_llm_analysis(task, result)

        return result

    def _classify_task_category(self, task: str) -> str:
        """分类任务类型"""
        task_lower = task.lower()
        best_category = "general"
        max_matches = 0

        for category, info in self.TASK_CATEGORIES.items():
            matches = sum(1 for kw in info["keywords"] if kw.lower() in task_lower)
            if matches > max_matches:
                max_matches = matches
                best_category = category

        return best_category

    def _rate_difficulty(self, task: str, category: str) -> int:
        """评估任务难度 1-10"""
        base_diff = self.TASK_CATEGORIES.get(category, {}).get("base_difficulty", 5)

        complexity_factors = 0
        task_lower = task.lower()

        if any(w in task_lower for w in ["所有", "全部", "批量", "大量", "thousands", "all"]):
            complexity_factors += 2
        if any(w in task_lower for w in ["自动", "智能", "持续", "监控", "循环", "auto", "monitor"]):
            complexity_factors += 1
        if any(w in task_lower for w in ["复杂", "高级", "深度", "综合", "complex", "advanced"]):
            complexity_factors += 2
        if any(w in task_lower for w in ["完美", "精确", "准确", "100%", "perfect", "accurate"]):
            complexity_factors += 1

        sentence_count = task.count('。') + task.count('.')
        if sentence_count > 3:
            complexity_factors += 1

        difficulty = base_diff + complexity_factors
        return max(1, min(10, difficulty))

    def _match_existing_skills(self, task: str) -> tuple[List[str], float]:
        """匹配现有技能"""
        task_lower = task.lower()
        matching_skills = []
        max_confidence = 0.0

        for skill_name, skill_config in self.skills_registry.items():
            tags = skill_config.get("tags", [])
            desc = skill_config.get("description", "")
            name_match = skill_name.lower() in task_lower
            tag_match = any(tag.lower() in task_lower for tag in tags)
            desc_match = any(w in desc.lower() for w in task_lower.split())

            confidence = 0.0
            if name_match:
                confidence += 0.5
            if tag_match:
                confidence += 0.3
            if desc_match:
                confidence += 0.2

            if confidence > 0.1:
                matching_skills.append({
                    "name": skill_name,
                    "confidence": min(1.0, confidence)
                })
                max_confidence = max(max_confidence, confidence)

        matching_skills.sort(key=lambda x: x["confidence"], reverse=True)
        skill_names = [s["name"] for s in matching_skills[:3]]

        return skill_names, max_confidence

    def _determine_evolution_need(self, difficulty: int, confidence: float) -> bool:
        """判断是否需要进化"""
        if difficulty >= 7:
            return True
        if difficulty >= 5 and confidence < 0.5:
            return True
        if confidence < 0.3:
            return True
        return False

    async def _enrich_with_llm_analysis(self, task: str, result: Dict) -> Dict:
        """使用LLM增强分析"""
        try:
            prompt = f"""
            分析这个任务，提供JSON格式输出：
            任务: {task}

            输出:
            {{
                "complexity_rating": 1-10,
                "requires_new_skill": true/false,
                "key_challenges": ["..."],
                "suggested_approach": "..."
            }}
            """

            if hasattr(self.llm, "complete"):
                llm_response = await self.llm.complete(prompt)
                import json
                try:
                    analysis = json.loads(llm_response)
                    result["llm_analysis"] = analysis
                    result["difficulty"] = (result["difficulty"] + analysis.get("complexity_rating", 5)) // 2
                    if analysis.get("requires_new_skill"):
                        result["requires_evolution"] = True
                except Exception:
                    pass
        except Exception:
            pass

        return result


