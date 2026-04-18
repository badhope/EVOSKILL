"""
🧠 Evolution Memory - 进化记忆库
记录进化历史，存储成功模式和失败教训
"""

import json
from typing import Dict, List, Any
from pathlib import Path
from datetime import datetime, timedelta


class EvolutionMemory:
    """进化记忆库
    存储进化历史和学习到的模式
    """

    def __init__(self, storage_path: Path):
        self.storage_path = storage_path
        self.events_path = storage_path / "evolution_events.jsonl"
        self.patterns_path = storage_path / "successful_patterns.json"
        self.lessons_path = storage_path / "failure_lessons.json"

        self._init_storage()
        self.successful_patterns = self._load_json(self.patterns_path, default={})
        self.failure_lessons = self._load_json(self.lessons_path, default={})

    def _init_storage(self):
        """初始化存储"""
        self.storage_path.mkdir(parents=True, exist_ok=True)
        for path in [self.events_path, self.patterns_path, self.lessons_path]:
            if not path.exists():
                if path.suffix == '.jsonl':
                    path.touch()
                else:
                    with open(path, 'w') as f:
                        json.dump({}, f)

    def _load_json(self, path: Path, default: Any = None) -> Any:
        """加载JSON文件"""
        if path.exists():
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception:
                pass
        return default or {}

    def _save_json(self, path: Path, data: Any):
        """保存JSON文件"""
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def record_event(self, event_type: str, data: Dict):
        """记录进化事件"""
        event = {
            "timestamp": datetime.now().isoformat(),
            "type": event_type,
            "data": data
        }

        with open(self.events_path, 'a', encoding='utf-8') as f:
            f.write(json.dumps(event, ensure_ascii=False) + '\n')

    def record_success_pattern(self, pattern_name: str, pattern: Dict):
        """记录成功模式"""
        pattern_key = pattern_name.lower().replace(' ', '_')

        if pattern_key not in self.successful_patterns:
            self.successful_patterns[pattern_key] = {
                "name": pattern_name,
                "occurrences": 0,
                "first_seen": datetime.now().isoformat(),
                "success_rate": 0.0,
                "examples": []
            }

        record = self.successful_patterns[pattern_key]
        record["occurrences"] += 1
        record["last_seen"] = datetime.now().isoformat()
        if "examples" in pattern:
            record["examples"].extend(pattern["examples"][:3])

        self._save_json(self.patterns_path, self.successful_patterns)

    def record_failure_lesson(self, error_type: str, lesson: Dict):
        """记录失败教训"""
        lesson_key = error_type.lower().replace(' ', '_')

        if lesson_key not in self.failure_lessons:
            self.failure_lessons[lesson_key] = {
                "error_type": error_type,
                "occurrences": 0,
                "first_seen": datetime.now().isoformat(),
                "avoidance_strategy": "",
                "related_tasks": []
            }

        record = self.failure_lessons[lesson_key]
        record["occurrences"] += 1
        record["last_seen"] = datetime.now().isoformat()
        if "strategy" in lesson:
            record["avoidance_strategy"] = lesson["strategy"]
        if "task" in lesson:
            record["related_tasks"].append(lesson["task"][:50])

        self._save_json(self.lessons_path, self.failure_lessons)

    def get_statistics(self) -> Dict:
        """获取统计信息"""
        event_count = 0
        event_types = {}

        if self.events_path.exists():
            with open(self.events_path, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.strip():
                        event_count += 1
                        try:
                            event = json.loads(line)
                            evt_type = event.get("type", "unknown")
                            event_types[evt_type] = event_types.get(evt_type, 0) + 1
                        except Exception:
                            pass

        return {
            "total_events": event_count,
            "event_types": event_types,
            "successful_patterns": len(self.successful_patterns),
            "failure_lessons": len(self.failure_lessons),
            "pattern_occurrences": sum(p["occurrences"] for p in self.successful_patterns.values()),
            "total_failures_recorded": sum(l["occurrences"] for l in self.failure_lessons.values())
        }

    def get_recent_events(self, limit: int = 50) -> List[Dict]:
        """获取最近的事件"""
        events = []
        if self.events_path.exists():
            with open(self.events_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
                for line in lines[-limit:]:
                    if line.strip():
                        try:
                            events.append(json.loads(line))
                        except Exception:
                            pass
        return list(reversed(events))

    def get_top_patterns(self, limit: int = 10) -> List[Dict]:
        """获取最成功的模式"""
        patterns = list(self.successful_patterns.values())
        patterns.sort(key=lambda x: x["occurrences"], reverse=True)
        return patterns[:limit]

    def get_common_failures(self, limit: int = 10) -> List[Dict]:
        """获取最常见的失败"""
        failures = list(self.failure_lessons.values())
        failures.sort(key=lambda x: x["occurrences"], reverse=True)
        return failures[:limit]

    def get_lessons_for_task(self, task: str) -> List[Dict]:
        """获取相关任务的教训"""
        task_lower = task.lower()
        lessons = []

        for lesson in self.failure_lessons.values():
            for related_task in lesson.get("related_tasks", []):
                if related_task.lower() in task_lower or task_lower in related_task.lower():
                    lessons.append({
                        "error_type": lesson["error_type"],
                        "strategy": lesson["avoidance_strategy"],
                        "occurrences": lesson["occurrences"]
                    })
                    break

        return lessons

    def cleanup_old_events(self, days: int = 30):
        """清理旧事件"""
        cutoff = (datetime.now() - timedelta(days=days)).isoformat()
        preserved = []

        if self.events_path.exists():
            with open(self.events_path, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.strip():
                        try:
                            event = json.loads(line)
                            if event.get("timestamp", "") > cutoff:
                                preserved.append(line)
                        except Exception:
                            preserved.append(line)

            with open(self.events_path, 'w', encoding='utf-8') as f:
                f.writelines(preserved)

        return len(preserved)


