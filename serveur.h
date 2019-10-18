#ifndef SERVEUR_H_  
#define SERVEUR_H_
#endif

int three_way_handshake(int socket, struct sockaddr_in adresse);
void seq_plus_plus(unsigned char seq[]);
int receive_ack(unsigned char seq_envoye[], int socket, struct sockaddr_in adresse);