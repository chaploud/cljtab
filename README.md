# cljtab - Clojure CLI Tab Completion

Simple bash tab completion for Clojure CLI commands.

## Installation

Install cljtab as a Clojure CLI tool:

```bash
clj -Ttools install io.github.chaploud/cljtab '{:git/tag "v0.1.0"}' :as cljtab
```

## Setup

Install bash completion:

```bash
clj -Tcljtab setup
source ~/.bashrc
```

## Usage

### Generate Cache

Generate completion candidates for current directory:

```bash
clj -Tcljtab generate
```

### Tab Completion

Once installed, tab completion works automatically:

```bash
clj -A<TAB>        # Shows available aliases: :deps :test :build
clj -X:deps <TAB>  # Shows :deps functions: list tree find-versions prep
clj -Ttools <TAB>  # Shows tools functions: install install-latest list
```

## Files Created

- **Completion script**: `~/.local/share/bash-completion/completions/clj`
- **Cache**: `~/.cache/cljtab/<project-path>/candidates.edn`

## Changes to .bashrc

The setup adds only one line to `.bashrc`:

```bash
# Clojure CLI tab completion (cljtab)
[[ -f "$HOME/.local/share/bash-completion/completions/clj" ]] && source "$HOME/.local/share/bash-completion/completions/clj"
```

The actual completion function is stored in the separate completion file following Linux/Unix conventions.