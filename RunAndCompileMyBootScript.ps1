$binFolder = "bin";
remove-item -path $binFolder -Recurse -ErrorAction SilentlyContinue
new-item -name $binFolder -itemtype directory
javac -classpath "jade.jar;." MyBoot.java -d $binFolder
cp default.properties $binFolder
cd $binFolder
java -classpath "../jade.jar;." MyBoot
cd ..
pause
