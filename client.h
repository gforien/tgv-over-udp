#ifndef CLIENT_H_  
#define CLIENT_H_
#endif

int three_way_handshake(int socket, struct sockaddr_in adresse);
void acknowledge(unsigned char seq_recu[], int socket, struct sockaddr_in adresse);