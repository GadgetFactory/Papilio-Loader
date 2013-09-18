#!/bin/sh
echo Be sure there is a Java installed. For ubuntu do a "sudo apt-get install default-jdk"
echo Be sure libftdi is installed. For ubuntu do a "sudo apt-get install libftdi-dev"
cd Java-GUI
sudo ./build.sh
sudo mkdir -p /opt/GadgetFactory/papilio-loader/
sudo cp -pr papilio-loader.jar papilio-loader.sh programmer/ images/ help/ /opt/GadgetFactory/papilio-loader/
sudo ln -s /opt/GadgetFactory/papilio-loader/papilio-loader.sh /usr/local/bin/papilio-loader-gui
sudo ln -s /opt/GadgetFactory/papilio-loader/programmer/linux32/papilio-prog /usr/local/bin/papilio-prog
