#!/bin/bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install borkdude/brew/babashka
mkdir -p "$HOME/Library/LaunchAgents"
brew autoupdate start --upgrade --enable-notification --immediate
brew upgrade babashka

brew doctor
