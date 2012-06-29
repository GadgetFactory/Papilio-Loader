Option Explicit

	Dim objShell	'As WScript.Shell
	Dim sPrompt, iResponse
	Dim sPapilioLoader, sArguments

Set objShell = CreateObject("WScript.Shell")

sPrompt = "Do you wish to pass just .bit file or both .bit and .hex files?" & vbCrLf & _
		  "Please indicate your choice" & VbCrLf & VbCrLf
sPrompt = sPrompt & _
		  "{Yes}" & vbTab & "Only .bit File" & vbCrLf & _
		  "{No}" & vbTab & ".bit File + .hex File"

iResponse = MsgBox(sPrompt, vbYesNo, "Select Arguments")

'-splash:about.gif
sPapilioLoader = "javaw -classpath bin net.gadgetfactory.papilio.loader.PapilioLoader "

If (iResponse = vbYes) Then
	sArguments = """..\Test Files\Quickstart-Papilio_One_250K.bit"""
Else
	sArguments = """..\Test Files\AVR8_PapilioOne.bit""  ""..\Test Files\BPW5007_Button_LED_Wing.cpp.hex"""
End If

objShell.Run sPapilioLoader & sArguments

Set objShell = Nothing