package plugins.speechreco.adaptation;

/**
 * Une adaptation orginale, lorsqu'il y a très peu de données d'adaptation:
 * combinaison de instance-based models + GMM
 * L'idée générale est d'avoir 2 modèles en compétition pour classer chaque trame:
 * MOD1 qui s'appuie sur les trames déjà vues
 * MOD2 qui est le SI-GMM associé à un état particulier des 3-ph
 * Chaque modèle fournit une confiance en sa classification sous la forme d'un posterior.
 * La combinaison est réalisée par un module qui réalise une alpha-intégration de ces 2 distributions
 * sachant un prior qui donne gagnant MOD1 ou MOD2 selon la quantité d'info d'adaptation déjà vue.
 * 
 * 
 * Une autre approche est de concevoir un unique modèle qui sort une distrib de probas sur les états des HMMs
 * en fonction de: (1) les log-likes des GMMs; (2) la distance minimale aux segments précédents;
 * (3) le nb de segments de cet état du HMM vus
 * 
 * - Ce modèle doit être entraîné sur les quelques secondes de parole déjà vu.
 * - Il faut que ce modèle généralise les HMMs de base
 * 
 * @author xtof
 *
 */

public class CombinedIBL {

}
