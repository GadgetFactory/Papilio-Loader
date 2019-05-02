#!/bin/bash

if [ "$SUDO_USER" == "" ]; then
    echo "Please run as sudo"
    exit 7
fi

apt-get install -y default-jdk
apt-get install -y libftdi-dev
cp -r ./GadgetFactory /opt/
ln -sf /opt/GadgetFactory/papilio-loader/papilio-loader.sh /usr/local/bin/papilio-loader-gui
ln -sf /opt/GadgetFactory/papilio-loader/programmer/linux/papilio-prog /usr/local/bin/papilio-prog
