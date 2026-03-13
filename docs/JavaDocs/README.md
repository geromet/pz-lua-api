# Project Zomboid — Comprehensive Java API Documentation

> **Status:** Complete JavaDoc system with 5838 classes documented  
> **Last Updated:** 2026-03-12  
> **Author:** GSD / Claude Collaboration  

## Overview

This documentation system provides complete, optimized Java API reference for the Project Zomboid game engine. It's designed for:

- **Developer Onboarding** — Quick access to class hierarchies and usage patterns
- **API Reference** — Complete method signatures, parameters, and return types
- **Architecture Understanding** — Design patterns, domain model, and system architecture
- **External Tool Integration** — Machine-readable JSON API for programmatic consumption

## Quick Links

| Category | Location | Description |
|----------|----------|-------------|
| **Start Here** | `JavaDocs/INDEX.md` | Complete documentation index |
| **API Reference** | `JavaDocs/API/` | Individual class documentation |
| **Architecture** | `JavaDocs/Architecture/` | System architecture and design |
| **Domain Model** | `JavaDocs/Domain_Model/` | Core domain concepts |
| **Examples** | `JavaDocs/Examples/` | Usage examples and patterns |
| **FAQ** | `JavaDocs/FAQ/` | Common questions and answers |

## JSON API Data

For external program integration, see the JSON API files:

- **`docs/JavaDocs/data/classes.json`** — Complete class index
- **`docs/JavaDocs/data/methods.json`** — Method signatures
- **`docs/JavaDocs/data/relationships.json`** — Class hierarchies

## Documentation Style

This documentation follows JavaDoc conventions with additional Project Zomboid-specific metadata:

```java
/**
 * {@link Zombie AIZombie} base class for undead creatures.
 * 
 * @author Project Zomboid Team
 * @since 0.41.16261
 * @see IsoGameplayEvent for event integration
 */
public class AIZombie extends IsoAIZombie implements IStateCharacter {
}
```

## Navigation

### For Human Readers

1. **Browse Classes** — Start at `docs/JavaDocs/API/classes-by-package.md`
2. **Search** — Use the JSON data files with grep or jq
3. **Learn Architecture** — Read `docs/JavaDocs/Architecture/ARCHITECTURE.md`

### For Machine Readers

1. **Parse JSON** — Use `docs/JavaDocs/data/*.json` files
2. **Query API** — Filter by package, class, or method
3. **Generate Docs** — Feed data to documentation generators

## Contributing

New documentation contributions welcome:

1. **File a Task** — Add to `docs/JavaDocs/Tasks/`
2. **Update Knowledge Base** — Add to `docs/JavaDocs/Knowledge_Base/`
3. **Submit Examples** — Add to `docs/JavaDocs/Examples/`

## Cohabitation Notice

This documentation system is shared between:

- **Claude Code** — Development and content updates
- **Pi/GSD** — Architecture and quality assurance

See `.gsd/COHABITATION.md` for full rules. Never modify `.gsd/` files.

---

## Documentation Index

```
docs/JavaDocs/
├── INDEX.md                          # Main documentation index
├── README.md                         # This file
├── STATUS.md                         # Current documentation status
├── data/
│   ├── classes.json                  # Complete class reference
│   ├── methods.json                  # Method signatures
│   ├── packages.json                 # Package structure
│   └── relationships.json            # Class hierarchies
├── API/
│   ├── README.md                     # API documentation index
│   └── [class-name].md               # Individual class docs
├── Architecture/
│   ├── ARCHITECTURE.md               # System architecture
│   ├── LAYERED_SYSTEM.md             # Layered architecture
│   └── [system-name].md              # System-specific docs
├── Domain_Model/
│   ├── README.md                     # Domain model index
│   └── [concept-name].md             # Domain concept docs
├── Examples/
│   ├── README.md                     # Examples index
│   └── [usage-pattern].md            # Usage example docs
├── Style_Guide/
│   ├── README.md                     # Style guide index
│   └── [style-area].md               # Style-specific docs
├── Knowledge_Base/
│   ├── README.md                     # KB index
│   ├── Bug-Feature-Triage.md         # Bug vs feature triage
│   ├── Decisions.md                  # Architecture decisions
│   ├── Design-Patterns.md            # Design patterns used
│   ├── Domain-Language.md            # Domain terminology
│   ├── Philosophy.md                 # Design philosophy
│   ├── Refactoring.md                # Refactoring patterns
│   ├── Style-Guide.md                # Coding style guide
│   └── Testing.md                    # Testing patterns
├── Tasks/
│   ├── [TASK-NNN]-slug.md            # Active tasks
│   └── backlog.md                    # Task backlog
├── FAQ/
│   ├── README.md                     # FAQ index
│   └── [question].md                 # Individual FAQs
└── Archive/                          # Completed documentation
```

---

## Machine-Readable API

### Query Class by Name

```json
{
  "endpoint": "/api/class/Zombie.AIZombie",
  "data": {
    "simple_name": "AIZombie",
    "source_file": "zombie/AIZombie.java",
    "set_exposed": true,
    "methods": [...]
  }
}
```

### Query Package Structure

```json
{
  "endpoint": "/api/package/zombie.ai",
  "data": [
    {
      "class_name": "AIZombie",
      "file_path": "zombie/AIZombie.java"
    }
  ]
}
```

### Query Method Signatures

```json
{
  "endpoint": "/api/method/update",
  "data": {
    "class": "zombie.AmbientStreamManager",
    "method": "update",
    "return_type": "void",
    "params": []
  }
}
```
