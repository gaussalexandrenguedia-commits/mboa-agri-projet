package com.example.data

data class CropDiagnostic(
    val plantName: String,
    val diseaseName: String,
    val confidence: Int,
    val symptoms: String,
    val treatmentLocal: String,
    val treatmentChemical: String
)

object CropDiagnostics {
    val items = listOf(
        CropDiagnostic(
            plantName = "Cacao",
            diseaseName = "Pourriture brune des cabosses (Phytophthora)",
            confidence = 94,
            symptoms = "Taches brunes circulaires et humides sur les cabosses de cacao qui s'étendent rapidement, suivies d'un feutrage blanc poudreux désagréable.",
            treatmentLocal = "1. Élaguer l'arbre pour augmenter le passage de la lumière et réduire l'humidité.\n2. Retirer régulièrement les cabosses infectées de l'arbre et les enfouir profondément sous terre hors du champ.\n3. Appliquer une décoction froide de cendres de bois de cuisine mélangée à du savon noir local sur les troncs pour former une barrière naturelle.",
            treatmentChemical = "En cas de forte infestation, appliquer après la saison des pluies un fongicide à base d'oxyde de cuivre ou de métalaxyl-M (dosé exactement à 50g pour 15L d'eau), en portant des équipements de protection individuelle complets."
        ),
        CropDiagnostic(
            plantName = "Manioc",
            diseaseName = "Mosaïque africaine du manioc (ACMD)",
            confidence = 96,
            symptoms = "Malformations sévères des feuilles qui s'enroulent sur elles-mêmes, taches claires jaunes et vertes en motif mosaïque, entraînant une réduction drastique du rendement en tubercules de manioc.",
            treatmentLocal = "1. Utiliser exclusivement des boutures saines prélevées sur des plants vigoureux n'ayant aucun symptôme.\n2. Arracher complètement et brûler les plants malades dès l'apparition des premiers symptômes pour éviter la propagation par les mouches blanches.\n3. Cultiver des variétés locales résistantes recommandées par l'IRAD (Cameroun).",
            treatmentChemical = "Il n'existe aucun traitement chimique direct contre les virus des plantes. Les traitements chimiques visent uniquement le contrôle des mouches blanches vectrices par pulvérisation d'insecticides naturels à base d'huile d'insecte ou de savon doux de potassium."
        ),
        CropDiagnostic(
            plantName = "Caféier",
            diseaseName = "Rouille orangée (Hemileia vastatrix)",
            confidence = 91,
            symptoms = "Plaques poudreuses jaune-orange vif caractéristiques sur la face inférieure des feuilles de caféier, entraînant une chute précoce des feuilles et l'affaiblissement complet de la plante.",
            treatmentLocal = "1. Améliorer la ventilation en ajustant l'ombrage des arbres de protection.\n2. Nettoyer régulièrement la base des caféiers et enrichir le sol en compost bien décomposé pour booster leur défenses naturelles.\n3. Asperger une infusion d'ail concentrée (3 têtes d'ail broyées et macérées dans 1L d'eau chaude) diluée à 10% sur les feuilles.",
            treatmentChemical = "Pulvériser un fongicide cuprique préventif (comme l'hydroxyde de cuivre, à hauteur de 30-45g pour un pulvérisateur à dos de 15L) dès l'apparition des premières pluies de la saison."
        ),
        CropDiagnostic(
            plantName = "Bananier / Plantain",
            diseaseName = "Cercosporiose noire (Maladie de Sigatoka)",
            confidence = 93,
            symptoms = "Fines stries sombres allongées parallèles aux nervures foliaires qui mûrissent en de grandes taches brunes d'aspect desséché et brûlé, réduisant le calibre des régimes de bananes plantain.",
            treatmentLocal = "1. Couper et détruire immédiatement par le feu ou enfouissement les parties nécrosées des feuilles inférieures.\n2. Maintenir une bonne fertilisation en appliquant un paillage riche en feuilles mortes sèches exemptes de la maladie et de la cendre de bois riche en potassium.\n3. Optimiser la densité de plantation (minimum 2.5m de distance entre les rejets).",
            treatmentChemical = "L'application de fongicides systémiques (triazoles ou strobilurines) peut être envisagée en alternance pour éviter la résistance, dosée précisément de 10 à 15 ml par pulvérisateur de 15L sous le contrôle d'un conseiller agronomique de zone."
        ),
        CropDiagnostic(
            plantName = "Maïs",
            diseaseName = "Sclérostriose / Mildiou du maïs",
            confidence = 88,
            symptoms = "Apparition de longues stries blanchâtres à vert-jaune parallèles le long des limbes des feuilles de maïs, s'accompagnant souvent d'un fin duvet blanc poudreux sur la face inférieure par matinée humide.",
            treatmentLocal = "1. Pratiquer une rotation culturale stricte d'au moins 2 ans avec des cultures non céréalières (comme l'arachide, le soja ou le manioc).\n2. Ramasser et enfouir tous les résidus de récolte après chaque moisson pour éliminer les oospores du sol.\n3. Semer aux dates recommandées pour que le jeune plant se développe en dehors de la période de forte humidité propice aux spores.",
            treatmentChemical = "Le traitement de semence préventif au métalaxyl (comme Apron Star) offre une barrière de protection excellente pour le jeune plant de maïs au Cameroun."
        )
    )
}
