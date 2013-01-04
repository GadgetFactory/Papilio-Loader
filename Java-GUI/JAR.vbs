Option Explicit

	Const JAR_FILE_NAME = "papilio-loader.jar"
	Const MANIFEST_FILE = "PapilioLoader.mf"
	
	Dim objShell	'As WScript.Shell
	Dim iErrorCode

Set objShell = CreateObject("WScript.Shell")

iErrorCode = objShell.Run("jar cfm0 " & JAR_FILE_NAME & " " & MANIFEST_FILE & " -C bin .", 0, True)

If (iErrorCode <> 0) Then
	WScript.Echo "Error occured creating papilio-loader.jar file" & vbCrLf & "Error Code: " & iErrorCode
End If

Set objShell = Nothing