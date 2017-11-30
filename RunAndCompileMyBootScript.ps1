rm -r bin/*
javac -classpath "jade.jar;." MyBoot.java -d "bin/"
cp default.properties bin/
cd bin/
java -classpath "../jade.jar;." MyBoot
pause
