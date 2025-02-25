package by.lupach.drawingeditor.models.triangles;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

import java.util.Objects;

@Data
public class Edge {
    private Pixel p;
    private Pixel q;
    public Edge(Pixel p, Pixel q) {
        if (p.getX() < q.getX() || (p.getX() == q.getX() && p.getY() < q.getY())) {
            this.p = p;
            this.q = q;
        } else {
            this.p = q;
            this.q = p;
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return p.equals(edge.p) && q.equals(edge.q);
    }
    @Override
    public int hashCode() {
        return Objects.hash(p, q);
    }
}