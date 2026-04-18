"""
🧬 EVOSKILL - Autonomous AI Skill Evolution Framework

Core Modules:
- evolution_engine: Main evolution pipeline
- mission_controller: Task analysis and triggering
- discovery_module: Solution approach generation
- trueskill_rater: Bayesian skill quality rating
- skill_incubator: Production skill generation
- textgrad_engine: Natural gradient optimization
- skill_bank: Hierarchical 3-level skill library (SkillRL)
- speciation_evolution: Darwinian genetic operators
- evolution_memory: Lifelong learning persistence
"""

from .evolution_engine import EvolutionEngine
from .skill_bank import SkillBank, Skill
from .textgrad_engine import TextGradEngine
from .trueskill_rater import TrueSkillRater
from .discovery_module import DiscoveryModule
from .mission_controller import MissionController
from .skill_incubator import SkillIncubator

__all__ = [
    "EvolutionEngine",
    "SkillBank",
    "Skill",
    "TextGradEngine",
    "TrueSkillRater",
    "DiscoveryModule",
    "MissionController",
    "SkillIncubator",
]

__version__ = "1.0.0"
__author__ = "EVOSKILL AI Research Team"
