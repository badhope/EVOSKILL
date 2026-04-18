<div align="center">

<br>

# 🧬 EVOSKILL

### An AI Agent Skill, that creates better skills.

*Yes, that's recursion.*

<br>

[![Skill](https://img.shields.io/badge/Type-Agent%20Skill-purple?style=flat-square)]()
[![Python](https://img.shields.io/badge/Python-3.10+-blue?style=flat-square)](https://python.org)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)
[![Stars](https://img.shields.io/github/stars/badhope/EVOSKILL?style=flat-square&color=gold)](https://github.com/badhope/EVOSKILL/stargazers)

[**🇨🇳 中文版本**](README_CN.md)

---

</div>

---

## 🤔 What is this?

This is **a skill for AI agents**.

LLMs keep re-inventing the wheel every time. This skill lets them stop doing that.

When your agent figures something out that actually works, EVOSKILL:

1. 🧪 Notices that this wasn't just a one-off solution
2. 📦 Packages it into a reusable skill
3. ⭐ Actually rates how good that skill is (not just "it ran!")
4. 📈 Makes the skill better over time
5. 🏛️ Puts the actually-useful ones on a shelf

That's it. Just a skill, that makes more skills.

---

## 🚀 Use this skill in your agent

```python
from evoskill import EvolutionEngine

# Give your agent the power of evolution 🧬
agent.skills.append(EvolutionEngine())

# Now your agent can autonomously:
#  - Discover when a new skill is needed
#  - Create and rate candidate implementations
#  - Incubate production-ready skills
#  - Evolve existing skills to be better
```

It's just another skill your agent can use.

---

## 🔬 How this skill works

Seven simple stages:

```
🎯 Stage 1: "Wait, this task is a pattern"
🔍 Stage 2: "Let me try 3 different ways"
⭐ Stage 3: "Actually... that one wasn't that good"
📦 Stage 4: "Okay let's make this reusable"
🚀 Stage 5: "Actually I can make this even better"
🌱 Stage 6: "Hmm what if I combine #4 and #6?"
🧠 Stage 7: "Okay I should remember this one"
```

---

## 📚 Research credits (these people are way smarter)

| Paper | Year | Venue | What we use |
|-------|------|-------|-------------|
| **SkillRL** | 2026 | ICLR | Hierarchical SKILLBANK was too good an idea |
| **TextGrad** | 2025 | Nature | "Feedback is gradients. Duh." |
| **TrueSkill** | 2005 | NIPS | Microsoft figured out skill rating in 2005 |
| **Memento-Skills** | 2025 | NeurIPS | Actual memory, not just a vector store |

---

## 📁 Just a skill, not a framework

```
EVOSKILL/
├── skill.yaml                    # Skill definition (for agents)
└── evoskill/
    ├── __init__.py
    └── modules/
        ├── evolution_engine.py   # Main skill entry point
        ├── skill_bank.py         # Hierarchical skill storage
        ├── trueskill_rater.py    # Quality rating
        ├── textgrad_engine.py    # Skill optimization
        ├── discovery_module.py   # Solution discovery
        ├── mission_controller.py # When to evolve
        ├── skill_incubator.py    # Production skill generation
        ├── speciation_evolution.py  # Genetic operators
        └── evolution_memory.py   # Lifelong learning
```

---

<div align="center">

*It's skills all the way down.*

</div>
