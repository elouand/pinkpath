package com.traveling



object NetworkConfig {

    // Adresse IP mise à jour d'après hostname -I

    private const val IP_ADDRESS = "192.168.1.68"

    private const val PORT = "3000"


    const val BASE_URL = "http://$IP_ADDRESS:$PORT/api/"



    // Utile pour afficher les images/audios dans TravelShare

    const val UPLOADS_URL = "http://$IP_ADDRESS:$PORT/uploads/"

}