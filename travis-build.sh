#!/usr/bin/env bash
set -e
rm -rf *.zip
chmod +x ./grailsw
./grailsw refresh-dependencies --non-interactive
./grailsw compile --non-interactive
./grailsw test-app --non-interactive
./grailsw package-plugin --non-interactive
./grailsw doc --pdf --non-interactive
