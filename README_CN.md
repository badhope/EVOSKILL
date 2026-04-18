<div align="center">

<br>

# 🧬 EVOSKILL

### 一个能创造更好技能的 AI Agent 技能

*是的，这是递归。*

<br>

[![Skill](https://img.shields.io/badge/类型-Agent%20Skill-purple?style=flat-square)]()
[![Python](https://img.shields.io/badge/Python-3.10+-blue?style=flat-square)](https://python.org)
[![License](https://img.shields.io/badge/许可证-MIT-green?style=flat-square)](LICENSE)
[![Stars](https://img.shields.io/github/stars/badhope/EVOSKILL?style=flat-square&color=gold)](https://github.com/badhope/EVOSKILL/stargazers)

[**🇬🇧 English**](README.md)

---

</div>

> ⚠️ **重要**: 这只是一个技能，不是一个框架。你可以把它加到你的 Agent 里，就像其他技能一样。

---

## 🤔 这是什么？

这是一个**给 AI Agent 用的技能**。

LLM 每次都在重复造轮子。这个技能让它们停下来。

当你的 Agent 发现某个方案确实有效时，EVOSKILL 会：

1. 🧪 注意到这不仅仅是一次性解决方案
2. 📦 把它打包成可复用的技能
3. ⭐ 真正评估这个技能有多好（不只是"它跑了！"）
4. 📈 随时间让技能变得更好
5. 🏛️ 把真正有用的放到技能银行

就是这样。一个能创造技能的技能。

---

## 🚀 在你的 Agent 中使用

```python
from evoskill import EvolutionEngine

# 给你的 Agent 进化的力量 🧬
agent.skills.append(EvolutionEngine())

# 现在你的 Agent 可以自主：
#  - 发现什么时候需要新技能
#  - 创建和评估候选实现
#  - 孵化生产级技能
#  - 进化现有技能使其更好
```

这只是你的 Agent 可以使用的另一个技能。

---

## 🔬 这个技能如何工作

七个简单阶段：

```
🎯 阶段 1: "等等，这个任务是个模式"
🔍 阶段 2: "让我试 3 种不同的方法"
⭐ 阶段 3: "其实...那个方法没那么好"
📦 阶段 4: "好，让它可复用"
🚀 阶段 5: "其实我可以让它更好"
🌱 阶段 6: "嗯，如果我把 #4 和 #6 结合呢？"
🧠 阶段 7: "好，我得记住这个"
```

---

## 📚 研究致谢（这些人聪明多了）

| 论文 | 年份 | 会议 | 我们用了什么 |
|------|------|------|-------------|
| **SkillRL** | 2026 | ICLR | 分层 SKILLBANK 的想法太好了 |
| **TextGrad** | 2025 | Nature | "反馈就是梯度。废话。" |
| **TrueSkill** | 2005 | NIPS | 微软在 2005 年就搞懂技能评分了 |
| **Memento-Skills** | 2025 | NeurIPS | 真正的记忆，不只是向量存储 |

---

## 📁 项目结构

```
EVOSKILL/
├── skill.yaml                    # 技能定义（给 Agent 用）
└── evoskill/
    ├── __init__.py
    └── modules/
        ├── evolution_engine.py   # 主入口
        ├── skill_bank.py         # 分层技能存储
        ├── trueskill_rater.py    # 质量评分
        ├── textgrad_engine.py    # 技能优化
        ├── discovery_module.py   # 解决方案发现
        ├── mission_controller.py # 何时进化
        ├── skill_incubator.py    # 生产级技能生成
        ├── speciation_evolution.py  # 遗传算子
        └── evolution_memory.py   # 终身学习
```

---

## ⚡ 快速开始

```bash
# 克隆仓库
git clone https://github.com/badhope/EVOSKILL.git
cd EVOSKILL

# 安装依赖
pip install -e .

# 运行测试
python test_evolution.py
```

---

## 🎯 使用场景

- **AI Agent 开发者**: 让你的 Agent 自动进化出新技能
- **研究者**: 研究技能学习、元学习、自主进化
- **自动化工程师**: 构建能自我改进的生产系统

---

## 🤝 贡献

欢迎贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

---

<div align="center">

*层层嵌套，皆是技能。*

**Made with 🧬 by [badhope](https://github.com/badhope)**

</div>
