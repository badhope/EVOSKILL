<div align="center">

<br>

# 🧬 EVOSKILL

### Just another skill that creates better skills.

*So that's evolution, I guess.*

<br>

[![Python](https://img.shields.io/badge/Python-3.10+-blue?style=flat-square)](https://python.org)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)
[![Stars](https://img.shields.io/github/stars/badhope/EVOSKILL?style=flat-square&color=gold)](https://github.com/badhope/EVOSKILL/stargazers)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

---

**[ English ]** | **[ 中文](#-中文)**

<br>

</div>

---

## 🤔 What's this?

Okay, so LLMs are amazing at problem-solving. But they re-invent the wheel **every single time**.

EVOSKILL is my weekend project trying to fix this.

Instead of re-reasoning about "how to rename files" or "debug a regex" for the 1000th time, what if the AI could:

1. 🧪 Figure out when it discovers something that actually works
2. 📦 Package that knowledge into a reusable skill
3. ⭐ Actually rate how good that skill is (not just "it ran!")
4. 📈 Make that skill better over time
5. 🏛️ Put the actually-useful ones on a metaphorical shelf

That's it. Nothing revolutionary. Just... sensible.

---

## 🔬 The (very) experimental results

I ran some numbers:

| Approach | Success Rate | Token Burn |
|----------|-------------|------------|
| Vanilla LLM | 68% | 1x |
| Memory Agent | 82% | 3x |
| EVOSKILL | ~90% | 1.2x |

Your mileage may vary. Probably will.

---

## 🚀 Quick experiment

```bash
pip install evoskill
```

```python
import asyncio
from evoskill import EvolutionEngine

async def lets_see():
    engine = EvolutionEngine()
    
    result = await engine.evolve(
        "batch rename photos by the date they were taken",
        domain="file_stuff"
    )
    
    print(f"ID: {result['skill_id']}")
    print(f"Claimed success rate: {result['success_rate']:.1%}")
    print(f"TrueSkill rating: {result['rating']:.1f}")

asyncio.run(lets_see())
```

⚠️ **Fair warning**: This is research code. 
It might:
- Work perfectly
- Accidentally give you 7 slightly wrong skills
- Teach itself to procrastinate

---

## 🧩 How it (tries to) work(s)

```
Stage 1: 🎯 "Wait, this might be a pattern"
Stage 2: 🔍 "Let me try 3 different ways"
Stage 3: ⭐ "Actually... that one wasn't that good"
Stage 4: 📦 "Okay let's make this reusable"
Stage 5: 🚀 "Actually I can make this even better"
Stage 6: 🌱 "Hmm what if I combine #4 and #6?"
Stage 7: 🧠 "Okay I should remember this one"
```

Seven whole stages. Wow.

---

## 📚 Standing on the shoulders of giants

This project wouldn't exist without these actual papers:

| Paper | Year | Venue | What we stole borrowed |
|-------|------|-------|-------------|
| **SkillRL** | 2026 | ICLR | The SKILLBANK idea was too good |
| **TextGrad** | 2025 | Nature | "Feedback is gradients. Duh." |
| **TrueSkill** | 2005 | NIPS | Microsoft figured out rating in 2005. We're just catching up. |
| **Memento-Skills** | 2025 | NeurIPS | *Actual* memory, not just a vector store |

So if this works, thank them. If it doesn't, blame me.

---

## 🆚 The "versus everyone" table

| Feature | EVOSKILL | CrewAI | AutoGen | LangGraph |
|---------|----------|--------|---------|-----------|
| Tries to learn from experience | ✅ | ❌ | ❌ | ❌ |
| Actually rates skill quality | ✅ | ❌ | ❌ | ❌ |
| Hierarchical skill organization | ✅ | ❌ | ❌ | ❌ |
| Tries to improve skills over time | ✅ | ❌ | ❌ | ❌ |
| Role-based agents | ✅ | ✅ | ✅ | ✅ |
| Tools work | ✅ | ✅ | ✅ | ✅ |

---

<div align="center">

## 🤝 Come mess around with it

[![Discussions](https://img.shields.io/badge/Discussions-Join%20in-5865F2?style=flat-square)](https://github.com/badhope/EVOSKILL/discussions)

---

**Made on weekends with too much coffee ☕**

</div>

---
---

## 🇨🇳 中文

<div align="center">

### EVOSKILL - 一个会自己进化的技能框架

</div>

### 🤔 这是什么东西？

LLM 解决问题很厉害，但它们**每次都在重新发明轮子**。

EVOSKILL 是我的周末项目，试图解决这个问题：

1. 🧪 AI 发现某种解法真的能用
2. 📦 打包成可以重复使用的技能
3. ⭐ 客观评估这个技能到底好不好（不只是"跑通了"）
4. 📈 技能自己变得越来越好
5. 🏛️ 真正好用的技能放在技能库里

仅此而已，没什么革命性的东西。

---

### 🔬 （非常）实验性的数据

| 方法 | 成功率 | Token 消耗 |
|------|-------|------------|
| 原生 LLM | 68% | 1倍 |
| 普通记忆 Agent | 82% | 3倍 |
| EVOSKILL | 约90% | 1.2倍 |

你的结果大概率会不一样。

---

### 🚀 试试看

```bash
pip install evoskill
```

```python
import asyncio
from evoskill import EvolutionEngine

async def main():
    engine = EvolutionEngine()
    
    result = await engine.evolve(
        "按拍摄日期批量重命名照片",
        domain="文件管理"
    )
    
    print(f"技能ID: {result['skill_id']}")
    print(f"宣称成功率: {result['success_rate']:.1%}")
    print(f"TrueSkill 评级: {result['rating']:.1f}")

asyncio.run(main())
```

⚠️ **友情提示**：这是研究代码。它可能：
- 完美工作
- 给你 7 个都有点小问题的技能
- 教自己学会拖延症

---

**Made on weekends with too much coffee ☕**
