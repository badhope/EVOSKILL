"""
🧬 EVOSKILL - Hierarchical Skill Bank
Implementation based on SkillRL: https://arxiv.org/pdf/2602.08234

SKILLBANK organizes evolved skills into a hierarchical library:
- General Skills: Universal strategic guidance
- Domain Skills: Category-level heuristics
- Task Skills: Specific task implementations

This provides 7.8% improvement over standard memory-based methods
(SkillRL vs SimpleMem+GRPO, 89.9% vs 82.1% success rate)
"""

import json
import numpy as np
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import Dict, List, Optional, Tuple
from datetime import datetime


@dataclass
class Skill:
    skill_id: str
    name: str
    version: str
    category: str
    domain: str
    description: str
    success_rate: float
    usage_count: int
    code: str
    created_at: str
    rating: float
    examples: List[str]
    parent_skill: Optional[str] = None
    evolved_from: List[str] = None


class SkillBank:
    def __init__(self, storage_path: Path):
        self.storage_path = storage_path / "skillbank"
        self.storage_path.mkdir(parents=True, exist_ok=True)
        
        self.general_skills: Dict[str, Skill] = {}
        self.domain_skills: Dict[str, Dict[str, Skill]] = {}
        self.task_skills: Dict[str, Skill] = {}
        
        self.load()
    
    def add_skill(self, skill: Skill, level: str = "task") -> bool:
        skill.created_at = datetime.now().isoformat()
        
        if level == "general":
            self.general_skills[skill.skill_id] = skill
        elif level == "domain":
            if skill.domain not in self.domain_skills:
                self.domain_skills[skill.domain] = {}
            self.domain_skills[skill.domain][skill.skill_id] = skill
        else:
            self.task_skills[skill.skill_id] = skill
        
        self.save()
        return True
    
    def retrieve(self, task: str, domain: Optional[str] = None, top_k: int = 3) -> List[Tuple[Skill, float]]:
        candidates = []
        
        for skill in self.general_skills.values():
            score = self._semantic_match(task, skill.description)
            candidates.append((skill, score * 1.2))
        
        if domain and domain in self.domain_skills:
            for skill in self.domain_skills[domain].values():
                score = self._semantic_match(task, skill.description)
                candidates.append((skill, score * 1.1))
        
        for skill in self.task_skills.values():
            score = self._semantic_match(task, skill.description)
            score *= (0.5 + skill.success_rate / 2)
            candidates.append((skill, score))
        
        candidates.sort(key=lambda x: x[1], reverse=True)
        return candidates[:top_k]
    
    def _semantic_match(self, query: str, text: str) -> float:
        query_words = set(query.lower().split())
        text_words = set(text.lower().split())
        overlap = len(query_words & text_words)
        return overlap / (1 + len(query_words))
    
    def get_skill_statistics(self) -> Dict:
        return {
            "general": len(self.general_skills),
            "domains": len(self.domain_skills),
            "domain_skills": sum(len(d) for d in self.domain_skills.values()),
            "task_skills": len(self.task_skills),
            "total_usage": sum(s.usage_count for s in self.task_skills.values()),
            "avg_success_rate": np.mean([s.success_rate for s in self.task_skills.values()]) if self.task_skills else 0
        }
    
    def promote_skill(self, skill_id: str) -> str:
        if skill_id in self.task_skills:
            skill = self.task_skills[skill_id]
            if skill.success_rate > 0.8 and skill.usage_count > 10:
                del self.task_skills[skill_id]
                skill.category = "domain"
                self.domain_skills.setdefault(skill.domain, {})[skill_id] = skill
                self.save()
                return f"promoted to domain: {skill_id}"
        
        for domain in self.domain_skills:
            if skill_id in self.domain_skills[domain]:
                skill = self.domain_skills[domain][skill_id]
                if skill.success_rate > 0.9 and skill.usage_count > 50:
                    del self.domain_skills[domain][skill_id]
                    skill.category = "general"
                    skill.domain = "universal"
                    self.general_skills[skill_id] = skill
                    self.save()
                    return f"promoted to general: {skill_id}"
        
        return "no promotion"
    
    def save(self):
        data = {
            "general_skills": {k: asdict(v) for k, v in self.general_skills.items()},
            "domain_skills": {d: {k: asdict(v) for k, v in s.items()} 
                            for d, s in self.domain_skills.items()},
            "task_skills": {k: asdict(v) for k, v in self.task_skills.items()}
        }
        with open(self.storage_path / "skillbank.json", 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2)
    
    def load(self):
        path = self.storage_path / "skillbank.json"
        if path.exists():
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            self.general_skills = {k: Skill(**v) for k, v in data.get("general_skills", {}).items()}
            self.domain_skills = {d: {k: Skill(**v) for k, v in s.items()} 
                                for d, s in data.get("domain_skills", {}).items()}
            self.task_skills = {k: Skill(**v) for k, v in data.get("task_skills", {}).items()}
