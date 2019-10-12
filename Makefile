BINARY=client serveur
FLAGS= -Wall -Wextra -pedantic -std=c11

all: $(BINARY)

serveur: serveur.o
	gcc $(FLAGS) $^ -o serveur


client: client.o
	gcc $(FLAGS) $^ -o client

%.o: %.c
	gcc -c $<

clean:
	rm -f $(BINARY) *.o

rebuild: clean all