---
categories: Doc cas particuliers
date: 2013/04/06 15:25:00
title: Offset en début de fichier
---
Il peut arriver que le début de certains fichiers WAV ne soit pas transcris, ou bien que le fichier WAV
commence par de la musique. Dans de tels cas, la transcription ne doit pas être alignée avec le début du
fichier WAV, mais en débutant à un certain offset, par exemple 1min30, voire 5 minutes.

Dans ce cas, si on lance l'auto-aligner, il se trompera systématiquement dès le début, car il essayera d'aligner
le texte avec le début du fichier WAV. Il faut alors faire la chose suivante:

* supprimer tout alignement existant (menu edit->clear all align)
* s'il n'y en a pas déjà, ajouter un "mot", ou plutôt un symbole, qui sera considéré comme un bruit, au
tout début de la transcription. Le "mot" qu'il faut ajouter dépend des expressions régulières qui sont
définies pour reconnaître les bruits, mais par défaut, on peut ajouter par exemple les 3 caractères ###
* ensuite reparser le texte pour que le système comprenne qu'il y a un "bruit" au début du fichier:
les 3 caractères ### doivent alors être surlignés en bleu clair.
* puis cliquer sur le premier vrai mot du texte, juste après les 3 caractères ###:
un menu apparaît sous la souris, sélectionner "set Time deb"
* Ce menu vous permet de rentrer manuellement le temps de début, en secondes depuis le début du fichier WAV,
du mot sélectionné. Une fenêtre vous demande de rentrer alors le nombre de secondes, puis tous les mots
encore non alignés précédants (et en particulier le bruit ###) sont alors "équi-alignés" automatiquement,
c'est-à-dire que l'intervale de temps non aligné précédent est divisé en parts égales, une pour chaque mot précédent.
Dans notre cas, il n'y a qu'un "mot", le bruit ###, dont la durée est donc fixée à la zone du fichier WAV qui ne doit
pas être alignée.
* Vous pouvez ensuite lancer l'auto-aligner, qui reprendra au bon endroit du fichier WAV

