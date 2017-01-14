#!/usr/bin/env python

# import des bibliothèque nécéssaire
import gopigo
from gopigo import *
import sys,os

#permet de récupérer le paramètre envoyé en ligne de commande
a=sys.argv[1]

#Effectue une instruction en fonction du paramètre reçu
if a=='H':
        fwd()                 #permet au GoPigo 2 d'avancer
        led_off(LED_R)        #permet l'extinction de la led droite
        led_off(LED_L)        #permet l'extinction de la led gauche
elif a=='G':
        left()                #permet au GoPigo 2 de faire une rotation sur la gauche
elif a=='D':
        right()               #permet au GoPigo 2 de faire une rotation sur la droite
elif a=='B':
        bwd()                 #permet au GoPigo 2 de reculer
        led_on(LED_R)         #permet l'allumage de la led droite
        led_on(LED_L)         #permet l'allumage de la led droite
elif a=='X':
        stop()                #permet au Gopigo 2 de ne plus bouger
        led_off(LED_R)
        led_off(LED_L)
elif a=='T':
        increase_speed()      #permet d'augmenter la vitesse du robot
elif a=='L':
        decrease_speed()      #permet de diminuer la vitesse du robot
spd=gopigo.read_motor_speed() #permet de récupérer la vitesse des moteurs
print ("Vitesse -> M1:%d ,M2:%d " %(spd[0],spd[1]))
