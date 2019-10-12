#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include "client.h"

#define RCVSIZE 1024

// ( ! ) dans rcvfrm() il faut bien donner un pointeur vers une variable i = sizeof(adresse)
//       et non pas la taille elle-même parce qu'on ne la connait pas à l'avance

int main (int argc, char *argv[]) {

    int port = 5001;
    char strAdresse[] = "000.000.000.000";
    char msg[RCVSIZE];
    char blanmsg[RCVSIZE];

    // On gère les arguments
    switch (argc) {
        default:
            printf("syntax: ./client <ip_serveur> <port_serveur>\n");
            exit(1);
            break;;

        case 2:
            sprintf(strAdresse, "%s", argv[1]);
            break;;

        case 3:
            sprintf(strAdresse, "%s", argv[1]);
            port = atoi(argv[2]);
            break;;
    }
    printf("Connecting to ip %s on port %d\n", strAdresse, port);


    /************************************************************************/
    /*                          1. SOCKET INITIALE
    /************************************************************************/
    // On crée notre socket "vide"
    struct sockaddr_in adresse;
    socklen_t alen= sizeof(adresse);
    int valid = 1;
    memset(&adresse, 0, sizeof(adresse));
    adresse.sin_family= AF_INET;
    adresse.sin_port= htons(port);
    adresse.sin_addr.s_addr = inet_addr(strAdresse);

    // On initalise le descripteur
    int server_desc = socket(AF_INET, SOCK_DGRAM, 0);
    if (server_desc < 0) {
        perror("cannot create socket\n");
        return -1;
    }
    setsockopt(server_desc, SOL_SOCKET, SO_REUSEADDR, &valid, sizeof(int));
    // On ne bind pas pour le client
    // On est en UPD, on n'appelle pas listen()
    


    /************************************************************************/
    /*                          2. THREE WAY HANDSHAKE + NOUVELLE SOCKET
    /************************************************************************/
    int i_newport = three_way_handshake(server_desc, adresse);
    if (i_newport <0){
        printf("main(): TWH échoué -> exit\n");
        return -1;
    }
    else {
        printf("main(): port reçu par TWH = %d\n", i_newport);
    }

    // on initialise la nouvelle socket et on ne bind pas pour le client
    adresse.sin_port= htons(i_newport);

    int i_socket_new;
    if ( (i_socket_new = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("main(): erreur sur l'ouverture du descripteur");
        exit(-1);
    }
    setsockopt(i_socket_new, SOL_SOCKET, SO_REUSEADDR, &valid, sizeof(int));
    printf("main(): descripteur de fichier ouvert sur le port %d\n", i_newport);


    /************************************************************************/
    /*                          3. RECEPTION DU FICHIER
    /************************************************************************/
    FILE *file;
    file = fopen("./fichierRECU", "wb");
    if (file == NULL) {
        perror("main(): erreur à l'ouverture du fichier");
        exit(-1);
    }
    
    size_t n, m;
    unsigned char buffer[RCVSIZE];
    // int cwnd = 8192;

/*    //for(int i=0; i++; i<5) {  
    while(1) {
        fgets(msg, RCVSIZE, stdin);

        sendto(i_socket_new, msg, RCVSIZE, 0, (struct sockaddr*)&adresse, sizeof(adresse));
        //recvfrom(server_desc, blanmsg, RCVSIZE, 0, (struct sockaddr*)&adresse, sizeof(adresse));
        //printf("%s",blanmsg);
    }*/
    printf("main(): fin du for\n");

    do {
        printf("main(): dans le du while\n");
        n = recvfrom(i_socket_new, buffer, RCVSIZE, 0, (struct sockaddr*)&adresse, &alen);
        printf("%d octets reçus\n", n);
        if (n) {
            m = fwrite(buffer, 1, n, file);
            printf("%d octets écrits\n", m);
        } else {
            m = 0;
            printf("m = 0\n");
        }
    } while ((n>0) && (n==m));
        printf("main(): fin du while\n");

    if (m) {
        printf("m=%d\n",m);
        perror("main(): erreur à la fin du while");
    }




    printf("main(): on ferme les descripteurs\n");
    close(server_desc);
    close(i_socket_new);
    printf("main(): exit\n");
    return 0;
}


int three_way_handshake(int socket, struct sockaddr_in adresse){
    printf("three_way_handshake(): début\n");
    socklen_t alen= sizeof(adresse);

    // on envoie SYN
    if (sendto(socket, "SYN", 3, 0, (struct sockaddr*)&adresse, sizeof(adresse)) <0){
        perror("three_way_handshake(): SYN non-envoyé");
    }
    
    // on doit recevoir SYN-ACK 8000
    char msg_complet[14];
    char synack[8];
    if (recvfrom(socket, msg_complet, 14, 0, (struct sockaddr*)&adresse, &alen) <0) {
        perror("three_way_handshake(): SYN-ACK 8000");
 
    // on récupère le début du message uniquement
    } else {
        strncpy(synack, msg_complet, 7);
        synack[7] = '\0';
        if (strcmp(synack, "SYN-ACK") != 0) {
            printf("three_way_handshake(): problème avec le message reçu (SYN-ACK = %s)\n", synack);
            return -1;
        }
    }

    //on récupère le numéro de port
    char s_port[6] = "     ";
    // (!) Attention si on n'a pas reçu un message de 13 caractères ça ne marchera pas
    strncpy(s_port, msg_complet+8, 5);
    printf("s_port : %s et synack + 5 = %s\n", s_port, synack+5);

    // on envoie ACK
    if (sendto(socket, "ACK", 3, 0, (struct sockaddr*)&adresse, sizeof(adresse)) <0){
        perror("three_way_handshake(): ACK non-envoyé");
    }
    
    printf("three_way_handshake(): return 0\n");
    return atoi(s_port);
}