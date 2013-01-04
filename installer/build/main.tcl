set enc [encoding system]
encoding system utf-8
source [file join $::installkit::root main2.tcl]
encoding system $enc
