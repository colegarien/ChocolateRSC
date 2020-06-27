#!/bin/bash

# Run the game server
echo ""
echo "Running the game server. Press CTRL + C to shut it down or"
echo "CTRL + A + D to detach the screen so this continues in the background."
echo ""
touch ../gameserver.log && chmod 777 ../gameserver.log &>/dev/null
gradlew run | tee -a ../gameserver.log
