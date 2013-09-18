#!/bin/sh
cd Java-GUI
sudo ./build.sh
sudo mkdir -p /opt/GadgetFactory/papilio-loader/
sudo cp -p papilio-loader.jar papilio-loader.sh /opt/GadgetFactory/papilio-loader/
sudo ln -s /opt/GadgetFactory/papilio-loader/papilio-loader.sh /usr/local/bin/papilio-loader

