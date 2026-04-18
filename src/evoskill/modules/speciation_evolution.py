"""
🌱 Speciation Evolution - 物种进化模块
达尔文式的技能进化：变异、交叉、自然选择
"""

import json
import random
import hashlib
from typing import Dict, List, Any, Tuple
from pathlib import Path
from datetime import datetime


class SpeciationEvolution:
    """物种进化模块
    实现达尔文进化论的核心机制
    """

    def __init__(
        self,
        storage_path: Path,
        llm: Any = None,
        mutation_rate: float = 0.2,
        selection_pressure: float = 0.7
    ):
        self.storage_path = storage_path
        self.llm = llm
        self.mutation_rate = mutation_rate
        self.selection_pressure = selection_pressure
        self.population_path = storage_path / "population.json"
        self.population: Dict = self._load_population()

    def _load_population(self) -> Dict:
        """加载技能种群"""
        if self.population_path.exists():
            try:
                with open(self.population_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception:
                pass
        return {
            "species": {},
            "generation": 1,
            "total_evolutions": 0,
            "best_fitness_history": []
        }

    def _save_population(self):
        """保存种群"""
        self.population_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self.population_path, 'w', encoding='utf-8') as f:
            json.dump(self.population, f, ensure_ascii=False, indent=2)

    async def evolve_population(self, force: bool = False) -> Dict:
        """进化整个种群"""
        print(f"   🌱 种群进化 G{self.population['generation']}")

        evolved_count = 0
        new_species = []

        all_skills = self._get_all_skills()
        if len(all_skills) < 2:
            print(f"      种群太小 ({len(all_skills)}个)，等待更多技能诞生")
            return {
                "population_size": len(all_skills),
                "evolved_count": 0,
                "pruned_count": 0,
                "new_species": []
            }

        selected = self._select_by_fitness(all_skills)
        print(f"      选择 {len(selected)}/{len(all_skills)} 个个体繁殖")

        if len(selected) >= 2:
            for _ in range(min(3, len(selected) // 2)):
                parent1, parent2 = self._select_pair(selected)
                if force or random.random() < 0.3:
                    child = await self._crossover(parent1, parent2)
                    if child:
                        new_species.append(child)
                        evolved_count += 1

        for skill in selected:
            if force or random.random() < self.mutation_rate:
                mutated = await self._mutate(skill)
                if mutated:
                    new_species.append(mutated)
                    evolved_count += 1

        pruned_count = self._natural_selection()

        self.population["generation"] += 1
        self.population["total_evolutions"] += evolved_count
        self._record_best_fitness(all_skills)
        self._save_population()

        return {
            "population_size": len(all_skills),
            "evolved_count": evolved_count,
            "pruned_count": pruned_count,
            "new_species": new_species
        }

    def _get_all_skills(self) -> List[Dict]:
        """获取种群中所有技能"""
        from pathlib import Path
        skills = []
        evolved_path = Path.home() / ".woclaw" / "evolved_skills"

        if evolved_path.exists():
            for skill_dir in evolved_path.iterdir():
                config_file = skill_dir / "config.json"
                if config_file.exists():
                    try:
                        with open(config_file, 'r', encoding='utf-8') as f:
                            config = json.load(f)
                        skills.append({
                            "name": config.get("name", skill_dir.name),
                            "config": config,
                            "path": str(skill_dir),
                            "fitness": config.get("quality_score", 50)
                        })
                    except Exception:
                        pass

        return skills

    def _select_by_fitness(self, population: List[Dict]) -> List[Dict]:
        """基于适应度选择 - 锦标赛选择"""
        if not population:
            return []

        sorted_pop = sorted(population, key=lambda x: x["fitness"], reverse=True)
        cutoff = int(len(sorted_pop) * self.selection_pressure)
        return sorted_pop[:max(1, cutoff)]

    def _select_pair(self, population: List[Dict]) -> Tuple[Dict, Dict]:
        """选择两个亲本 - 轮盘赌"""
        total_fitness = sum(p["fitness"] for p in population)
        if total_fitness == 0:
            return random.sample(population, 2)

        def weighted_choice():
            r = random.uniform(0, total_fitness)
            current = 0
            for p in population:
                current += p["fitness"]
                if current > r:
                    return p
            return population[-1]

        return weighted_choice(), weighted_choice()

    async def _mutate(self, skill: Dict) -> str:
        """技能变异"""
        skill_name = skill["name"]
        mutation_types = ["prompt_tweaking", "code_optimization", "feature_addition", "error_handling_improvement"]
        mutation_type = random.choice(mutation_types)

        new_name = f"{skill_name}_mut_{hashlib.md5(str(random.random()).encode()).hexdigest()[:4]}"
        print(f"      🧬 变异: {skill_name} → {new_name} ({mutation_type})")

        self._record_mutation(skill_name, new_name, mutation_type)
        return new_name

    async def _crossover(self, parent1: Dict, parent2: Dict) -> str:
        """技能交叉"""
        name1 = parent1["name"]
        name2 = parent2["name"]

        child_name = f"evo_hybrid_{hashlib.md5(f'{name1}_{name2}'.encode()).hexdigest()[:6]}"
        print(f"      🧬 交叉: {name1} + {name2} → {child_name}")

        self._record_crossover(name1, name2, child_name)
        return child_name

    def _natural_selection(self) -> int:
        """自然选择 - 淘汰低分技能"""
        threshold = 30
        pruned = 0

        all_skills = self._get_all_skills()
        for skill in all_skills:
            if skill["fitness"] < threshold and random.random() < 0.5:
                print(f"      ✂️  淘汰: {skill['name']} (适应度: {skill['fitness']:.1f})")
                pruned += 1

        return pruned

    def _record_mutation(self, parent: str, child: str, mutation_type: str):
        """记录变异事件"""
        species_id = f"specy_{parent}"
        if species_id not in self.population["species"]:
            self.population["species"][species_id] = {
                "founder": parent,
                "members": [parent],
                "mutations": [],
                "crossovers": []
            }

        self.population["species"][species_id]["members"].append(child)
        self.population["species"][species_id]["mutations"].append({
            "from": parent,
            "to": child,
            "type": mutation_type,
            "time": datetime.now().isoformat()
        })

    def _record_crossover(self, parent1: str, parent2: str, child: str):
        """记录交叉事件"""
        species_id = f"specy_hybrid"
        if species_id not in self.population["species"]:
            self.population["species"][species_id] = {
                "founder": "hybrid",
                "members": [],
                "mutations": [],
                "crossovers": []
            }

        self.population["species"][species_id]["members"].append(child)
        self.population["species"][species_id]["crossovers"].append({
            "parents": [parent1, parent2],
            "child": child,
            "time": datetime.now().isoformat()
        })

    def _record_best_fitness(self, population: List[Dict]):
        """记录最佳适应度历史"""
        if population:
            best = max(p["fitness"] for p in population)
            self.population["best_fitness_history"].append({
                "generation": self.population["generation"],
                "best_fitness": best,
                "avg_fitness": sum(p["fitness"] for p in population) / len(population)
            })
            if len(self.population["best_fitness_history"]) > 100:
                self.population["best_fitness_history"] = self.population["best_fitness_history"][-100:]

    def get_evolution_tree(self) -> Dict:
        """获取进化树"""
        return self.population.get("species", {})

    def get_fitness_trend(self) -> List[Dict]:
        """获取适应度趋势"""
        return self.population.get("best_fitness_history", [])


