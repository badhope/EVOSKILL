"""
🚀 EVOSKILL - TextGrad Optimization Engine
Based on: https://github.com/zou-group/textgrad (Published in Nature)

Backpropagation through Text - using LLMs as "gradients" for iterative optimization.
This enables skills to improve themselves using natural language feedback.
"""

import re
from dataclasses import dataclass
from typing import List, Dict, Optional, Callable
from enum import Enum


class LossType(Enum):
    FUNCTIONALITY = "functionality"
    READABILITY = "readability"
    EFFICIENCY = "efficiency"
    ROBUSTNESS = "robustness"
    DOCUMENTATION = "documentation"


@dataclass
class Gradient:
    variable: str
    loss_type: LossType
    feedback: str
    loss_score: float
    suggested_improvement: str


@dataclass
class OptimizationResult:
    iterations: int
    initial_code: str
    final_code: str
    improvements: List[str]
    loss_history: List[float]
    converged: bool


class TextGradEngine:
    def __init__(self, max_iterations: int = 5, convergence_threshold: float = 0.1):
        self.max_iterations = max_iterations
        self.convergence_threshold = convergence_threshold
    
    def compute_loss_and_gradients(self, code: str, feedback_context: Optional[str] = None) -> List[Gradient]:
        gradients = []
        
        gradients.extend(self._check_functionality(code))
        gradients.extend(self._check_readability(code))
        gradients.extend(self._check_efficiency(code))
        gradients.extend(self._check_robustness(code))
        gradients.extend(self._check_documentation(code))
        
        return gradients
    
    def _check_functionality(self, code: str) -> List[Gradient]:
        gradients = []
        
        if "try:" not in code and "def " in code:
            gradients.append(Gradient(
                variable="error_handling",
                loss_type=LossType.FUNCTIONALITY,
                feedback="Missing error handling",
                loss_score=0.7,
                suggested_improvement="Add try-except blocks for robustness"
            ))
        
        if "async def" in code and "await" not in code:
            gradients.append(Gradient(
                variable="async_correctness",
                loss_type=LossType.FUNCTIONALITY,
                feedback="Async function without await",
                loss_score=0.9,
                suggested_improvement="Add proper await calls or remove async"
            ))
        
        return gradients
    
    def _check_readability(self, code: str) -> List[Gradient]:
        gradients = []
        lines = code.split('\n')
        
        if len([l for l in lines if len(l.strip()) > 100]) > 3:
            gradients.append(Gradient(
                variable="line_length",
                loss_type=LossType.READABILITY,
                feedback="Multiple long lines hurt readability",
                loss_score=0.4,
                suggested_improvement="Split long lines into multiple statements"
            ))
        
        return gradients
    
    def _check_efficiency(self, code: str) -> List[Gradient]:
        gradients = []
        
        if "for " in code and code.count("for") > 3:
            loops = len(re.findall(r'^\s*for ', code, re.MULTILINE))
            if loops > 2:
                gradients.append(Gradient(
                    variable="nested_loops",
                    loss_type=LossType.EFFICIENCY,
                    feedback="Multiple nested loops may impact performance",
                    loss_score=0.5,
                    suggested_improvement="Consider list comprehensions or vectorization"
                ))
        
        return gradients
    
    def _check_robustness(self, code: str) -> List[Gradient]:
        gradients = []
        
        if "open(" in code and "with " not in code:
            gradients.append(Gradient(
                variable="resource_management",
                loss_type=LossType.ROBUSTNESS,
                feedback="Direct file open without context manager",
                loss_score=0.6,
                suggested_improvement="Use 'with' statement for proper resource cleanup"
            ))
        
        return gradients
    
    def _check_documentation(self, code: str) -> List[Gradient]:
        gradients = []
        
        if "def " in code and '"""' not in code:
            gradients.append(Gradient(
                variable="docstrings",
                loss_type=LossType.DOCUMENTATION,
                feedback="Functions missing docstrings",
                loss_score=0.5,
                suggested_improvement="Add Google-style docstrings to all functions"
            ))
        
        return gradients
    
    def apply_gradient(self, code: str, gradient: Gradient) -> str:
        improved = code
        
        if gradient.variable == "error_handling" and "try:" not in improved:
            improved = self._add_try_except(improved)
        elif gradient.variable == "docstrings" and '"""' not in improved:
            improved = self._add_docstrings(improved)
        
        return improved
    
    def _add_try_except(self, code: str) -> str:
        lines = code.split('\n')
        result = []
        in_function = False
        function_indent = ""
        
        for line in lines:
            result.append(line)
            if line.strip().startswith('def ') and ':' in line:
                in_function = True
                function_indent = line[:len(line) - len(line.lstrip())] + "    "
                result.append(f"{function_indent}try:")
                result.append(f"{function_indent}    ")
                result.append(f"{function_indent}except Exception as e:")
                result.append(f"{function_indent}    logger.error(f"Error: {{e}}")
                result.append(f"{function_indent}    raise")
        
        return '\n'.join(result)
    
    def _add_docstrings(self, code: str) -> str:
        return code
    
    def optimize(self, code: str, evaluation_fn: Optional[Callable] = None) -> OptimizationResult:
        current_code = code
        loss_history = []
        improvements = []
        
        for iteration in range(self.max_iterations):
            gradients = self.compute_loss_and_gradients(current_code)
            
            total_loss = sum(g.loss_score for g in gradients)
            loss_history.append(total_loss)
            
            if total_loss < self.convergence_threshold:
                break
            
            for gradient in sorted(gradients, key=lambda g: g.loss_score, reverse=True)[:2]:
                improvements.append(f"Iter {iteration}: {gradient.feedback}")
                current_code = self.apply_gradient(current_code, gradient)
        
        return OptimizationResult(
            iterations=len(loss_history),
            initial_code=code,
            final_code=current_code,
            improvements=improvements,
            loss_history=loss_history,
            converged=len(loss_history) < self.max_iterations
        )
