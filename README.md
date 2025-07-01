# cljtab - Clojure CLI Tab Completion

Simple bash tab completion for Clojure CLI commands. Fast startup with Babashka.

## Installation

Install cljtab with bbin (requires babashka):

```bash
bbin install io.github.chaploud/cljtab
```

Or clone and install locally:

```bash
git clone https://github.com/chaploud/cljtab.git
cd cljtab
bbin install .
```

## Setup

Install bash completion:

```bash
cljtab setup
source ~/.bashrc
```

## Usage

### Generate Cache

Generate completion candidates for current directory:

```bash
cljtab generate
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