import java.util.ArrayList;
import java.util.List;

public class Resultado {
    String nome;
    List<Processo> processos = new ArrayList<>();
    List<TrechoGantt> gantt = new ArrayList<>();
    int trocasContexto;
    int tempoFinal;

    public double mediaEspera() {
        double soma = 0;
        for (Processo p : processos) {
            soma += p.tempoEspera();
        }
        return soma / processos.size();
    }

    public double mediaRetorno() {
        double soma = 0;
        for (Processo p : processos) {
            soma += p.tempoRetorno();
        }
        return soma / processos.size();
    }

    public double mediaResposta() {
        double soma = 0;
        for (Processo p : processos) {
            soma += p.tempoResposta();
        }
        return soma / processos.size();
    }

    public double mediaRespostaTipo(String tipo) {
        double soma = 0;
        int qtd = 0;
        for (Processo p : processos) {
            if (p.tipo.equals(tipo)) {
                soma += p.tempoResposta();
                qtd++;
            }
        }
        if (qtd == 0) {
            return 0;
        }
        return soma / qtd;
    }

    public double mediaEsperaGrupo(boolean sensivel) {
        double soma = 0;
        int qtd = 0;
        for (Processo p : processos) {
            if (p.ehSensivel() == sensivel) {
                soma += p.tempoEspera();
                qtd++;
            }
        }
        if (qtd == 0) {
            return 0;
        }
        return soma / qtd;
    }
}
