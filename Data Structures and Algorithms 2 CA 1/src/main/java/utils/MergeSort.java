package utils;

import java.util.List;


public class MergeSort {

    public static void sortBySizeDesc(List<LeafCluster> clusters) {
        if (clusters == null || clusters.size() < 2) return;
        mergeSort(clusters, 0, clusters.size() - 1);
    }



    private static void mergeSort(List<LeafCluster> list, int left, int right) {
        if (left >= right) return;
        int mid = (left + right) / 2;
        mergeSort(list, left, mid);
        mergeSort(list, mid + 1, right);
        merge(list, left, mid, right);
    }

    private static void merge(List<LeafCluster> list, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        LeafCluster[] L = new LeafCluster[n1];
        LeafCluster[] R = new LeafCluster[n2];

        for (int i = 0; i < n1; i++) L[i] = list.get(left + i);
        for (int j = 0; j < n2; j++) R[j] = list.get(mid + 1 + j);

        int i = 0, j = 0, k = left;

        while (i < n1 && j < n2) {
            if (L[i].getSize() >= R[j].getSize()) list.set(k++, L[i++]);
            else                                  list.set(k++, R[j++]);
        }
        while (i < n1) list.set(k++, L[i++]);
        while (j < n2) list.set(k++, R[j++]);
    }
}