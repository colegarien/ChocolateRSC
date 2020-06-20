#!/bin/bash

echo ""
echo ""
echo "Compiling the game server."
echo ""
sudo gradlew build
echo ""
echo ""
echo "Done!"
./run_server.sh
