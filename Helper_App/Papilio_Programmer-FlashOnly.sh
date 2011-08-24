bitfile=bitfile
	
./dialog --title "Papilio SPI Flash Programmer" \
		--menu "Please choose the size of your Papilio board:" 15 55 5 \
		"bscan_spi_xc3s250e.bit" "250K Papilio" \
		"bscan_spi_xc3s500e.bit" "500K Papilio" \
		"bscan_spi_xc3s100e.bit" "100K Papilio" 2> $bitfile
return_value=$?

./papilio-prog.exe -v -f "$1" -b `./cat $bitfile` -sa -r
return_value=$?

if [ $return_value == 1 ] #If programming failed then show error.
then
	./dialog --timeout 5 --msgbox "The bit file failed to program to the Papilio, please check that the Papilio is plugged into a USB port." 15 55
	read -n1 -r -p "Press any key to continue..." key
fi
