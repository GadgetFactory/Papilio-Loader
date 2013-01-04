Set objShell = CreateObject("WScript.Shell")

objShell.Run "%COMSPEC% /k javac -sourcepath src -d src\net\gadgetfactory\papilio\loader src\net\gadgetfactory\papilio\loader\PapilioLoader.java"