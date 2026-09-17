public class Processo {
    String pid;
    String nome;
    int chegada;
    int cpuTotal;
    int prioridade;
    String tipo;
    int operacaoES;
    double mediaES;
    int duracaoES;

    int cpuRestante;
    int cpuExecutado;
    String estado;
    int tempoInicio;
    int tempoConclusao;
    int totalIO;
    int ioFim;
    int filaAtual;
    int esperaAtual;
    int numES;
    int intervaloES;
    int iosFeitas;
    int proximoPontoES;

    public Processo(String pid, String nome, int chegada, int cpuTotal, int prioridade,
                    String tipo, int operacaoES, double mediaES, int duracaoES) {
        this.pid = pid;
        this.nome = nome;
        this.chegada = chegada;
        this.cpuTotal = cpuTotal;
        this.prioridade = prioridade;
        this.tipo = tipo;
        this.operacaoES = operacaoES;
        this.mediaES = mediaES;
        this.duracaoES = duracaoES;
        resetar();
    }

    public Processo copiar() {
        return new Processo(pid, nome, chegada, cpuTotal, prioridade, tipo, operacaoES, mediaES, duracaoES);
    }

    public void resetar() {
        cpuRestante = cpuTotal;
        cpuExecutado = 0;
        estado = "NAO_CHEGOU";
        tempoInicio = -1;
        tempoConclusao = -1;
        totalIO = 0;
        ioFim = -1;
        filaAtual = filaDoTipo();
        esperaAtual = 0;
        iosFeitas = 0;

        if (operacaoES == 1) {
            numES = (int) Math.round(mediaES);
            if (numES < 1) {
                numES = 1;
            }
            intervaloES = cpuTotal / (numES + 1);
            if (intervaloES < 1) {
                intervaloES = 1;
            }
            proximoPontoES = intervaloES;
        } else {
            numES = 0;
            intervaloES = 0;
            proximoPontoES = Integer.MAX_VALUE;
        }
    }

    public int filaDoTipo() {
        if (tipo.equals("tempo_real") || tipo.equals("interativo")) {
            return 1;
        }
        if (tipo.equals("io_bound") || tipo.equals("misto")) {
            return 2;
        }
        return 3;
    }

    public int quantumDoTipo() {
        if (filaDoTipo() == 1) {
            return 2;
        }
        if (filaDoTipo() == 2) {
            return 4;
        }
        return 8;
    }

    public boolean ehSensivel() {
        return tipo.equals("tempo_real") || tipo.equals("interativo");
    }

    public int prioridadeEfetiva() {
        int valor = prioridade - (esperaAtual / 10);
        if (valor < 1) {
            return 1;
        }
        return valor;
    }

    public boolean deveFazerES() {
        if (iosFeitas >= numES || duracaoES <= 0 || cpuRestante <= 0) {
            return false;
        }
        if (cpuExecutado >= proximoPontoES) {
            iosFeitas++;
            proximoPontoES += intervaloES;
            return true;
        }
        return false;
    }

    public int tempoRetorno() {
        return tempoConclusao - chegada;
    }

    public int tempoEspera() {
        return tempoRetorno() - cpuTotal - totalIO;
    }

    public int tempoResposta() {
        return tempoInicio - chegada;
    }
}
