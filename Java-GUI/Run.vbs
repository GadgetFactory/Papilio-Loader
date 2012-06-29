Set objShell = CreateObject("WScript.Shell")

'-splash:about.gif 
objShell.Run "javaw -classpath bin net.gadgetfactory.papilio.loader.PapilioLoader"