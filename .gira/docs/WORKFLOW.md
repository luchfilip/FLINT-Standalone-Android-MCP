# Minimal Workflow Guide for ACP

## Overview
Simple three-stage workflow for straightforward task management.

## Workflow States
1. **Todo** - Tasks to be done
2. **In Progress** - Currently working on (WIP limit: 3)
3. **Done** - Completed tasks

## Common Commands

```bash
# Create a task
gira ticket create "Update documentation"

# Start work
gira ticket move ACP-1 "in progress"

# Complete task
gira ticket move ACP-1 done

# View board
gira board
```

## Best Practices
- Keep tasks small and actionable
- Limit work in progress
- Review and archive done items regularly
