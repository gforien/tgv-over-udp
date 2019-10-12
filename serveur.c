#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <netinet/in.h>
#include "serveur.h"

#define MAXBUFFER 65535

// ( ! ) dans rcvfrm() il faut bien donner un pointeur vers une variable i = sizeof(adresse)
//       et non pas la taille elle-même parce qu'on ne la connait pas à l'avance

int main (int argc, char *argv[]) {
    printf("main(): début\n");

    // On gère les arguments
    int i_port= 5001;
    if (argc == 2) {
        i_port = atoi(argv[1]);
    }
    else {
        printf("main(): problème avec les arguments reçus\n");
        exit(-1);
    }


    // On crée notre socket "vide"
    struct sockaddr_in s_address;
    int i_valid = 1;
    memset(&s_address, 0, sizeof(s_address));
    s_address.sin_family= AF_INET;
    s_address.sin_port= htons(i_port);
    s_address.sin_addr.s_addr= htonl(INADDR_ANY);


    // On initalise le descripteur
    int i_socket_fd;
    if ( (i_socket_fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("main(): erreur sur l'ouverture du descripteur");
        exit(-1);
    }
    setsockopt(i_socket_fd, SOL_SOCKET, SO_REUSEADDR, &i_valid, sizeof(int));
    printf("main(): descripteur de fichier ouvert sur le port %d\n", i_port);


    // On bind le tout
    if (bind(i_socket_fd, (struct sockaddr*) &s_address, sizeof(s_address)) == -1) {
        perror("main(): erreur sur le bind");
        close(i_socket_fd);
        exit(-1);
    }
    printf("main(): socket bindé au descripteur %d\n", i_port);
    // On est en UPD, on n'appelle pas listen()


    // paramètres de la socket client
    struct sockaddr_in s_client;
    memset(&s_client, 0, sizeof(s_client));
    socklen_t alen= sizeof(s_client);




    /************************************************************************/
    /*                          2. THREE WAY HANDSHAKE + NOUVELLE SOCKET
    /************************************************************************/

    if (three_way_handshake(i_socket_fd, s_client) <0){
        printf("main(): three_way_handshake pas correctement exécuté\n");
        exit(-1);
    } else {
        printf("main(): three_way_handshake correctement exécuté\n");
    }

    s_address.sin_port= htons(8000);
    // On initalise la nouvelle socket
    int i_socket_new;
    if ( (i_socket_new = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        perror("main(): erreur sur l'ouverture du descripteur");
        exit(-1);
    }
    setsockopt(i_socket_new, SOL_SOCKET, SO_REUSEADDR, &i_valid, sizeof(int));
    printf("main(): descripteur de fichier ouvert sur le port %d\n", 8000);

    // On bind le tout
    if (bind(i_socket_new, (struct sockaddr*) &s_address, sizeof(s_address)) == -1) {
        perror("main(): erreur sur le bind");
        close(i_socket_new);
        exit(-1);
    }
    printf("main(): nouvelle socket bindée au descripteur %d\n", 8000);



    /************************************************************************/
    /*                          3. ENVOI DU FICHIER
    /************************************************************************/
    FILE *file;
    file = fopen("./13Mofile", "rb");
    if (file == NULL) {
        perror("main(): erreur à la lecture du fichier");
        exit(-1);
    }

    FILE *file2;
    file2 = fopen("./fichierRECU", "wb");
    if (file2 == NULL) {
        perror("main(): erreur à l'ouverture du fichier");
        exit(-1);
    }

    size_t n, m;
    unsigned char buffer[MAXBUFFER];
    int cwnd = 8192;

    //for(int i=0; i<5; i++) { 
/*    while(1) {
       int msg = recvfrom(i_socket_new, buffer, sizeof(buffer), 0,(struct sockaddr*)&s_client, &alen);
        //sendto(i_socket_fd , buffer, strlen(buffer), 0, (struct sockaddr*)&s_client, alen);
        printf("%s\n", buffer);
        //s = s*2;
    } */

    printf("main(): début du while\n");
    do {
        n = fread(buffer, 1, cwnd, file);
        printf("%d octets lus\n", n);
        if (n) {
            // m = fwrite(buffer, 1, n, file2);
            m = sendto(i_socket_new, buffer, n, 0, (struct sockaddr*)&s_client, sizeof(s_client));
            printf("%d octets envoyés\n", m);
        } else {
            m = 0;
            printf("m = 0\n");
        }
    } while ((n>0) && (n==m));
    if (m) {
        printf("m=%d\n",m);
        perror("main(): erreur à la fin du while");
    }
        
 
    printf("main(): on ferme les descripteurs\n");
    close(i_socket_fd);
    close(i_socket_new);
    printf("main(): exit\n");
    return 0;
}


int three_way_handshake(int socket, struct sockaddr_in adresse) {
    printf("three_way_handshake(): début\n");

    socklen_t alen= sizeof(adresse);

    // on reçoit SYN
    char syn[4] = "   ";
    if (recvfrom(socket, syn, 3, 0, (struct sockaddr*)&adresse, &alen) <0) {
        perror("three_way_handshake(): SYN non-reçu");
    }

    // si c'est pas un vrai SYN
    if (strcmp(syn, "SYN") != 0) {
        printf("three_way_handshake(): problème avec le message reçu (SYN = %s)\n", syn);
    }

    // si c'est bien un SYN, on envoie SYN-ACK
    // (!) Attention on doit bien envoyer un message de 13 caractères
    char synack[14] = "SYN-ACK 8000 ";
    if (sendto(socket, synack, 14, 0, (struct sockaddr*)&adresse, sizeof(adresse)) <0) {
        perror("three_way_handshake(): SYN-ACK non-envoyé");
    }

    // si c'est bien envoyé, on doit recevoir ACK
    char ack[4] = "   ";
    if (recvfrom(socket, ack, 3, 0, (struct sockaddr*)&adresse, &alen) <0) {
        perror("three_way_handshake(): ACK non-reçu");
    } else if (strcmp(ack, "ACK") != 0) {
        printf("three_way_handshake(): problème avec le message reçu (ACK = %s)\n", ack);
    }

    printf("three_way_handshake(): return 0\n");
    return 0;
}