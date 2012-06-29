Set objShell = CreateObject("WScript.Shell")

objShell.Run "%COMSPEC% /k javac -sourcepath src -d bin\net\gadgetfactory\papilio\loader net\gadgetfactory\papilio\loader\PapilioLoader.java"