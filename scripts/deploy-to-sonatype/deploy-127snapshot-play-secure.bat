set VERSION=1.2.7-SNAPSHOT
call ..\set-play-home-%VERSION%.bat

set MODULE_NAME=secure

call deploy-play-module-without-jar-snapshot.bat