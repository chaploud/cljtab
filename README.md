# cljtab - Clojure CLI Tab Completion

Simple bash tab completion for Clojure CLI commands. Fast startup with Babashka.

## Installation

### Method 1: Using bbin (requires babashka and Java)

```bash
bbin install io.github.chaploud/cljtab
```

### Method 2: Local installation with bbin

```bash
git clone https://github.com/chaploud/cljtab.git
cd cljtab
bbin install .
```

### Method 3: Direct babashka execution (recommended - no Java required)

```bash
git clone https://github.com/chaploud/cljtab.git
cd cljtab
chmod +x cljtab
./cljtab setup
```

Or add to PATH:
```bash
ln -s "$(pwd)/cljtab" ~/.local/bin/cljtab
```

Note: If you encounter classpath errors with bbin installation, use this method instead.

## Setup

Install shell completion (auto-detects your shell):

```bash
cljtab setup
```

Or specify shell explicitly:

```bash
# For bash
cljtab setup bash
source ~/.bashrc

# For zsh
cljtab setup zsh
source ~/.zshrc

# For both shells
cljtab setup both
```

## Usage

### Generate Cache

Generate completion candidates for current directory (must be run in a directory with deps.edn):

```bash
cljtab generate
```

### Clean Installation

Remove all cache files and shell configuration:

```bash
cljtab clean
```

### Tab Completion

Once installed, tab completion works automatically:

```bash
clj -A<TAB>        # Shows available aliases: :deps :test :build
clj -X:deps <TAB>  # Shows :deps functions: list tree find-versions prep
clj -Ttools <TAB>  # Shows tools functions: install install-latest list
```

The completion system automatically searches for the nearest cache file in parent directories, allowing you to work in subdirectories of your project.

## Files Created

### Bash
- **Completion script**: `~/.local/share/bash-completion/completions/clj`
- **Changes to .bashrc**: Sources the completion file

### Zsh
- **Completion script**: `~/.local/share/zsh/site-functions/_clj`
- **Changes to .zshrc**: Adds completion directory to fpath and enables completion

### Cache
- **Cache**: `~/.cache/cljtab/<full-project-path>/candidates.edn`
  - Cache files are stored using the full directory path structure
  - Completion searches for the nearest cache file in parent directories

## Shell Configuration Changes

### Bash (.bashrc)
```bash
# Clojure CLI tab completion (cljtab)
[[ -f "$HOME/.local/share/bash-completion/completions/clj" ]] && source "$HOME/.local/share/bash-completion/completions/clj"
```

### Zsh (.zshrc)
```bash
# Clojure CLI tab completion (cljtab)
fpath=("$HOME/.local/share/zsh/site-functions" $fpath)
autoload -U compinit
compinit
```

The actual completion functions are stored in separate files following shell conventions.