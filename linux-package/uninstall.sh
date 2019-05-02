#!/bin/bash

if [ "$SUDO_USER" == "" ]; then
    echo "Please run as sudo"
    exit 7
fi


rm -rf /opt/GadgetFactory
rm -f /usr/local/bin/papilio-loader-gui
rm -f /usr/local/bin/papilio-prog
