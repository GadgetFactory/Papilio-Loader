javac -sourcepath src -d src/net/gadgetfactory/papilio/loader src/net/gadgetfactory/papilio/loader/PapilioLoader.java
jar cfm0 papilio-loader.jar PapilioLoader.mf -C src/net/gadgetfactory/papilio/loader/ .
#java -jar papilio-loader.jar
