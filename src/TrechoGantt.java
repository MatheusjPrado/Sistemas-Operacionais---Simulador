public class TrechoGantt {
    int inicio;
    int fim;
    String pid;

    public TrechoGantt(int inicio, int fim, String pid) {
        this.inicio = inicio;
        this.fim = fim;
        this.pid = pid;
    }

    public int duracao() {
        return fim - inicio;
    }
}
