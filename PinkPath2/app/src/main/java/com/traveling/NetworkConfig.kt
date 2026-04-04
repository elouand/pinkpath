package com.traveling



object NetworkConfig {

    // Tu n'as qu'UNE SEULE ligne à modifier ici quand tu changes de lieu

    private const val IP_ADDRESS = "192.168.1.25"

    private const val PORT = "3000"


    const val BASE_URL = "http://$IP_ADDRESS:$PORT/api/"



    // Utile pour afficher les images/audios dans TravelShare

    const val UPLOADS_URL = "http://$IP_ADDRESS:$PORT/uploads/"

}