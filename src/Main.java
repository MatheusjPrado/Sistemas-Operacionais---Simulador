import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        File arquivoProcessos = achar("processos_entrada_correlacionados.csv", "processos.csv");
        File arquivoHistorico = achar("historico_escalonamento_2000_correlacionado.csv", "historico.csv");

        if (arquivoProcessos == null) {
            System.out.println("Nao achei o CSV de processos. Coloque-o na pasta dados.");
            return;
        }

        List<Processo> processos = lerProcessos(arquivoProcessos);
        PrintWriter arquivo = new PrintWriter("resultados.txt", "UTF-8");

        println(arquivo, "SIMULADOR DE ESCALONAMENTO DE PROCESSOS");
        println(arquivo, "Processos lidos: " + processos.size());
        println(arquivo, "Arquivo: " + arquivoProcessos.getAbsolutePath());
        println(arquivo, "");

        analisarHistorico(arquivoHistorico, arquivo);

        Resultado rr = Simulador.executar(processos, Simulador.ROUND_ROBIN);
        Resultado filas = Simulador.executar(processos, Simulador.MULTIPLAS_FILAS);
        Resultado proposto = Simulador.executar(processos, Simulador.PROPOSTO);

        imprimirResultado(rr, arquivo);
        imprimirResultado(filas, arquivo);
        imprimirResultado(proposto, arquivo);
        imprimirComparacao(rr, filas, proposto, arquivo);

        arquivo.close();
        System.out.println("Resultados tambem foram gravados em resultados.txt");
    }

    private static void analisarHistorico(File arquivoHistorico, PrintWriter saida) throws Exception {
        println(saida, "=== USO DO HISTORICO (opcional) ===");
        if (arquivoHistorico == null) {
            println(saida, "Historico nao encontrado. O metodo proposto nao depende dele.");
            println(saida, "");
            return;
        }

        int candidatos = 0;
        int selecionados = 0;
        double esperaSel = 0;
        double esperaNao = 0;
        int qtdNao = 0;
        Map<String, Integer> tipoSel = new LinkedHashMap<>();
        tipoSel.put("tempo_real", 0);
        tipoSel.put("interativo", 0);
        tipoSel.put("io_bound", 0);
        tipoSel.put("misto", 0);
        tipoSel.put("cpu_bound", 0);
        tipoSel.put("batch", 0);
        int sensivelSel = 0;

        BufferedReader br = new BufferedReader(new FileReader(arquivoHistorico));
        String linha = br.readLine();
        while ((linha = br.readLine()) != null) {
            if (linha.trim().isEmpty()) {
                continue;
            }
            String[] c = linha.split(",");
            String tipo = c[9];
            int espera = Integer.parseInt(c[15]);
            int sensivel = Integer.parseInt(c[19]);
            int selecionado = Integer.parseInt(c[28]);
            candidatos++;
            if (selecionado == 1) {
                selecionados++;
                esperaSel += espera;
                if (tipoSel.containsKey(tipo)) {
                    tipoSel.put(tipo, tipoSel.get(tipo) + 1);
                }
                if (sensivel == 1) {
                    sensivelSel++;
                }
            } else {
                qtdNao++;
                esperaNao += espera;
            }
        }
        br.close();

        println(saida, "Arquivo: " + arquivoHistorico.getName());
        println(saida, "Candidatos no historico: " + candidatos);
        println(saida, "Processos escolhidos: " + selecionados);
        println(saida, "Espera media dos escolhidos: " + arredonda(esperaSel / selecionados));
        println(saida, "Espera media dos nao escolhidos: " + arredonda(esperaNao / qtdNao));
        println(saida, "Escolhidos que sao sensiveis a resposta: " + sensivelSel + " de " + selecionados);
        println(saida, "Escolhas por tipo:");
        for (String tipo : tipoSel.keySet()) {
            println(saida, "  " + tipo + ": " + tipoSel.get(tipo));
        }
        println(saida, "Conclusao: o historico escolhe bastante quem ja esperou e quem e interativo/tempo real.");
        println(saida, "Por isso o metodo proposto usa prioridade + envelhecimento + preferencia interativa.");
        println(saida, "");
    }

    private static void imprimirResultado(Resultado r, PrintWriter saida) {
        println(saida, "============================================================");
        println(saida, "ALGORITMO: " + r.nome);
        println(saida, "============================================================");
        println(saida, String.format("Tempo medio de espera  : %.2f", r.mediaEspera()));
        println(saida, String.format("Tempo medio de retorno : %.2f", r.mediaRetorno()));
        println(saida, String.format("Tempo medio de resposta: %.2f", r.mediaResposta()));
        println(saida, "Trocas de contexto     : " + r.trocasContexto);
        println(saida, "Tempo final da simulacao: " + r.tempoFinal);
        println(saida, String.format("Resposta media tempo_real : %.2f", r.mediaRespostaTipo("tempo_real")));
        println(saida, String.format("Resposta media interativo : %.2f", r.mediaRespostaTipo("interativo")));
        println(saida, String.format("Resposta media cpu_bound  : %.2f", r.mediaRespostaTipo("cpu_bound")));
        println(saida, String.format("Resposta media batch      : %.2f", r.mediaRespostaTipo("batch")));
        println(saida, String.format("Espera media sensiveis    : %.2f", r.mediaEsperaGrupo(true)));
        println(saida, String.format("Espera media demais       : %.2f", r.mediaEsperaGrupo(false)));
        println(saida, "");
        println(saida, String.format("%-6s %-22s %-12s %6s %6s %6s %8s %7s %8s",
                "PID", "Nome", "Tipo", "Cheg.", "Inicio", "Fim", "Retorno", "Espera", "Resposta"));

        for (Processo p : r.processos) {
            println(saida, String.format("%-6s %-22s %-12s %6d %6d %6d %8d %7d %8d",
                    p.pid, p.nome, p.tipo, p.chegada, p.tempoInicio, p.tempoConclusao,
                    p.tempoRetorno(), p.tempoEspera(), p.tempoResposta()));
        }

        println(saida, "");
        println(saida, "Ordem de execucao (cada vez que um processo entra na CPU):");
        String ordem = ordemExecucao(r);
        saida.println(ordem);
        if (ordem.length() > 200) {
            System.out.println(ordem.substring(0, 200) + " ... (completa em resultados.txt)");
        } else {
            System.out.println(ordem);
        }
        println(saida, "");

        saida.println("Diagrama de Gantt:");
        System.out.println("Diagrama de Gantt (inicio; completo em resultados.txt):");
        int mostrados = 0;
        for (TrechoGantt t : r.gantt) {
            String linha = String.format("  [%4d - %4d] %-8s (%d u.t.)", t.inicio, t.fim, t.pid, t.duracao());
            saida.println(linha);
            if (mostrados < 10) {
                System.out.println(linha);
                mostrados++;
            }
        }
        if (r.gantt.size() > 10) {
            System.out.println("  ... mais " + (r.gantt.size() - 10) + " trechos no arquivo resultados.txt");
        }
        println(saida, "");
    }

    private static String ordemExecucao(Resultado r) {
        StringBuilder sb = new StringBuilder();
        String anterior = "";
        for (TrechoGantt t : r.gantt) {
            if (t.pid.equals("OCIOSO")) {
                continue;
            }
            if (!t.pid.equals(anterior)) {
                if (sb.length() > 0) {
                    sb.append(" -> ");
                }
                sb.append(t.pid);
                anterior = t.pid;
            }
        }
        return sb.toString();
    }

    private static void imprimirComparacao(Resultado rr, Resultado filas, Resultado proposto, PrintWriter saida) {
        println(saida, "============================================================");
        println(saida, "COMPARACAO DOS TRES ALGORITMOS");
        println(saida, "============================================================");
        println(saida, String.format("%-32s %12s %12s %12s", "Metrica", "RR", "Filas", "Proposto"));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Espera media", rr.mediaEspera(), filas.mediaEspera(), proposto.mediaEspera()));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Retorno medio", rr.mediaRetorno(), filas.mediaRetorno(), proposto.mediaRetorno()));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Resposta media", rr.mediaResposta(), filas.mediaResposta(), proposto.mediaResposta()));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Resposta interativo", rr.mediaRespostaTipo("interativo"), filas.mediaRespostaTipo("interativo"), proposto.mediaRespostaTipo("interativo")));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Resposta tempo_real", rr.mediaRespostaTipo("tempo_real"), filas.mediaRespostaTipo("tempo_real"), proposto.mediaRespostaTipo("tempo_real")));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Resposta cpu_bound", rr.mediaRespostaTipo("cpu_bound"), filas.mediaRespostaTipo("cpu_bound"), proposto.mediaRespostaTipo("cpu_bound")));
        println(saida, String.format("%-32s %12.2f %12.2f %12.2f", "Resposta batch", rr.mediaRespostaTipo("batch"), filas.mediaRespostaTipo("batch"), proposto.mediaRespostaTipo("batch")));
        println(saida, String.format("%-32s %12d %12d %12d", "Trocas de contexto", rr.trocasContexto, filas.trocasContexto, proposto.trocasContexto));
        println(saida, "");
        println(saida, "Menor espera media : " + vencedorEspera(rr, filas, proposto));
        println(saida, "Menor resposta media: " + vencedorResposta(rr, filas, proposto));
        println(saida, "");
        println(saida, "METODO PROPOSTO: Prioridade com Envelhecimento");
        println(saida, "- Escolha: menor prioridade efetiva.");
        println(saida, "- Prioridade efetiva = prioridade do arquivo - (tempo de espera / 10).");
        println(saida, "- Empate: ganha tempo_real/interativo; depois quem esperou mais.");
        println(saida, "- Quantum: 2 (tempo_real/interativo), 4 (io_bound/misto), 8 (cpu_bound/batch).");
        println(saida, "- E/S: sai da CPU, fica bloqueado e volta para pronto.");
        println(saida, "- Anti-inanicao: a cada 10 u.t. esperando, a prioridade melhora em 1.");
        println(saida, "- Limitacao: o valor 10 do envelhecimento e fixo; processos longos ainda podem atrasar um pouco os curtos.");
    }

    private static String vencedorEspera(Resultado a, Resultado b, Resultado c) {
        Resultado melhor = a;
        if (b.mediaEspera() < melhor.mediaEspera()) {
            melhor = b;
        }
        if (c.mediaEspera() < melhor.mediaEspera()) {
            melhor = c;
        }
        return melhor.nome;
    }

    private static String vencedorResposta(Resultado a, Resultado b, Resultado c) {
        Resultado melhor = a;
        if (b.mediaResposta() < melhor.mediaResposta()) {
            melhor = b;
        }
        if (c.mediaResposta() < melhor.mediaResposta()) {
            melhor = c;
        }
        return melhor.nome;
    }

    private static List<Processo> lerProcessos(File arquivo) throws Exception {
        List<Processo> lista = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(arquivo));
        String linha = br.readLine();
        while ((linha = br.readLine()) != null) {
            if (linha.trim().isEmpty()) {
                continue;
            }
            String[] c = linha.split(",");
            lista.add(new Processo(
                    c[0],
                    c[1],
                    Integer.parseInt(c[2]),
                    Integer.parseInt(c[3]),
                    Integer.parseInt(c[4]),
                    c[5],
                    Integer.parseInt(c[6]),
                    Double.parseDouble(c[8]),
                    Integer.parseInt(c[9])
            ));
        }
        br.close();
        return lista;
    }

    private static File achar(String... nomes) {
        String[] pastas = {".", "dados", "src", "..", "../dados"};
        for (String pasta : pastas) {
            for (String nome : nomes) {
                File f = new File(pasta, nome);
                if (f.isFile()) {
                    return f.getAbsoluteFile();
                }
            }
        }
        return null;
    }

    private static void println(PrintWriter saida, String texto) {
        System.out.println(texto);
        saida.println(texto);
    }

    private static String arredonda(double valor) {
        return String.format("%.2f", valor);
    }
}
