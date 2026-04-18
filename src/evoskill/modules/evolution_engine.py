"""
🧬 Evolution Engine - AI进化核心引擎
整合: 技能发现 → 评级 → 孵化 → 优化 → 物种进化
"""

import os
import json
import time
import random
import hashlib
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, field, asdict
from datetime import datetime

from .mission_controller import MissionController
from .discovery_module import DiscoveryModule
from .trueskill_rater import TrueSkillRater
from .skill_incubator import SkillIncubator
from .textgrad_optimizer import TextGradOptimizer
from .speciation_evolution import SpeciationEvolution
from .evolution_memory import EvolutionMemory


@dataclass
class EvolutionContext:
    """进化上下文"""
    task: str
    task_id: str
    start_time: float
    mode: str = "auto"
    stage: str = "init"
    discovered_solutions: List[Dict] = field(default_factory=list)
    best_solution: Optional[Dict] = None
    new_skill_name: Optional[str] = None
    optimization_rounds: int = 0
    success: bool = False
    message: str = ""
    evolved_skills_count: int = 0


class EvolutionEngine:
    """AI进化核心引擎"""

    def __init__(
        self,
        skill_path: Path,
        config: Dict,
        llm: Any = None,
        skill_manager: Any = None
    ):
        self.skill_path = skill_path
        self.config = config
        self.llm = llm
        self.skill_manager = skill_manager

        self.evo_config = config.get("evolution_config", {
            "discovery_attempts": 3,
            "success_threshold": 0.8,
            "optimization_cycles": 5,
            "mutation_rate": 0.2,
            "selection_pressure": 0.7,
            "auto_evolve": True
        })

        self.storage_path = Path.home() / ".woclaw" / "evolution"
        self.storage_path.mkdir(parents=True, exist_ok=True)

        self.mission_controller = MissionController(llm=llm)
        self.discovery_module = DiscoveryModule(llm=llm)
        self.trueskill_rater = TrueSkillRater(storage_path=self.storage_path)
        self.skill_incubator = SkillIncubator(storage_path=self.storage_path, llm=llm)
        self.textgrad_optimizer = TextGradOptimizer(llm=llm)
        self.speciation_evolution = SpeciationEvolution(
            storage_path=self.storage_path,
            llm=llm,
            mutation_rate=self.evo_config["mutation_rate"],
            selection_pressure=self.evo_config["selection_pressure"]
        )
        self.memory = EvolutionMemory(storage_path=self.storage_path)

        print(f"✅ 进化引擎初始化完成")
        print(f"   存储路径: {self.storage_path}")

    async def start_evolution_cycle(self, task: str, mode: str = "auto") -> Dict:
        """启动完整的进化循环"""
        task_id = self._generate_task_id(task)
        context = EvolutionContext(
            task=task,
            task_id=task_id,
            start_time=time.time(),
            mode=mode
        )

        self.memory.record_event("evolution_start", {
            "task": task,
            "task_id": task_id,
            "mode": mode
        })

        try:
            await self._run_evolution_pipeline(context)
            context.success = True
            context.message = "进化循环完成"

        except Exception as e:
            context.success = False
            context.message = f"进化失败: {str(e)}"
            self.memory.record_event("evolution_error", {
                "task_id": task_id,
                "error": str(e)
            })

        duration = time.time() - context.start_time
        self.memory.record_event("evolution_complete", {
            "task_id": task_id,
            "duration": duration,
            "success": context.success,
            "new_skill": context.new_skill_name
        })

        result = {
            "success": context.success,
            "message": context.message,
            "task_id": task_id,
            "duration_seconds": round(duration, 2),
            "new_skill": context.new_skill_name,
            "evolved_skills": context.evolved_skills_count
        }

        return result

    async def _run_evolution_pipeline(self, context: EvolutionContext):
        """执行进化管道的各个阶段"""

        print("\n" + "─" * 50)
        print("📋 阶段1: 任务分析与匹配")
        print("─" * 50)
        context.stage = "mission_analysis"

        mission_result = await self.mission_controller.analyze_task(context.task)
        print(f"   任务类型: {mission_result['task_type']}")
        print(f"   难度评级: {mission_result['difficulty']}/10")
        print(f"   匹配技能: {mission_result['matching_skills']}")

        if mission_result["can_handle_with_existing"] and mission_result["confidence"] >= 0.9:
            print(f"✅ 现有技能足以处理，置信度 {mission_result['confidence']:.0%}")
            context.message = "使用现有技能处理，无需进化"
            return

        print(f"🧬 触发进化 - 现有技能置信度仅 {mission_result['confidence']:.0%}")

        print("\n" + "─" * 50)
        print("🔍 阶段2: 解决方案探索")
        print("─" * 50)
        context.stage = "discovery"

        attempts = self.evo_config["discovery_attempts"]
        context.discovered_solutions = await self.discovery_module.discover_solutions(
            context.task,
            num_attempts=attempts
        )

        print(f"   生成 {len(context.discovered_solutions)} 个候选方案")

        print("\n" + "─" * 50)
        print("⭐ 阶段3: 方案评级与筛选")
        print("─" * 50)
        context.stage = "rating"

        rated_solutions = []
        for i, solution in enumerate(context.discovered_solutions):
            rating = await self.trueskill_rater.rate_solution(
                solution,
                context.task
            )
            rated_solutions.append({**solution, **rating})
            print(f"   方案 {i+1}: 综合分 {rating['overall_score']:.1f} - {solution['strategy_name']}")

        context.best_solution = max(rated_solutions, key=lambda x: x["overall_score"])
        print(f"\n   🏆 最佳方案: {context.best_solution['strategy_name']}")
        print(f"      成功率: {context.best_solution['success_rate']:.1f}")
        print(f"      效率分: {context.best_solution['efficiency_score']:.1f}")

        if context.best_solution["overall_score"] < 60:
            print("⚠️  最佳方案分数不足，启动优化循环")
            context.best_solution = await self.textgrad_optimizer.optimize_solution(
                context.best_solution,
                context.task
            )
            context.optimization_rounds += 1

        print("\n" + "─" * 50)
        print("📦 阶段4: 技能孵化")
        print("─" * 50)
        context.stage = "incubation"

        skill_result = await self.skill_incubator.incubate_skill(
            context.best_solution,
            context.task
        )

        if skill_result["success"]:
            context.new_skill_name = skill_result["skill_name"]
            print(f"   🎉 新技能诞生: {skill_result['skill_name']} v{skill_result['version']}")
            print(f"   路径: {skill_result['path']}")

            await self.trueskill_rater.register_new_skill(
                skill_result["skill_name"],
                context.best_solution
            )
        else:
            print(f"❌ 技能孵化失败: {skill_result.get('error', '未知错误')}")

        print("\n" + "─" * 50)
        print("🌱 阶段5: 物种进化")
        print("─" * 50)
        context.stage = "speciation"

        evolution_result = await self.speciation_evolution.evolve_population()
        context.evolved_skills_count = evolution_result["evolved_count"]

        print(f"   种群大小: {evolution_result['population_size']}")
        print(f"   进化技能: {evolution_result['evolved_count']} 个")
        print(f"   淘汰技能: {evolution_result['pruned_count']} 个")
        if evolution_result["new_species"]:
            print(f"   🆕 新物种: {', '.join(evolution_result['new_species'])}")

    async def get_status(self) -> Dict:
        """获取进化引擎状态"""
        memory_stats = self.memory.get_statistics()
        skill_stats = self.trueskill_rater.get_skill_leaderboard()

        return {
            "success": True,
            "status": "running",
            "statistics": memory_stats,
            "top_skills": skill_stats[:10],
            "population_size": len(skill_stats),
            "total_evolution_cycles": memory_stats.get("total_cycles", 0)
        }

    async def list_evolved_skills(self) -> Dict:
        """列出所有已进化的技能"""
        skills = self.trueskill_rater.get_skill_leaderboard(limit=100)
        return {
            "success": True,
            "skills": skills,
            "count": len(skills)
        }

    async def optimize_skill(self, skill_name: str) -> Dict:
        """优化特定技能"""
        print(f"🚀 开始优化技能: {skill_name}")

        skill_data = self.trueskill_rater.get_skill_data(skill_name)
        if not skill_data:
            return {"success": False, "error": f"技能不存在: {skill_name}"}

        optimized = await self.textgrad_optimizer.optimize_skill(skill_data)

        if optimized["success"]:
            await self.skill_incubator.update_skill(skill_name, optimized)
            await self.trueskill_rater.update_skill_rating(skill_name, optimized)

        return optimized

    async def trigger_speciation(self) -> Dict:
        """触动物种进化"""
        return await self.speciation_evolution.evolve_population(force=True)

    def _generate_task_id(self, task: str) -> str:
        """生成任务ID"""
        timestamp = str(time.time()).encode()
        task_hash = hashlib.md5(task.encode()).hexdigest()[:8]
        return f"evo_{timestamp}_{task_hash}"
