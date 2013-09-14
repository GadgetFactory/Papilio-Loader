# Copyright 2009-2011 Jack Gassett
# Creative Commons Attribution license
# Made for the Papilio FPGA boards

bitfile=bitfile
	
./dialog --timeout 3 --title "Papilio Programmer" \
		--menu "Use the arrow keys to select your option:" 15 55 5 \
		"1" "Program FPGA - Temporary" \
		"2" "Program SPI Flash - Permanent" \
		"3" "Save bit file to Favorites folder" 2> $bitfile
return_value=`cat $bitfile`	
#echo $return_value

case "$return_value" in
	"1")
	
		echo "Programming the FPGA"
		echo $1
		linbin/papilio-prog -v -f $1 -v
		return_value=$?
		;;	
	"2")
		echo "Programming to SPI Flash"
		# Find device id and choose appropriate bscan bit file
	
		device_id=`linbin/papilio-prog -j | awk '{print $9}'`
		return_value=$?
		
		case $device_id in
			XC3S250E)
				echo "Programming a Papilio One 250K"
				bscan_bitfile=bscan_spi_xc3s250e.bit
				;;	
			XC3S500E)
				echo "Programming a Papilio One 500K"
				bscan_bitfile=bscan_spi_xc3s500e.bit
				;;
			XC6SLX9)
				echo "Programming a Papilio Plus LX9"
				bscan_bitfile=bscan_spi_lx9.bit
				;;				
			*)
				echo "Unknown Papilio Board"
				;;
		esac

		linbin/papilio-prog -v -f "$1" -b $bscan_bitfile -sa -r
		#Cause the Papilio to restart
		linbin/papilio-prog -c
		return_value=$?
		;;
	"3")
		echo "Copy to Favorites"
		#TODO figure how to do in Windows and Linux
		./cp $1 "$2/Favorites/"
		cd "$2/Favorites/"
		explorer .	
		;;
	*)
		echo "Programming the FPGA - Default"
		echo $1
		linbin/papilio-prog -v -f $1 -v
		return_value=$?
		;;
		#exit 1
esac
		
if [ $return_value == 1 ] #If programming failed then show error.
then
	linbin/dialog --timeout 5 --msgbox "The bit file failed to program to the Papilio, please check that the Papilio is plugged into a USB port." 15 55
	read -n1 -r -p "Press any key to continue..." key
fi
