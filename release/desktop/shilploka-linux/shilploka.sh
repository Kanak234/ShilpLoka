#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
echo "☸ Launching ShilpLoka: Ancient Voxel Sandbox..."
if command -v xdg-open > /dev/null; then
  xdg-open "$DIR/dist/index.html"
elif command -v sensible-browser > /dev/null; then
  sensible-browser "$DIR/dist/index.html"
else
  google-chrome "$DIR/dist/index.html" || firefox "$DIR/dist/index.html"
fi
