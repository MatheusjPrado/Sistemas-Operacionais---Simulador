# Simulador de Escalonamento de Processos

Trabalho de Sistemas Operacionais em Java. O programa le o CSV de processos, simula 3 escalonadores e imprime metricas, tabela por processo e diagrama de Gantt.

## Como rodar no IntelliJ

1. File > Open e escolha esta pasta (`trabalhoSO`).
2. Se pedir, marque como projeto / sources a pasta `src`.
3. Abra `src/Main.java` e clique em Run.
4. A saida aparece no console e tambem no arquivo `resultados.txt`.

Se o IntelliJ nao achar os CSV, confira se a Working directory do Run e a pasta do projeto (nao a pasta `src`).

Pelo terminal, na pasta do projeto:

```
javac -encoding UTF-8 src/*.java -d out
java -cp out Main
```

## Arquivos

- `src/Processo.java` - dados e estado de um processo
- `src/Simulador.java` - o relogio da simulacao e os 3 algoritmos
- `src/Main.java` - le CSV, roda os 3 metodos e imprime
- `dados/processos_entrada_correlacionados.csv` - entrada obrigatoria
- `dados/historico_escalonamento_2000_correlacionado.csv` - historico opcional

## Como a simulacao funciona

O tempo anda de 1 em 1. A cada instante:

1. chegam processos novos
2. processos que terminaram E/S voltam para pronto
3. se a CPU estiver livre, escolhe o proximo
4. o escolhido usa a CPU por 1 unidade
5. se acabou a CPU, finaliza; se pediu E/S, bloqueia; se o quantum acabou, volta para a fila

Estados: `NAO_CHEGOU`, `PRONTO`, `EXECUTANDO`, `BLOQUEADO`, `FINALIZADO`.

## E/S (igual nos 3 algoritmos)

Se `operacao_es = 1`, o processo faz algumas paradas de E/S.

Quantas paradas: `round(media_es)`, no minimo 1.
Quando: o CPU e dividido em fatias. Depois de cada fatia (menos a ultima), ele bloqueia por `duracao_es`.

Exemplo: CPU 23 e media_es 3.8 -> 4 E/S. Fatia = 23 / 5 = 4. Pede E/S depois de usar 4, 8, 12 e 16 de CPU.

Nao usamos probabilidade aleatoria, para o resultado ser sempre o mesmo.

## Algoritmo 1 - Round-Robin

- Uma fila unica de prontos.
- Quantum fixo = 4.
- Quem usa os 4 sem terminar vai para o fim da fila.
- Quem termina libera a CPU na hora.
- Quem pede E/S sai da CPU; quando a E/S acaba, entra no fim da fila.

## Algoritmo 2 - Multiplas Filas

| Fila | Tipo | Politica | Quantum |
|------|------|----------|---------|
| 1 | tempo_real e interativo | RR | 2 |
| 2 | io_bound e misto | RR | 4 |
| 3 | cpu_bound e batch | RR | 8 |

- Sempre roda a fila 1 se ela tiver alguem. Senao a 2. Senao a 3.
- Preempcao: se chega (ou volta) um processo de fila melhor, o da fila pior sai na hora.
- Anti-inanicao: se um processo espera mais de 30 na fila, sobe uma fila (3 -> 2, ou 2 -> 1).

## Algoritmo 3 - Prioridade com Envelhecimento

Metodo proposto pelo grupo.

- Escolha: pega o pronto com menor prioridade efetiva.
- Prioridade efetiva = prioridade do CSV - (espera / 10). Quanto mais espera, mais urgente fica.
- Empate: primeiro tempo_real/interativo; depois quem esperou mais.
- Quantum conforme o tipo: 2, 4 ou 8.
- E/S igual aos outros.
- Processos interativos tem preferencia no desempate e quantum menor (respondem mais rapido).
- Anti-inanicao: o envelhecimento impede que alguem fique para sempre no fim.

O historico foi usado so para observar que as decisoes antigas tambem favoreciam quem esperou muito e processos sensiveis. A regra do metodo proposto segue essa ideia, mas nao copia o historico.

Limitacao: o "10" do envelhecimento e um valor fixo. Processos longos ainda podem atrasar um pouco os curtos.

## Metricas

```
tempo_retorno  = tempo_conclusao - tempo_chegada
tempo_espera   = tempo_retorno - tempo_cpu_total - tempo_total_em_io
tempo_resposta = primeiro_tempo_em_cpu - tempo_chegada
```
