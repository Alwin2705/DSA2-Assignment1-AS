package utils;

import java.util.ArrayList;
import java.util.List;

public class LeafCluster {

    private final int id;


    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;


    private int size;


    private int rank;


    private final List<int[]> pixels = new ArrayList<>();


    public LeafCluster(int id) {
        this.id = id;
    }


    public void addPixel(int x, int y) {
        pixels.add(new int[]{x, y});
        size++;
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
    }



    public int getId()    { return id;   }
    public int getMinX()  { return minX; }
    public int getMinY()  { return minY; }
    public int getMaxX()  { return maxX; }
    public int getMaxY()  { return maxY; }


    public int getWidth()  { return maxX - minX + 1; }


    public int getHeight() { return maxY - minY + 1; }


    public int getSize()   { return size; }


    public List<int[]> getPixels() { return pixels; }

    public int getRank()           { return rank; }
    public void setRank(int rank)  { this.rank = rank; }



    public double getCenterX() { return minX + getWidth()  / 2.0; }
    public double getCenterY() { return minY + getHeight() / 2.0; }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeafCluster)) return false;
        return id == ((LeafCluster) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "LeafCluster{rank=" + rank
                + ", size=" + size
                + ", bbox=[" + minX + "," + minY + "→" + maxX + "," + maxY + "]}";
    }
}