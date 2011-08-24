bitfile=bitfile
	
./papilio-prog.exe -v -f "$1" -v
return_value=$?
echo "Programming returned: " $return_value	
if [ $return_value == 0 ] #If programming was succesful then ask about programming to flash.
then	

	./dialog --cancel-label "Program Permanently" --ok-label "Exit" --title "Papilio Programmer" \
			--pause "Your bit file has temporarily been loaded to the FPGA but will be lost once power is removed.\n\nUse the arrow keys to select 'Program Permanently' to keep the design after power is removed." 15 55 10 	
	return_value=$?
	echo $return_value	

	if [ $return_value == 1 ]
	then	
	./dialog --title "Papilio SPI Flash Programmer" \
			--menu "Please choose the size of your Papilio board:" 15 55 5 \
			"bscan_spi_xc3s250e.bit" "250K Papilio" \
			"bscan_spi_xc3s500e.bit" "500K Papilio" \
			"bscan_spi_xc3s100e.bit" "100K Papilio" 2> $bitfile
	return_value=$?

	./papilio-prog.exe -v -f "$1" -b `./cat $bitfile` -sa -r
	return_value=$?
	fi

fi

	
if [ $return_value == 1 ] #If programming failed then show error.
then
	./dialog --timeout 5 --msgbox "The bit file failed to program to the Papilio, please check that the Papilio is plugged into a USB port." 15 55
	read -n1 -r -p "Press any key to continue..." key
fi