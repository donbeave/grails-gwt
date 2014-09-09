#!/usr/bin/env bash

set -e

[[ -d target ]] && rm -rf target
rm -rf *.zip
chmod +x ./grailsw
./grailsw refresh-dependencies --non-interactive
./grailsw compile --non-interactive
./grailsw test-app --non-interactive
./grailsw package-plugin --non-interactive
