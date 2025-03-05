package by.lupach.drawingeditor.models.voronoi;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

@Data
public class Edge {
    private Pixel a, b;

    public Edge(Pixel a, Pixel b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Edge edge = (Edge) obj;
        return (a.equals(edge.a) && b.equals(edge.b)) || (a.equals(edge.b) && b.equals(edge.a));
    }

    @Override
    public int hashCode() {
        return a.hashCode() + b.hashCode();
    }
}
