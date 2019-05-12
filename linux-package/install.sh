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

# Setup for use as non-root
usermod -a -G dialout $SUDO_USER
chgrp dialout /usr/local/bin/papilio-prog
rulestr1="SUBSYSTEMS==\"usb\", ATTRS{idVendor}==\"0403\", ATTRS{idProduct}==\"6010\", GROUP=\"dialout\""
rulestr2="SUBSYSTEMS==\"usb\", ATTRS{idVendor}==\"0403\", ATTRS{idProduct}==\"7bc0\", GROUP=\"dialout\""
echo $rulestr1 > /etc/udev/rules.d/papilio.rules
echo $rulestr2 >> /etc/udev/rules.d/papilio.rules
echo
echo "Note: to work without sudo, logout/login and replug any connected boards (or reboot)"
