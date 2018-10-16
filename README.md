 # Middleware Project - Pay2bid
Distributed auction house application - Middleware project, M2 ALMA 2016/2017   
**Auteurs** : Alexis Giraudet, Arnaud Grall, Thomas Minier
**Contributeurs** : Aurélien Brisseau, Théo Dolez, Laurent Girard, Florent Mercier

# Prerequisites
* Java version : 1.6 or newer
* Maven

# Installation

Navigate into the project directory and build it using Maven
```
cd pay2bid/
mvn compile
mvn package assembly:single
```

# Launch the Server
```
java -jar target/pay2bid-1.0-SNAPSHOT-jar-with-dependencies.jar -l
```

# Launch a Client
```
java -jar target/pay2bid-1.0-SNAPSHOT-jar-with-dependencies.jar
```

# TO DO

- [x] Le client proposant l'enchère ne devrait pas pouvoir bid sur celle-ci
- [x] Pour le vendeur, les champs pour bid réapparaissent à chaque fin de nouveau round, on devrait rien voir
- [x] identificateurs pour les ≠ clients
- [ ] séparation par thèmes
- [x] qui gagne en cas d'égalité -> Random selon le hashcode
- [x] enchère négatives permises
- [x] anciennes enchères se relancent avec les nouvelles --> fieldtext de l'ancienne enchère réapparait
- [x] finir l'enchère quand tout le monde a bid sans attendre fin du timer -> TODO : enlever le timer pour le vendeur (qui ne peut pas bid maintenant)
- [x] gestion des déconnexions pendant enchère --> timeElapsed non appelé par le client qui se déconnecte, l'enchère ne s'arrête jamais
- [x] durant l'enchère on ne sait pas qui l'emporte
- [x] erreur si personne ne bid au premier tour
- [ ] correction options host et port
- [x] catch l'exception lorsque le client est lancé sans serveur
- [x] Erreur quand les clients raise un prix inférieur à celui en cours
- [x] estVendeur géré uniquement localement si plusieurs enchères dans la queue on ne sait plus qui est vendeur 

