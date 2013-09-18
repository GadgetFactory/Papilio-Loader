#!/bin/sh
sudo mkdir -p /opt/GadgetFactory/papilio-loader/
sudo cp -p Java-GUI/papilio-loader.jar Java-GUI/papilio-loader.sh /opt/GadgetFactory/papilio-loader/
sudo ln -s /opt/GadgetFactory/papilio-loader/papilio-loader.sh /usr/local/bin/papilio-loader

