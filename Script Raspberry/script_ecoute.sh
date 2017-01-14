#!/bin/bash
#permet d'écouter les données reçues sur le serial ttyUSB0 (module Bluetooth HC-05) sur 9600 Bauds
stty -F /dev/ttyUSB0 9600 cs8 ignbrk -brkint -imaxbel -opost -onlcr -isig -icanon -iexten -echo -echoe -echok -echoctl -echoke noflsh -ixon -crtscts -parenb -parodd -cstopb cread clocal
#Tant que le script reçoit un caractère il le transmet au script python en paramètre
while true
do
   inputline=""
   inputline=$(head -c 1 < /dev/ttyUSB0)
   python /home/pi/Desktop/basic_robot_Control.py $inputline
done
