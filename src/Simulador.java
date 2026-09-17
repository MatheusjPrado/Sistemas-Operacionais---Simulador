import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Simulador {
    public static final int ROUND_ROBIN = 1;
    public static final int MULTIPLAS_FILAS = 2;
    public static final int PROPOSTO = 3;

    public static Resultado executar(List<Processo> originais, int modo) {
        List<Processo> processos = new ArrayList<>();
        for (Processo original : originais) {
            processos.add(original.copiar());
        }

        LinkedList<Processo>[] filas = criarFilas();
        Resultado resultado = new Resultado();
        resultado.nome = nomeDoModo(modo);
        resultado.processos = processos;

        Processo cpu = null;
        int quantumRestante = 0;
        String ultimoPid = null;
        int tempo = 0;
        int limite = 1000000;

        while (tempo <= limite) {
            if (todosFinalizados(processos)) {
                break;
            }

            chegar(processos, tempo, modo, filas);
            desbloquear(processos, tempo, modo, filas);

            if (modo == MULTIPLAS_FILAS && cpu != null && precisaPreemptar(cpu, filas)) {
                cpu.estado = "PRONTO";
                colocarNaFila(cpu, modo, filas);
                cpu = null;
                quantumRestante = 0;
            }

            if (cpu == null) {
                cpu = escolher(modo, filas, processos);
                if (cpu != null) {
                    cpu.estado = "EXECUTANDO";
                    cpu.esperaAtual = 0;
                    if (cpu.tempoInicio < 0) {
                        cpu.tempoInicio = tempo;
                    }
                    quantumRestante = quantumDe(cpu, modo);
                    if (ultimoPid == null || !ultimoPid.equals(cpu.pid)) {
                        resultado.trocasContexto++;
                        ultimoPid = cpu.pid;
                    }
                }
            }

            if (cpu == null) {
                registrarGantt(resultado.gantt, tempo, "OCIOSO");
                incrementarEspera(processos);
                promoverSeNecessario(modo, processos, filas);
                tempo++;
                continue;
            }

            registrarGantt(resultado.gantt, tempo, cpu.pid);
            cpu.cpuRestante--;
            cpu.cpuExecutado++;
            quantumRestante--;

            incrementarEspera(processos);
            promoverSeNecessario(modo, processos, filas);

            int depois = tempo + 1;
            if (cpu.cpuRestante == 0) {
                cpu.estado = "FINALIZADO";
                cpu.tempoConclusao = depois;
                cpu = null;
            } else if (cpu.deveFazerES()) {
                cpu.estado = "BLOQUEADO";
                cpu.ioFim = depois + cpu.duracaoES;
                cpu.totalIO += cpu.duracaoES;
                cpu = null;
            } else if (quantumRestante <= 0) {
                cpu.estado = "PRONTO";
                colocarNaFila(cpu, modo, filas);
                cpu = null;
            }

            tempo = depois;
        }

        resultado.tempoFinal = tempo;
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<Processo>[] criarFilas() {
        LinkedList<Processo>[] filas = new LinkedList[4];
        filas[1] = new LinkedList<>();
        filas[2] = new LinkedList<>();
        filas[3] = new LinkedList<>();
        return filas;
    }

    private static String nomeDoModo(int modo) {
        if (modo == ROUND_ROBIN) {
            return "Round-Robin (quantum 4)";
        }
        if (modo == MULTIPLAS_FILAS) {
            return "Multiplas Filas";
        }
        return "Prioridade com Envelhecimento";
    }

    private static boolean todosFinalizados(List<Processo> processos) {
        for (Processo p : processos) {
            if (!p.estado.equals("FINALIZADO")) {
                return false;
            }
        }
        return true;
    }

    private static void chegar(List<Processo> processos, int tempo, int modo, LinkedList<Processo>[] filas) {
        for (Processo p : processos) {
            if (p.estado.equals("NAO_CHEGOU") && p.chegada == tempo) {
                p.estado = "PRONTO";
                p.filaAtual = p.filaDoTipo();
                p.esperaAtual = 0;
                colocarNaFila(p, modo, filas);
            }
        }
    }

    private static void desbloquear(List<Processo> processos, int tempo, int modo, LinkedList<Processo>[] filas) {
        for (Processo p : processos) {
            if (p.estado.equals("BLOQUEADO") && p.ioFim == tempo) {
                p.estado = "PRONTO";
                p.esperaAtual = 0;
                colocarNaFila(p, modo, filas);
            }
        }
    }

    private static void colocarNaFila(Processo p, int modo, LinkedList<Processo>[] filas) {
        if (modo == ROUND_ROBIN) {
            filas[1].add(p);
        } else if (modo == MULTIPLAS_FILAS) {
            filas[p.filaAtual].add(p);
        }
    }

    private static Processo escolher(int modo, LinkedList<Processo>[] filas, List<Processo> processos) {
        if (modo == ROUND_ROBIN) {
            return filas[1].poll(); // primeiro da fila unica
        }
        if (modo == MULTIPLAS_FILAS) {
            if (!filas[1].isEmpty()) {
                return filas[1].poll(); // sempre a fila mais prioritaria
            }
            if (!filas[2].isEmpty()) {
                return filas[2].poll();
            }
            return filas[3].poll();
        }

        Processo melhor = null;
        for (Processo p : processos) {
            if (!p.estado.equals("PRONTO")) {
                continue;
            }
            if (melhor == null || eMelhor(p, melhor)) {
                melhor = p;
            }
        }
        return melhor;
    }

    private static boolean eMelhor(Processo a, Processo b) {
        int pa = a.prioridadeEfetiva();
        int pb = b.prioridadeEfetiva();
        if (pa != pb) {
            return pa < pb;
        }
        if (a.ehSensivel() != b.ehSensivel()) {
            return a.ehSensivel();
        }
        if (a.esperaAtual != b.esperaAtual) {
            return a.esperaAtual > b.esperaAtual;
        }
        return a.chegada < b.chegada;
    }

    private static int quantumDe(Processo p, int modo) {
        if (modo == ROUND_ROBIN) {
            return 4;
        }
        if (modo == MULTIPLAS_FILAS) {
            if (p.filaAtual == 1) {
                return 2;
            }
            if (p.filaAtual == 2) {
                return 4;
            }
            return 8;
        }
        return p.quantumDoTipo();
    }

    private static boolean precisaPreemptar(Processo cpu, LinkedList<Processo>[] filas) {
        if (cpu.filaAtual >= 2 && !filas[1].isEmpty()) {
            return true;
        }
        return cpu.filaAtual >= 3 && !filas[2].isEmpty();
    }

    private static void incrementarEspera(List<Processo> processos) {
        for (Processo p : processos) {
            if (p.estado.equals("PRONTO")) {
                p.esperaAtual++;
            }
        }
    }

    private static void promoverSeNecessario(int modo, List<Processo> processos, LinkedList<Processo>[] filas) {
        if (modo != MULTIPLAS_FILAS) {
            return;
        }
        for (Processo p : processos) {
            if (p.estado.equals("PRONTO") && p.esperaAtual > 30 && p.filaAtual > 1) {
                filas[p.filaAtual].remove(p);
                p.filaAtual--;
                p.esperaAtual = 0;
                filas[p.filaAtual].add(p);
            }
        }
    }

    private static void registrarGantt(List<TrechoGantt> gantt, int tempo, String pid) {
        if (!gantt.isEmpty()) {
            TrechoGantt ultimo = gantt.get(gantt.size() - 1);
            if (ultimo.pid.equals(pid) && ultimo.fim == tempo) {
                ultimo.fim = tempo + 1;
                return;
            }
        }
        gantt.add(new TrechoGantt(tempo, tempo + 1, pid));
    }
}
