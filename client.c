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

// ( ! ) dans rcvfrm() il faut bien donner un pointeur vers une variable i = sizeof(s_servaddr)
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
    printf("main(): connecting to ip %s on port %d\n", strAdresse, port);



    /************************************************************************/
    /*                          1. SOCKET INITIALE
    /************************************************************************/
    // On crée l'adresse du serveur 127.0.0.1:5001
    struct sockaddr_in s_servaddr;
    socklen_t t_servaddrlen= sizeof(s_servaddr);
    int i_1 = 1;
    memset(&s_servaddr, 0, sizeof(s_servaddr));
    s_servaddr.sin_family= AF_INET;
    s_servaddr.sin_port= htons(port);
    s_servaddr.sin_addr.s_addr = inet_addr(strAdresse);

    // On initalise la socket
    int i_socketfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (i_socketfd < 0) {
        perror("cannot create socket\n");
        exit(-1);
    }
    setsockopt(i_socketfd, SOL_SOCKET, SO_REUSEADDR, &i_1, sizeof(int));

    // On ne bind pas pour le client
    // On est en UPD, on n'appelle pas listen()



    /************************************************************************/
    /*                2. THREE WAY HANDSHAKE + NOUVELLE SOCKET
    /************************************************************************/
    int i_newport = three_way_handshake(i_socketfd, s_servaddr);
    if (i_newport <0){
        printf("main(): TWH échoué -> exit\n");
        exit(-1);
    }
    else {
        printf("main(): port reçu par TWH = %d\n", i_newport);
    }

    // on initialise la nouvelle socket sur le port 8000
    s_servaddr.sin_port= htons(i_newport);

    int i_socket_new;
    if ( (i_socket_new = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("main(): erreur sur l'ouverture du descripteur");
        exit(-1);
    }
    setsockopt(i_socket_new, SOL_SOCKET, SO_REUSEADDR, &i_1, sizeof(int));
    printf("main(): descripteur de fichier ouvert sur le port %d\n", i_newport);

    // on ne bind pas pour le client



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

    printf("main(): ecris 5 messages pour le serveur\n");
    for(int i=0; i<5; i++) {
        fgets(msg, RCVSIZE, stdin);
        sendto(i_socket_new, msg, RCVSIZE, 0, (struct sockaddr*)&s_servaddr, sizeof(s_servaddr));
        //recvfrom(i_socketfd, blanmsg, RCVSIZE, 0, (struct sockaddr*)&s_servaddr, sizeof(s_servaddr));
        //printf("%s",blanmsg);
    }
    printf("main(): fin de la boucle for\n");



/*
    struct timeval tv;
    tv.tv_sec = 0;
    tv.tv_usec = 100000;
    // if (setsockopt(rcv_sock, SOL_SOCKET, SO_RCVTIMEO,&tv,sizeof(tv)) < 0) {
        perror("Error");
    }

    do {
        printf("main(): dans le du while\n");
        n = recvfrom(i_socket_new, buffer, RCVSIZE, 0, (struct sockaddr*)&s_servaddr, &t_servaddrlen);
        printf("%d octets reçus\n", (int)n);
        if (n) {
            m = fwrite(buffer, 1, n, file);
            printf("%d octets écrits\n", (int)m);
        } else {
            m = 0;
            printf("m = 0\n");
        }
    } while ((n>0) && (n==m));
        printf("main(): fin du while\n");

    if (m) {
        printf("m=%d\n",(int)m);
        perror("main(): erreur à la fin du while");
    }*/




    printf("main(): on ferme les descripteurs\n");
    close(i_socketfd);
    close(i_socket_new);
    printf("main(): exit\n");
    return 0;
}


int three_way_handshake(int socket, struct sockaddr_in adresse){
    printf("three_way_handshake(): début\n");
    socklen_t addrlen= sizeof(adresse);

    // on envoie SYN
    if (sendto(socket, "SYN", 3, 0, (struct sockaddr*)&adresse, sizeof(adresse)) <0){
        perror("three_way_handshake(): SYN non-envoyé");
    }

    // on doit recevoir SYN-ACK 8000
    char msg_complet[14];
    char synack[8];
    if (recvfrom(socket, msg_complet, 14, 0, (struct sockaddr*)&adresse, &addrlen) <0) {
        perror("three_way_handshake(): SYN-ACK 8000");

    // on récupère le début du message uniquement
    } else {
        strncpy(synack, msg_complet, 7);
        synack[7] = '\0';
        if (strcmp(synack, "SYN-ACK") != 0) {
            printf("three_way_handshake(): problème avec le message reçu (SYN-ACK = %s)\n", synack);
            exit(-1);
        }
    }

    //on récupère le numéro de port
    char port[6] = "     ";
    // (!) Attention si on n'a pas reçu un message de 13 caractères ça ne marchera pas
    strncpy(port, msg_complet+8, 5);

    // on envoie ACK
    if (sendto(socket, "ACK", 3, 0, (struct sockaddr*)&adresse, sizeof(adresse)) <0){
        perror("three_way_handshake(): ACK non-envoyé");
    }

    printf("three_way_handshake(): return %d\n", atoi(port));
    return atoi(port);
}