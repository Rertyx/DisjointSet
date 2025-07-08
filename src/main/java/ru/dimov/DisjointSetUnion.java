package ru.dimov;

/**
 *Класс реализующий Систему непересекающихся множеств (DSU)
 */
public class DisjointSetUnion {
    private final int[] parent;
    private final int[] size;
    private int count;

    public DisjointSetUnion(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Number of elements must be non-negative.");
        }
        this.count = n;
        this.parent = new int[n];
        this.size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    /**
     * Находит корень (представителя) множества для элемента p.
     * Применяет сжатие пути.
     */
    public int find(int p) {
        validate(p);
        int root = p;
        while (root != parent[root]) {
            root = parent[root];
        }


        int current = p;
        while (current != root) {
            int next = parent[current];
            parent[current] = root;
            current = next;
        }
        return root;
    }

    /**
     * Объединяет два множества, содержащие элементы p и q.
     * Использует объединение по размеру.
     */
    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);

        if (rootP == rootQ) {
            return; // Уже в одном множестве
        }


        if (size[rootP] < size[rootQ]) {
            parent[rootP] = rootQ;
            size[rootQ] += size[rootP];
        } else {
            parent[rootQ] = rootP;
            size[rootP] += size[rootQ];
        }
        count--;
    }

    private void validate(int p) {
        int n = parent.length;
        if (p < 0 || p >= n) {
            throw new IllegalArgumentException("Index " + p + " is not between 0 and " + (n - 1));
        }
    }
}
