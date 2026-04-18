"""
⭐ TrueSkill Rater - 技能评级模块
基于微软TrueSkill算法的ELO风格技能评级系统
"""

import json
import math
from typing import Dict, List, Optional, Any
from pathlib import Path
from dataclasses import dataclass, field, asdict
from datetime import datetime


@dataclass
class SkillRating:
    """技能评级数据"""
    name: str
    mu: float = 25.0
    sigma: float = 8.333
    success_rate: float = 0.5
    efficiency: float = 0.5
    generalization: float = 0.5
    reliability: float = 0.5
    matches: int = 0
    wins: int = 0
    creation_time: str = ""
    last_updated: str = ""
    version: str = "1.0.0"
    lineage: List[str] = field(default_factory=list)
    tags: List[str] = field(default_factory=list)

    @property
    def rating(self) -> float:
        """保守技能分 = mu - 3*sigma"""
        return self.mu - 3 * self.sigma

    @property
    def overall_score(self) -> float:
        """综合评分 (0-100)"""
        base = min(100, (self.mu - 10) * 4)
        quality = (self.success_rate + self.efficiency +
                   self.generalization + self.reliability) * 6.25
        return (base * 0.6 + quality * 0.4)


class TrueSkillRater:
    """TrueSkill技能评级器"""

    BETA = 4.166
    TAU = 0.0833
    DRAW_PROBABILITY = 0.1

    def __init__(self, storage_path: Path):
        self.storage_path = storage_path
        self.ratings_path = storage_path / "skill_ratings.json"
        self.ratings: Dict[str, SkillRating] = {}
        self._load_ratings()

    def _load_ratings(self):
        """加载评级数据"""
        if self.ratings_path.exists():
            try:
                with open(self.ratings_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                for name, rating_data in data.items():
                    self.ratings[name] = SkillRating(**rating_data)
            except Exception:
                pass

    def _save_ratings(self):
        """保存评级数据"""
        self.ratings_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self.ratings_path, 'w', encoding='utf-8') as f:
            json.dump(
                {k: asdict(v) for k, v in self.ratings.items()},
                f,
                ensure_ascii=False,
                indent=2
            )

    async def rate_solution(self, solution: Dict, task: str) -> Dict:
        """对解决方案进行评级"""
        success_rate = solution.get("expected_success_probability", 0.5) * 100

        complexity_map = {"low": 80, "medium": 60, "high": 40}
        complexity_score = complexity_map.get(solution.get("estimated_complexity", "medium"), 60)

        efficiency_score = min(100, 100 - len(solution.get("edge_cases", [])) * 5)

        generalization = 70 if solution.get("generated_by_llm") else 50

        has_code = 1 if "code_snippet" in solution else 0
        has_tests = len(solution.get("test_cases", []))
        reliability = 40 + has_code * 20 + has_tests * 10

        overall_score = (
            success_rate * 0.3 +
            complexity_score * 0.15 +
            efficiency_score * 0.2 +
            generalization * 0.15 +
            reliability * 0.2
        )

        return {
            "success_rate": success_rate,
            "efficiency_score": efficiency_score,
            "generalization_score": generalization,
            "reliability_score": reliability,
            "complexity_score": complexity_score,
            "overall_score": overall_score
        }

    async def register_new_skill(self, skill_name: str, solution: Dict):
        """注册新技能"""
        now = datetime.now().isoformat()

        rating = SkillRating(
            name=skill_name,
            mu=15.0 + solution.get("overall_score", 60) / 10,
            success_rate=solution.get("success_rate", 50) / 100,
            efficiency=solution.get("efficiency_score", 60) / 100,
            generalization=solution.get("generalization_score", 60) / 100,
            reliability=solution.get("reliability_score", 60) / 100,
            creation_time=now,
            last_updated=now,
            tags=solution.get("tags", [])
        )

        self.ratings[skill_name] = rating
        self._save_ratings()
        print(f"      ⭐ 技能 {skill_name} 已注册，初始评级: {rating.rating:.1f}")

    async def update_skill_rating(self, skill_name: str, result: Dict):
        """更新技能评级"""
        if skill_name not in self.ratings:
            return

        rating = self.ratings[skill_name]

        success = result.get("success", False)
        performance = result.get("performance_score", 0.6)

        if success:
            delta_mu = 2 * performance
            rating.mu += delta_mu
            rating.wins += 1
        else:
            rating.mu = max(10, rating.mu - 1)

        rating.sigma = max(2, rating.sigma * 0.98 + self.TAU)
        rating.matches += 1
        rating.success_rate = (rating.success_rate * rating.matches +
                               (1 if success else 0)) / (rating.matches + 1)
        rating.last_updated = datetime.now().isoformat()

        self._save_ratings()

    async def update_ratings(self, skill_names: List[str], ranks: List[int]):
        """批量更新评级（技能对战）"""
        if len(skill_names) < 2:
            return

        ratings = [self.ratings.get(name, SkillRating(name=name)) for name in skill_names]

        for i, rating in enumerate(ratings):
            for j, other in enumerate(ratings):
                if i == j:
                    continue

                rank_diff = ranks[j] - ranks[i]
                if rank_diff > 0:
                    self._update_pairwise(rating, other, 1)
                elif rank_diff < 0:
                    self._update_pairwise(rating, other, 0)
                else:
                    self._update_pairwise(rating, other, 0.5)

        for name, rating in zip(skill_names, ratings):
            self.ratings[name] = rating

        self._save_ratings()

    def _update_pairwise(self, r1: SkillRating, r2: SkillRating, outcome: float):
        """TrueSkill pairwise update"""
        c = math.sqrt((r1.sigma ** 2) + (r2.sigma ** 2) + 2 * (self.BETA ** 2))

        winning_margin = math.sqrt(2) * self.BETA * self._inverse_cdf(
            (1 + self.DRAW_PROBABILITY) / 2
        )

        delta = r1.mu - r2.mu

        if outcome == 1:
            z = (delta - winning_margin) / c
        elif outcome == 0:
            z = (-delta - winning_margin) / c
        else:
            z = -abs(delta) / c

        p = self._cdf(z)
        v = self._pdf(z) / max(p, 0.001)

        if outcome == 1:
            r1.mu += (r1.sigma ** 2 / c) * v
            r2.mu -= (r2.sigma ** 2 / c) * v
        elif outcome == 0:
            r1.mu -= (r1.sigma ** 2 / c) * v
            r2.mu += (r2.sigma ** 2 / c) * v

        w = v * (v + z)
        r1.sigma = math.sqrt(max(4, r1.sigma ** 2 * (1 - w * r1.sigma ** 2 / c ** 2)))
        r2.sigma = math.sqrt(max(4, r2.sigma ** 2 * (1 - w * r2.sigma ** 2 / c ** 2)))

    def _cdf(self, x: float) -> float:
        """累积分布函数"""
        return 0.5 * (1 + math.erf(x / math.sqrt(2)))

    def _pdf(self, x: float) -> float:
        """概率密度函数"""
        return math.exp(-0.5 * x ** 2) / math.sqrt(2 * math.pi)

    def _inverse_cdf(self, p: float) -> float:
        """逆累积分布函数"""
        from scipy.special import erfinv
        return math.sqrt(2) * erfinv(2 * p - 1)

    def get_skill_leaderboard(self, limit: int = 50) -> List[Dict]:
        """获取技能排行榜"""
        leaderboard = []
        for name, rating in self.ratings.items():
            leaderboard.append({
                "name": name,
                "rating": round(rating.rating, 1),
                "mu": round(rating.mu, 1),
                "sigma": round(rating.sigma, 2),
                "overall_score": round(rating.overall_score, 1),
                "matches": rating.matches,
                "win_rate": round(rating.wins / max(1, rating.matches) * 100),
                "version": rating.version,
                "age_days": (datetime.now() - datetime.fromisoformat(rating.creation_time)).days if rating.creation_time else 0
            })

        leaderboard.sort(key=lambda x: x["rating"], reverse=True)
        return leaderboard[:limit]

    def get_skill_data(self, skill_name: str) -> Optional[Dict]:
        """获取单个技能数据"""
        if skill_name not in self.ratings:
            return None
        r = self.ratings[skill_name]
        return {
            "name": r.name,
            "rating": r.rating,
            "mu": r.mu,
            "sigma": r.sigma,
            "success_rate": r.success_rate,
            "efficiency": r.efficiency,
            "generalization": r.generalization,
            "reliability": r.reliability,
            "matches": r.matches,
            "version": r.version,
            "lineage": r.lineage
        }

    def prune_low_performers(self, threshold: float = 10.0) -> int:
        """淘汰低分技能"""
        to_remove = [name for name, r in self.ratings.items() if r.rating < threshold and r.matches > 5]
        for name in to_remove:
            del self.ratings[name]
        self._save_ratings()
        return len(to_remove)


