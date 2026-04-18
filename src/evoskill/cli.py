"""
🧬 EVOSKILL Command Line Interface

Universal CLI for the EVOSKILL autonomous skill evolution framework.
"""

import click
from rich.console import Console
from rich.panel import Panel

console = Console()

@click.group()
@click.version_option(version="1.0.0", prog_name="evoskill")
def cli():
    """
    🧬 EVOSKILL - Autonomous AI Skill Evolution Framework
    
    The world's first open-source framework enabling LLMs to achieve
    recursive self-improvement without weight updates.
    """
    pass

@cli.command()
def info():
    """Display EVOSKILL system information."""
    console.print(Panel.fit(
        "[bold cyan]🧬 EVOSKILL v1.0.0[/bold cyan]\n\n"
        "[bold]Autonomous AI Skill Evolution Framework[/bold]\n\n"
        "• SkillRL Hierarchical SKILLBANK\n"
        "• TextGrad Natural Gradient Optimization\n"
        "• TrueSkill Bayesian Rating System\n"
        "• Darwinian Genetic Evolution\n\n"
        "[dim]Evolution is just another skill.[/dim]",
        title="EVOSKILL",
        border_style="cyan"
    ))

@cli.command()
@click.argument("task")
@click.option("--domain", default="general", help="Domain category")
@click.option("--iterations", default=3, help="Evolution cycles")
def evolve(task, domain, iterations):
    """Evolve a new skill for a given TASK."""
    console.print(f"🔍 Starting evolution for: [bold]{task}[/bold]")
    console.print(f"📂 Domain: {domain} | 🔄 Iterations: {iterations}")
    console.print("\n⚠️  LLM integration requires additional configuration.")
    console.print("   This is a framework skeleton - see documentation for full setup.")

@cli.command()
def list():
    """List all evolved skills in SKILLBANK."""
    console.print("📦 SKILLBANK Library:")
    console.print("  • General Skills - 0")
    console.print("  • Domain Skills  - 0")
    console.print("  • Task Skills    - 0")
    console.print("\n[dim]Start evolving skills to populate your SKILLBANK![/dim]")

if __name__ == "__main__":
    cli()
