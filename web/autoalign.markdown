Tout commence par le bouton "AutoAlign", qui se trouve dans une instance
de la classe "ControlBox", instanciée dans la classe principale Aligneur.java

L'objet qui fait l'alignement est de la classe "AutoAligner"
Cet objet lance un thread (cf. method "run") qui réalise l'alignement et qui
est tué lorsqu'on appuie sur le bouton "StopIt", ce qui lance la method "stopAutoAlign"

Dans Aligneur, la var autoAligner contient les alignements en cours.

