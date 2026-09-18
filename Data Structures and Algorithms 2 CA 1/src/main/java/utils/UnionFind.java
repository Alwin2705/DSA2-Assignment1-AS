package utils;


public class UnionFind {

    private final int[] parent;


    private final int[] size;

    private int count;

    public UnionFind(int n) {
        this.parent = new int[n];
        this.size   = new int[n];
        this.count  = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i]   = 1;
        }
    }

    public int find(int x) {
        while (x != parent[x]) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    public boolean union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        if (ra == rb) return false;


        if (size[ra] < size[rb]) {
            int tmp = ra; ra = rb; rb = tmp;
        }
        parent[rb]  = ra;
        size[ra]   += size[rb];
        count--;
        return true;
    }

    public int getSize(int x) {
        return size[find(x)];
    }

    public int getCount() {
        return count;
    }

    public int[] getParents() {
        return parent;
    }
}